package com.yieldstreet.rabbit;

import com.yieldstreet.entity.*;
import com.yieldstreet.repository.AccreditationHistoryRepository;
import com.yieldstreet.repository.AccreditationRepository;
import org.quartz.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring instantiates this because its a @Service, using @RabbitListener attaches it to the queue.
 * Spring injects the dependencies such as the Repository layer.
 */
@Service
public class RabbitMQReceiver {
    private static Logger logger = LoggerFactory.getLogger(RabbitMQReceiver.class);

    @Value("${accreditation-expiry-delay-millis}")
    private String millisToExpiry;

    @Autowired
    private AccreditationRepository accreditationRepository;

    @Autowired
    private AccreditationHistoryRepository accreditationHistoryRepository;

    @Autowired
    private SchedulerFactoryBean quartzScheduler;

    @Autowired
    private RabbitMQSender rabbitMQSender;

    /**
     * if an exception is thrown from here, RabbitMQ will retry the message. that may not be ideal.
     * this method is single threaded since it is off a queue. we can enable multithreading per accreditation
     * using a hashing plugin to RabbitMQ.
     * @param acc
     */
    @Transactional
    @RabbitListener(queues = "${accreditation-queue}")
    public void receiveMessage(Accreditation acc) {
        logger.debug("Received an accreditation state change event from RabbitMQ: "+ acc.getAccreditationId());
        Optional<Accreditation> current = accreditationRepository.findById(acc.getAccreditationId());
        if(current.isPresent()){
            AccreditationStatus targetStatus = acc.getStatus();
            Accreditation accCurrent = current.get();
            AccreditationStatus currentStatus = accCurrent.getStatus();
            // we ignore loopback transitions to avoid polluting the history
            if(     (currentStatus!=AccreditationStatus.FAILED && targetStatus==AccreditationStatus.FAILED) ||
                    (currentStatus==AccreditationStatus.PENDING && targetStatus==AccreditationStatus.CONFIRMED) ||
                    (currentStatus==AccreditationStatus.CONFIRMED && targetStatus==AccreditationStatus.EXPIRED)    ) {
                // the history table has the old statuses, the accreditation table always has the latest
                // accreditation status - latest and history are linked by the accreditation_id and the timestamp
                long now = System.currentTimeMillis();
                AccreditationHistory accHistory = new AccreditationHistory();
                accHistory.setLastUpdateTime(now);
                accHistory.setAccreditationId(acc.getAccreditationId());
                accHistory.setOldStatus(accCurrent.getStatus());
                // there is a DB transaction around these saves
                accreditationHistoryRepository.save(accHistory);
                acc.setLastUpdateTime(now);
                accreditationRepository.save(acc);
                if(currentStatus==AccreditationStatus.PENDING && targetStatus==AccreditationStatus.CONFIRMED){
                    startScheduledTaskToExpireLater(accreditationRepository, acc, rabbitMQSender);
                    logger.debug("Scheduled a job to expire confirmed accreditation after period of inactivity exceeded: "+
                            acc.getAccreditationId());
                }
            }
            else{
                // log ignored transition for the accreditation_id
                logger.error("No state change was possible for accreditation "+ acc.getAccreditationId()
                            + ", Current state "+ currentStatus.name()
                            + ", Target state "+ targetStatus.name());
            }
        }
        else{
            logger.error("No state change possible for unknown accreditation: "+ acc.getAccreditationId());
        }
    }

    /**
     * Job will poll the accreditation every 1 minute to see if it needs to be expired, in PROD this would
     * most likely be less frequent.
     * @param acc
     */
    private void startScheduledTaskToExpireLater(AccreditationRepository accreditationRepository, Accreditation acc, RabbitMQSender rabbitMQSender){
        JobDetail detail =  JobBuilder.newJob(ExpiryJob.class)
                .withIdentity("schedule")
                .withDescription("Expiry schedule task")
                .build();
        // this is how you pass data into the quartz job
        JobDataMap data = detail.getJobDataMap();
        data.put("accreditationRepository", accreditationRepository);
        data.put("accreditation", acc);
        data.put("rabbitMQSender", rabbitMQSender);
        data.put("millisToExpiry", millisToExpiry);
        String MINUTELY_CRON = "0 * * * * ?";
        // String HOURLY_CRON = "* 0 * * * ?";
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(acc.getAccreditationId())
                .forJob(detail)
                .withSchedule(CronScheduleBuilder.cronSchedule(MINUTELY_CRON)).build();
        Scheduler scheduler = this.quartzScheduler.getScheduler();
        try {
            scheduler.scheduleJob(detail, new HashSet<>(Arrays.asList(trigger)), true);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not schedule an expiry job for confirmed accreditation: " +
                    acc.getAccreditationId());
        }
    }

    /**
     * this callback happens on the quartz thread, references on the main thread can be gone by the time this happens.
     * so pass what you need in here as arguments using the JobDataMap.
     */
    class ExpiryJob extends QuartzJobBean {
        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            // careful of logging here, it can get called a lot
            JobDataMap args = context.getJobDetail().getJobDataMap();
            AccreditationRepository accreditationRepository = (AccreditationRepository)args.get("accreditationRepository");
            Accreditation jobAccreditation = (Accreditation)args.get("accreditation");
            RabbitMQSender rabbitMQSender = (RabbitMQSender)args.get("rabbitMQSender");
            Long millisToExpiryValue = Long.valueOf((String)args.get("millisToExpiry"));
            String accreditationId = jobAccreditation.getAccreditationId();
            // load the latest in case something updated the timestamp since job was created
            Optional<Accreditation> current = accreditationRepository.findById(accreditationId);
            if(current.isPresent()){
                Accreditation acc = current.get();
                long now = System.currentTimeMillis();
                // its possible that just before the scheduled job was about to EXPIRE, someone triggered it via the API.
                // so we unschedule the job in both cases.
                if((now - acc.getLastUpdateTime()) > millisToExpiryValue) {
                    logger.debug("Expiry job activated and expiration time reached for accredication: " +
                            acc.getAccreditationId());
                    if (acc.getStatus() == AccreditationStatus.CONFIRMED) {
                        // change the status via the queue to avoid a race condition on the status
                        // queue enforces single thread (at least per accreditation_id)
                        acc.setStatus(AccreditationStatus.EXPIRED);
                        rabbitMQSender.send(acc);
                        killExpiryJob(context, accreditationId);
                        logger.debug("Queued expiration state change event, deactivating expiry job for accreditation: "+
                                accreditationId );
                    } else if (acc.getStatus() == AccreditationStatus.EXPIRED) {
                        killExpiryJob(context, accreditationId);
                    }
                }
            }
        }
    }

    /**
     * once the job has expired the confirmed accreditation it can be desecheduled from Quartz
     */
    private void killExpiryJob(JobExecutionContext context, String accreditationId){
        try {
            context.getScheduler().unscheduleJob(new TriggerKey(accreditationId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not unschedule an expiry job for expired accreditation: " +
                    accreditationId);
        }
    }

}

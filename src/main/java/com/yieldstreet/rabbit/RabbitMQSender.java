package com.yieldstreet.rabbit;

import com.yieldstreet.entity.Accreditation;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {

    @Autowired
    private AmqpTemplate rabbitTemplate;

    @Value("${accreditation-exchange}")
    private String exchange;

    @Value("${accreditation-routingkey}")
    private String routingkey;

    public void send(Accreditation to) {
        rabbitTemplate.convertAndSend(exchange, routingkey, to);
    }

}
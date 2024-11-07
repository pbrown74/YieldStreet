
Contents:
****************
Installation
Build
Run Tests
Architecture
Questions
Issues



Installation:
****************
Clone the repo from https://github.com/pbrown74/yieldstreet

Change directory to the root and build using the following steps.
(I bundled the pre-built JAR so you don't have to build, you can jump to Docker steps if you want)


Build:
************
To build the code use maven, and run the integration tests:
  mvn clean install

To build without running the tests (test takes about 5 minutes):
  mvn clean install -DskipTests

To build the Docker image, either run the run_assignment.sh script or issue commands from the project root:
  docker build -t assignment-0.0.1 .
  docker compose up

Docker compose will pull JDK 17, MySQL and RabbitMQ latest.

There is a main/resources/application.properties which has a 30 day accreditation expiry period. There is a
test/resources/application.properties which expires after 60 seconds, to make the testing doable.

After a few minutes, you should now have the service running on localhost 9999 inside Docker which has exposed 9999.
Be careful to not run the tests at the same time, or you will get a port binding clash since the tests run a Docker on 9999 too.


Run Tests
************************
For a manual test you can use the POSTman test cases i supplied in YieldStreet/src/test/resources
These are pointing at localhost:9999 so they should work out of the box. The POSTman tests cover:
  insert accreditation
  confirm accreditation
  fail accreditation
  expire accreditation
  get user accreditations
  get accreditations history

I do more thorough automated testing from com.yieldstreet.AssignmentApplicationTests which itself is a Dockerised test
leveraging the MySQL and RabbitMQ backends and TestContainers Spring project - so no mocking is used here. This
integration test covers the following flow:

	POST good accreditation
	POST second one for same userId, this should fail because of pending accreditation on userId
	GET the first accreditation, so we verify it was created
	PUT a bad accreditation to check the HTTP status code
	PUT a good accreditation to modify the state, asynchronous processing so do a little delay at the end
	GET the first accreditation to verify it was changed to CONFIRMED by doPUT, in a minute check it was pushed to EXPIRED
	POST second one again, this should work now because of first pending accreditation having move into CONFIRMED
	GET the users accreditations just to check we have 2 now
	GET the first accreditation to check it was pushed to EXPIRED, the 1 minute delay is configured in the test context
	GET the history of the first accreditation, it should be PENDING->CONFIRMED (EXPIRED is not in the history since its current)

I did not put any unit tests in, due to time. If i had more time i would put one around the RabbitReceiver to test the state transitions.
I would probably use mocking there to reduce dependency on the repository layer.


Architecture
************************
From a high level i've built a small 3-tier system. Frontend are REST endpoints done using SpringBOOT.
You can exercise these from POSTman. Middletier is RabbitMQ and custom code. Backend is MySQL.
Persistence is done using Hibernate/JPA, no DAO code is written except the entity object to represent the data.
For REST endpoint implementation i used Spring. Therefore the "service" is a SpringBOOT app, the main class
is com.yieldstreet.AssignmentApplication. I used Quartz as a scheduler to expire inactive confirmed accreditations
periodically- a job runs which checks if its accreditiation has been inactive for more than 30 days, if so
it will expire the accreditiation- via the queue to avoid a race where two threads hit the same accreditation.
Quartz is embedded at the minute so the "process" that is expiring inactive confirmed accreditations is actually
a background thread - not a separate process - though it could be.

The code is structured in the classic SpringBOOT way, you will find these packages under com.yieldstreet:

   src/
    controller/
    dto/
    entity/
    exception/
    rabbit/
    repository/
    service/

and
   test/


They are fairly self explanatatory but a brief explanation follows. (Controller / Service / Repository is standard SpringBOOT layering).

In the following package you find the only service class, that contains most of the business logic such as validation:

  service/

In here you find some Hibernate classes for Accreditation, Document and AccreditationHistory:

  entity/

In here you find the Rabbit Receiver, in there we have a state machine which handles the lifecyle of the accreditation:

  rabbit/


I centralised mapping of exceptions to HTTP status codes by using the Spring hook: GlobalControllerExceptionHandler



Design
************
POST inserts are done synchronously, there is validation done to prevent bad data getting into the system.

PUT updates , which modify accreditation statuses are done asynchronously by putting a message on RabbitMQ queue,
the consumer is single threaded, this helps to order the events that are happening to an accreditation. It does not
completely eliminate race conditions (discussed later). Even when the expiry job needs to expire an accreditation, it
goes via the queue - so only one thread is ever modifying an instance of an accreditation (rather than the PUT thread
and the expiry thread, or two PUT threads). One could speed up the queue by having multiple consumers, where a hashing
plugin is used in RabbitMQ so that state changes for one accreditation are done on one consumer thread. Multithreading
in that case is done only for different accreditation instances (IDs). PUT updates are done at same time as recording
the history of the state change.

GET is synchronous - its possible for a client to retrieve an accreditation while a state change is still on the queue.
There is no way around this in the current solution. I mention this later. You could call this "eventual consistency". The
update will get done, and there is a small window of time where the GET client can get a stale accreditiation if they pull
before the update is done.

Database Transactions are used where i modify two tables logically at once.

The History table stores the statuses as the accreditations evolves. The current status is stored on the Accreditation itself.
The key to link them is the accreditation ID, and the timestamp can be used to find the latest historic status.

I use the DTO pattern to seperate the objects used for JSON marshalling from the entities used in Hibernate.

UUID is used for the ID of an Accreditation. This is autogenerated in the persistence layer, by Hibernate.


Questions
*************
My answers to the questions are below the questions, tagged as "[PB]"



Question a)	Please provide an overview of the architecture, briefly mentioning the applied architectural patterns.

 [PB] see above, three tier architecture, eventual consistency, point-to-point messaging, database transactions, state machine to handle status transitions, DTOs, REST



Question b)	The  endpoints can be hit separately by different administrators, triggering multiple concurrent updates to the same accreditation status.
Please outline a high level solution which ensures that the checks outlined in 4) are always consistent with the latest state of the accreditation status.

 [PB] I would use a version number on the accreditation. Then we can do optimistic locking. This needs an extra field in the accreditation entity. Hibernate
 can manage this for you. Essentially, if you are holding a stale version of an accreditation , you wont have the latest version number on it. When you try
 and update with it, it will be rejected and you will need to pull the latest version of that accreditation and try again.



Question c)	Should the client facing traffic increase multiple fold, both the admin facing endpoints would be affected along with client facing API by
virtue of being part of the same service. Please provide an outline of the steps you would take to ensure that Yieldstreet’s client facing
accreditation flow is capable of handling the increased scale of traffic.

 [PB] I would put an accreditation cache into the Accreditation Service. When an accreditation is added it is cached. All calls to GET go to the cache first
 before pulling from the persistance if necessary. Accreditations are evicted from the cache by changes to the state done in the backend asynchronously (PUT) - could
 use RabbitMQ to carry the eviction event from the backend to the accreditation cache in the service/controller. This makes the window of error smaller but does not
 remove it completely. Because the cache can be stale while the eviction event is being moved around.

 [PB] Or, I would start a separate service for GET requests. You can have an API gateway which forwards GET requests to one server, and POST/PUT to another.
 Then you could put the GET service on a faster VM or cluster it horizontally by having multiple instances. Kubernetes can do that. Still the bottleneck
 would be the database, you would probably end up looking at caching in the end (or clustering the database which is not easy and synching has an overhead).


Issues
**********
Instead of one expiry job per accreditation, i could have one expiry job that looks for all expiring accreditations.
Although Quartz is not using one thread per job, it is smarter than that.

There should be a User entity so i can validate the UserId.

Queues and Tables are autocreated via Spring and updated each time around.


Paul Brown
November 2024

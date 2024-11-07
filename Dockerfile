FROM openjdk:17-oracle
EXPOSE 9999
ADD target/assignment-0.0.1-SNAPSHOT.jar assignment-0.0.1.jar
ENTRYPOINT ["java", "-jar", "/assignment-0.0.1.jar"]

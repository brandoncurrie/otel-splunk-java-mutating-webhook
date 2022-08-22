# Alpine Linux with OpenJDK JRE
FROM openjdk:8-jre-alpine

EXPOSE 8083

COPY target/greeting-service-0.0.1-SNAPSHOT.jar   /application.jar

CMD ["/usr/bin/java", "-jar","application.jar"]

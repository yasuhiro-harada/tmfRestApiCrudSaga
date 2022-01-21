FROM adoptopenjdk/openjdk11:alpine-jre
RUN mkdir /opt/app

COPY ./tmf/target/tmf-0.0.1-SNAPSHOT.jar /opt/app/.
CMD ["java", "-jar", "/opt/app/tmf-0.0.1-SNAPSHOT.jar"]
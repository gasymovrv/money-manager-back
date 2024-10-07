FROM eclipse-temurin:21
RUN mkdir /opt/app
COPY target/money-manager-back-1.1.0.jar /opt/app/mm.jar
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/opt/app/mm.jar"]
EXPOSE $PORT

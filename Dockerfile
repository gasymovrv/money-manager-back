FROM eclipse-temurin:21

# Executes only on building an image and works inside the image
RUN mkdir /opt/app

# Executes only on building an image and has access to the host machine
COPY target/money-manager-back-1.2.0.jar /opt/app/mm.jar

# Executes each time a container is launched based on the image
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/opt/app/mm.jar"]

EXPOSE $PORT

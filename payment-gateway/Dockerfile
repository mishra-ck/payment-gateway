# Use official JDK runtime
FROM eclipse-temurin:17-jdk-alpine
# Wrking directory inside container
WORKDIR /app
# Copy executable jar into the container
COPY target/payment-gateway-0.0.1-SNAPSHOT.jar app.jar
# Expose the port on which application is running
EXPOSE 8080
# Run the JAR file
ENTRYPOINT ["java","-jar","app.jar"]
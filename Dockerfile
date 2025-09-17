FROM openjdk:17-jdk-slim

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew

COPY src src

RUN ./gradlew build -x test
RUN find build/libs -name "*.jar" -not -name "*-plain.jar" -exec cp {} app.jar \;

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
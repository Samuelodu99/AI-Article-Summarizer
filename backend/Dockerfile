FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/ai-article-summarizer-backend-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java","-jar","/app/app.jar"]


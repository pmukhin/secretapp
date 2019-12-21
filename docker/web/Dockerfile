FROM openjdk:8
COPY ./modules/web/target/scala-2.12/web-assembly-0.0.1.jar /usr/src/tvi-web-app/app.jar
WORKDIR /usr/src/tvi-web-app
CMD ["java", "-jar", "app.jar"]
EXPOSE 8080
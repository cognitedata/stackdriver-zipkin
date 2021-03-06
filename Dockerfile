FROM java:8

WORKDIR /code

COPY collector/target/collector-0.4.0-cognite-4-SNAPSHOT.jar /code/stackdriver-zipkin.jar

EXPOSE 9411 9100
ENTRYPOINT [ "sh", "-c", "java -XX:+CrashOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -jar /code/stackdriver-zipkin.jar" ]

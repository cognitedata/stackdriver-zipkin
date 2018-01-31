FROM java:8

WORKDIR /code

COPY collector/target/collector-0.4.0-cognite-3.jar /code/stackdriver-zipkin.jar

EXPOSE 9411 9100
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /code/stackdriver-zipkin.jar" ]

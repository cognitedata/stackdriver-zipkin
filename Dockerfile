FROM maven:3.5.0-jdk-8

WORKDIR /code

ARG MAVEN_USER=cogniteread
ARG MAVEN_PASSWORD
ARG MAVEN_REPOSITORY=repository.dev.cognite.ai/repository/cognite-snapshot

ARG MAVEN_GROUP_ID=com.google.cloud.trace.adapters.zipkin
ARG MAVEN_ARTIFACT_ID=collector
ARG MAVEN_ARTIFACT_VERSION=0.4.0-SNAPSHOT

ENV JAR_FILE $MAVEN_ARTIFACT_ID-$MAVEN_ARTIFACT_VERSION.jar

RUN mvn dependency:get -DremoteRepositories="https://$MAVEN_USER:$MAVEN_PASSWORD@$MAVEN_REPOSITORY" \
    -Dartifact=$MAVEN_GROUP_ID:$MAVEN_ARTIFACT_ID:$MAVEN_ARTIFACT_VERSION \
    -Dtransitive=false && \
  mvn dependency:copy \
    -Dartifact=$MAVEN_GROUP_ID:$MAVEN_ARTIFACT_ID:$MAVEN_ARTIFACT_VERSION \
    -DoutputDirectory=/code && \
  rm -rf /root/.m2/settings.xml

EXPOSE 9411

ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /code/$JAR_FILE" ]

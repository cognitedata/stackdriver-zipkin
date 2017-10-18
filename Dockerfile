FROM maven:3.5.0-jdk-8

WORKDIR /code

ARG MAVEN_USER=cogniteread
ARG MAVEN_PASSWORD
ARG MAVEN_REPOSITORY=repository.dev.cognite.ai/repository/cognite

ARG MAVEN_GROUP_ID=com.google.cloud.trace.adapters.zipkin
ARG MAVEN_ARTIFACT_ID=collector
ARG MAVEN_ARTIFACT_VERSION=0.4.0-cognite-1

ENV JAR_FILE $MAVEN_ARTIFACT_ID-$MAVEN_ARTIFACT_VERSION.jar

# echo "${MAVEN_ARTIFACT_VERSION##`echo $MAVEN_ARTIFACT_VERSION | sed 's/-SNAPSHOT$//'`}" | tr '[:upper:]' '[:lower:]'
# This bit of black magic will do the following:
# 1. echo $MAVEN_ARTIFACT_VERSION | sed 's/-SNAPSHOT$//': Remove -SNAPSHOT from the version, if it exists.
#    0.4.0-SNAPSHOT => 0.4.0
#    0.4.0 => 0.4.0
# 2. Use this as the prefix to remove the largest prefix from MAVEN_ARTIFACT_VERSION
#    0.4.0-SNAPSHOT##0.4.0 => -SNAPSHOT
#    0.4.0##0.4.0 => "" (empty string)
# 3. Translate the result to lowercase
#    -SNAPSHOT => -snapshot
#    "" => "" (empty string)
RUN export SNAPSHOT_SUFFIX=$(echo "${MAVEN_ARTIFACT_VERSION##`echo $MAVEN_ARTIFACT_VERSION | sed 's/-SNAPSHOT$//'`}" | \
      tr '[:upper:]' '[:lower:]') && \
  mvn --quiet dependency:get -DremoteRepositories="https://$MAVEN_USER:$MAVEN_PASSWORD@$MAVEN_REPOSITORY$SNAPSHOT_SUFFIX" \
    -Dartifact=$MAVEN_GROUP_ID:$MAVEN_ARTIFACT_ID:$MAVEN_ARTIFACT_VERSION \
    -Dtransitive=false && \
  cp ~/.m2/repository/$(echo $MAVEN_GROUP_ID | sed 's|\.|/|g')/$MAVEN_ARTIFACT_ID/$MAVEN_ARTIFACT_VERSION/$JAR_FILE /code

EXPOSE 9411 9100

ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /code/$JAR_FILE" ]

@Library('jenkins-helpers@v0.1.8') _

mvnLibraryPipeline {
    resourceRequestMemory = '2500Mi'
    resourceLimitMemory = '2500Mi'
}

podTemplate(
    label: 'jnlp-docker',
    containers: [containerTemplate(name: 'jnlp',
                                   image: 'eu.gcr.io/cognitedata/build-client-docker:9cfb7a6',
                                   args: '${computer.jnlpmac} ${computer.name}',
                                   resourceRequestCpu: '1500m',
                                   resourceRequestMemory: '1500Mi',
                                   resourceLimitCpu: '1500m',
                                   resourceLimitMemory: '1500Mi',)],
    volumes: [secretVolume(secretName: 'jenkins-docker-builder',
                           mountPath: '/jenkins-docker-builder',
                           readOnly: true),
              secretVolume(secretName: 'maven-credentials',
                           mountPath: '/maven-credentials'),
              hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]) {
    node('jnlp-docker') {
        container('jnlp') {
            def gitCommit
            stage("Checkout for Docker") {
                checkout(scm)
                gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
            }
            stage("Build Docker container") {
                sh('#!/bin/sh -e\n'
                   + "docker build --build-arg MAVEN_PASSWORD=\"\$(cat /maven-credentials/maven-cognitedeploy-password.txt)\" -t eu.gcr.io/cognitedata/stackdriver-zipkin:${gitCommit} .")
            }
            if (env.BRANCH_NAME == 'master') {
                stage("Push Docker container") {
                    sh('#!/bin/sh -e\n' + 'docker login -u _json_key -p "$(cat /jenkins-docker-builder/credentials.json)" https://eu.gcr.io')
                    sh("docker push eu.gcr.io/cognitedata/stackdriver-zipkin:$gitCommit")
                }
            }
        }
    }
}

@Library('jenkins-helpers@v0.1.8') _

podTemplate(
    label: 'jnlp-stackdriver-zipkin',
    containers: [containerTemplate(name: 'docker',
                                   command: '/bin/cat -',
                                   image: 'docker:17.06.2-ce',
                                   resourceRequestCpu: '1500m',
                                   resourceRequestMemory: '1500Mi',
                                   resourceLimitCpu: '1500m',
                                   resourceLimitMemory: '1500Mi',
                                   ttyEnabled: true),
                 containerTemplate(name: 'maven',
                                   image: 'maven:3.5.2-jdk-8',
                                   command: '/bin/cat -',
                                   resourceRequestCpu: '1000m',
                                   resourceRequestMemory: '1500Mi',
                                   resourceLimitCpu: '1000m',
                                   resourceLimitMemory: '1500Mi',
                                   ttyEnabled: true)],
    volumes: [secretVolume(secretName: 'jenkins-docker-builder',
                           mountPath: '/jenkins-docker-builder',
                           readOnly: true),
              secretVolume(secretName: 'maven-credentials',
                           mountPath: '/maven-credentials'),
              configMapVolume(configMapName: 'codecov-script-configmap', mountPath: '/codecov-script'),
              hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
    envVars: [
            secretEnvVar(key: 'CODECOV_TOKEN', secretName: 'codecov-token-stackdriver-zipkin', secretKey: 'token.txt')
    ]) {
    node('jnlp-stackdriver-zipkin') {
        def gitCommit
        container('jnlp') {
            stage('Checkout') {
                checkout(scm)
                gitCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
            }
        }
        container('maven') {
            stage('Install Maven credentials') {
                sh("cp /maven-credentials/settings.xml /root/.m2")
            }
            stage('Test') {
                sh("mvn test")
            }
            stage("Upload Jacoco reports") {
                sh 'bash </codecov-script/upload-report.sh'
                jacoco(execPattern: 'target/jacoco.exec')
            }
            stage('Build') {
                sh("mvn package")
            }
        }
        container('docker') {
            stage('Build Docker container') {
                sh("docker build -t eu.gcr.io/cognitedata/stackdriver-zipkin:${gitCommit} .")
            }
            stage('Push Docker container') {
                sh('#!/bin/sh -e\n' + 'docker login -u _json_key -p "$(cat /jenkins-docker-builder/credentials.json)" https://eu.gcr.io')
                sh("docker push eu.gcr.io/cognitedata/stackdriver-zipkin:${gitCommit}")
            }
        }
    }
}

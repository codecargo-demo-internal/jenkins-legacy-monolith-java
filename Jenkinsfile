// Legacy monolith build pipeline - DO NOT TOUCH without talking to @garcia
// Last major update: 2024-03 (added SonarQube, broke it twice)
node {
    def mvnHome = tool 'maven-3.9'
    def version = ''

    try {
        stage('Checkout') {
            checkout scm
            version = sh(script: "${mvnHome}/bin/mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
            echo "Building version: ${version}"
        }

        stage('Build') {
            sh "${mvnHome}/bin/mvn clean package -DskipTests -pl api,core,web -am"
        }

        stage('Unit Tests') {
            parallel(
                'API Tests': {
                    sh "${mvnHome}/bin/mvn test -pl api"
                },
                'Core Tests': {
                    sh "${mvnHome}/bin/mvn test -pl core"
                },
                'Web Tests': {
                    sh "${mvnHome}/bin/mvn test -pl web"
                }
            )
        }

        stage('Integration Tests') {
            sh "${mvnHome}/bin/mvn verify -pl api -Pintegration-tests"
        }

        // SonarQube - added 2024-03, server is flaky so we catch errors
        stage('SonarQube Analysis') {
            catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                withSonarQubeEnv('SonarQube') {
                    sh "${mvnHome}/bin/mvn sonar:sonar -Dsonar.projectKey=redknot-monolith -Dsonar.projectName='Redknot Monolith'"
                }
            }
        }

        stage('Archive') {
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            junit '**/target/surefire-reports/*.xml'
        }

        if (env.BRANCH_NAME == 'main') {
            stage('Docker Build') {
                def image = "145023098958.dkr.ecr.us-east-2.amazonaws.com/redknot-monolith:${version}-${env.BUILD_NUMBER}"
                sh "docker build -t ${image} ."
                // TODO: move to shared library docker push
                sh "aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 145023098958.dkr.ecr.us-east-2.amazonaws.com"
                sh "docker push ${image}"
                currentBuild.description = "Image: ${image}"
            }
        }

        currentBuild.result = 'SUCCESS'
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        // Always notify - this was added after the incident in 2023-11
        if (currentBuild.result == 'FAILURE') {
            slackSend(
                channel: '#legacy-builds',
                color: 'danger',
                message: "MONOLITH BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)",
                tokenCredentialId: 'slack-webhook'
            )
            emailext(
                to: 'legacy-support@redknot-enterprises.com',
                subject: "BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Check console output at ${env.BUILD_URL}",
                attachLog: true
            )
        }
        cleanWs()
    }
}

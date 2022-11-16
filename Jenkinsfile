pipeline {
    agent {
        docker {
            image 'maven:3.8.1-adoptopenjdk-11' 
            args '-v /root/.m2:/root/.m2' 
        }
    }
    environment {
       CI = 'false'
    }
    stages {
        stage('Build') { 
            steps {
                sh 'mvn build' 
            }
        }
    }
}


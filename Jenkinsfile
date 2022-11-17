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
        stage('Run tests') { 
            environment {
                CI = 'false'
            }
            steps {
                sh 'ls'
                echo '=========='
                //sh 'ls node'
                //echo '=========='
                //sh 'rm -rf node'
                //echo '=========='
                //sh 'ls'
                echo '=========='
                sh 'mvn test'
            }
        }
    }
}

pipeline {
    agent {
        docker {
            image '3.8.6-openjdk-8' 
            args '-v /root/.m2:/root/.m2' 
        }
    }
    stages {
        stage('Run tests') { 
            steps {
                sh 'mvn test' 
            }
        }
    }
}
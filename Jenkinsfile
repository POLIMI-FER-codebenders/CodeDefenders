pipeline {
    agent none
    environment {
       CI = 'false'
    }
    stages {
        stage('Run tests') { 
            agent {
                // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
                dockerfile {
                    filename 'Dockerfile.test'
                    //dir 'build'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn test'
            }
        }
        stage('Docker build') { 
            agent {
                docker {
                    image 'maven:3.8.1-adoptopenjdk-11'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                sh 'ls'
                sh 'docker image ls'
                //sh 'docker build -f docker/Dockerfile .'
                //sh 'docker image ls'
            }
        }
    }
}

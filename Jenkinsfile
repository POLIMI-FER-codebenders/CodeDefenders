pipeline {
    agent {
        // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
        docker {
            image 'maven:3.6.3-openjdk-11'
            //dir 'jenkins'
            //label 'my-defined-label'
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
                sh 'mvn -v'
                sh 'npm -v'
                sh 'node -v'
                sh 'sudo mvn -X test' 
            }
        }
    }
}


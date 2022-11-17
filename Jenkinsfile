pipeline {
    agent {
        // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
        dockerfile {
            filename 'Dockerfile.test'
            //dir 'build'
            args '-v /root/.m2:/root/.m2'
        }
    }
    environment {
       CI = 'false'
    }
    stages {
        stage('Run tests') { 
            steps {
                sh 'mvn test'
            }
        }
        stage('Docker build') { 
            steps {
                sh 'docker'
            }
        }
    }
}

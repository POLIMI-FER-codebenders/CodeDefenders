pipeline {
    agent {
        // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
        docker {
            image 'maven:3.8-openjdk-11'
            //dir 'jenkins'
            //label 'my-defined-label'
            args '-v /root/.m2:/root/.m2' 
        }
    }
    stages {
        stage('Run tests') { 
            steps {
                sh 'mvn -X test' 
            }
        }
    }
}


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
            steps {
                env.CI = false
                sh 'mvn -v'
                sh 'mvn -X test' 
            }
        }
    }
}


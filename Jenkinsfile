pipeline {
    agent {
        // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
        dockerfile {
            filename 'Dockerfile.jenkins_agent'
            //dir 'build'
            //label 'my-defined-label'
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


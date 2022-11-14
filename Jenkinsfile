pipeline {
    
    environment {
       CI = 'false'
    }
    stages {
        stage('Run tests') { 
            agent {
                // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
                dockerfile {
                filename 'Dockerfile.jenkins_agent'
                //dir 'build'
                args '-v /root/.m2:/root/.m2'
                }
            }
            environment {
                CI = 'false'
            }
            steps {
                sh 'npm -v'
                sh 'mvn -X test'
            }
        }
    }
}


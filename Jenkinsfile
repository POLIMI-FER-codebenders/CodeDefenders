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
            // dind cache working? #3
            agent any
            environment {
		        DOCKERHUB_CREDENTIALS=credentials('dockerhub_access')
	        }
            steps {
                sh 'ls'
                //echo "Running commit: ${env.GIT_COMMIT}"
                sh "docker build --file ./docker/Dockerfile.deploy --tag hrom459/codedefenders:${env.GIT_COMMIT} ."
                //sh 'docker build -f docker/Dockerfile .'
                sh 'docker image ls'
                sh "echo ${DOCKERHUB_CREDENTIALS_USR}"
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW}"
                sh "echo ${DOCKERHUB_CREDENTIALS}"
                sh "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
                sh "docker push hrom459/codedefenders:${env.GIT_COMMIT}"
            }
        }
    }
}

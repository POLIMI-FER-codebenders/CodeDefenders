pipeline {
    agent none
    environment {
       CI = 'false'
    }
    
    stages {
        stage('Start job discord notify'){
            agent any
            environment {
		        DISCORD_WEBHOOK=credentials('discord_webhook')
	        }
            steps {
                discordSend (
                    description: "Job started", 
                    footer: "ETA 10min", 
                    link: env.BUILD_URL, 
                    result: currentBuild.currentResult, 
                    title: JOB_NAME, 
                    webhookURL: "$DISCORD_WEBHOOK"
                )
            }
        }
        stage('Run tests') { 
            agent {
                // Equivalent to "docker build -f Dockerfile.build --build-arg version=1.0.2 ./build/
                dockerfile {
                    filename 'Dockerfile.test'
                    //dir 'build'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            environment {
		        DISCORD_WEBHOOK=credentials('discord_webhook')
	        }
            steps {
                sh 'mvn test'
            }
        }
        stage('Docker build') { 
            // dind cache working? #4
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
    post{
        always{/*
        environment {
		        DISCORD_WEBHOOK=credentials('discord_webhook')
	        }*/
                echo "hello world"
                echo currentBuild.currentResult
                script{if (currentBuild.currentResult == 'SUCCESS') {
                discordSend (
                    description: "Job finished", 
                    footer: "Your image: hrom459/codedefenders:${env.GIT_COMMIT}", 
                    link: env.BUILD_URL, 
                    result: currentBuild.currentResult, 
                    title: JOB_NAME, 
                    webhookURL: "$DISCORD_WEBHOOK"
                )
            } else {
                discordSend (
                        description: "Job is not successful", 
                        footer: "You should checkout why", 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: "$DISCORD_WEBHOOK"
                    )
            }
            }
            
        }   
    }
}

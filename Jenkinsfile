pipeline {
    agent any
    environment {
       CI = 'false'
       DISCORD_WEBHOOK=credentials('discord_webhook')
    }
    
    stages {
        stage('Start job discord notify'){
            agent any
            steps {
                discordSend (
                    description: "Job started", 
                    footer: "ETA ~10min", 
                    link: env.BUILD_URL, 
                    result: "UNSTABLE", // so we get yellow color in discord, 
                    title: JOB_NAME, 
                    webhookURL: '$DISCORD_WEBHOOK'
                )
                sh 'printenv'
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
                sh "docker build --file ./docker/Dockerfile.deploy --tag codebenders/codedefenders:${env.GIT_COMMIT} ."
                //sh 'docker build -f docker/Dockerfile .'
                sh 'docker image ls'
                sh "echo ${DOCKERHUB_CREDENTIALS_USR}"
                sh "echo ${DOCKERHUB_CREDENTIALS_PSW}"
                sh "echo ${DOCKERHUB_CREDENTIALS}"
                sh "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
                sh "docker push codebenders/codedefenders:${env.GIT_COMMIT}"
            }
        }
        
    }
    post{
        success{
                echo currentBuild.currentResult
                discordSend (
                    description: "Job successful on branch ${env.GIT_BRANCH}", 
                    footer: "Your image: codebenders/codedefenders:${env.GIT_COMMIT}", 
                    link: env.BUILD_URL, 
                    result: currentBuild.currentResult, 
                    title: JOB_NAME, 
                    webhookURL: "$DISCORD_WEBHOOK"
                )
        } 
        unsuccessful {
                echo currentBuild.currentResult
                echo "${env.GIT_BRANCH}"
                sh 'printenv'
                discordSend (
                        description: "Job is not successful on branch ${env.GIT_BRANCH}", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: "$DISCORD_WEBHOOK"
                    )
        }
    }
}

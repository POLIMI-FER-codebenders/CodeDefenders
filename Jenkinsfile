pipeline {
    agent any
    environment {
       CI = 'false'
       DISCORD_WEBHOOK = credentials('discord_webhook')
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
                    webhookURL: DISCORD_WEBHOOK
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

                /*publishChecks (
                    name: 'mavening', 
                    title: 'Maven tests', 
                    summary: 'mvn test ran and returned no errors',
                    text: 'mvn test => success',
                    detailsURL: "${env.RUN_DISPLAY_URL}",
                    //actions: [[label:'Tilt', description:'Tilt', identifier:'Tilt']]
                )*/
            }
            post{
                always{
                    publishChecks (
                        name: 'mavening', 
                        title: 'Maven tests', 
                        summary: 'mvn test ran and returned no errors',
                        text: 'mvn test => success',
                        detailsURL: "${env.RUN_DISPLAY_URL}",
                        //actions: [[label:'Tilt', description:'Tilt', identifier:'Tilt']]
                    )
                }
            }
        }
        stage('Docker build') {
            agent any
            environment {
		        DOCKERHUB_CREDENTIALS = credentials('dockerhub_access')
	        }
            steps {
                sh "docker build --file ./docker/Dockerfile.deploy --tag codebenders/codedefenders:${env.GIT_COMMIT} ."
                sh "docker push codebenders/codedefenders:${env.GIT_COMMIT}"

                /*publishChecks (
                    name: 'dockering', 
                    title: 'Docker build and push', 
                    summary: 'Build docker image and publish to dockerhub',
                    text: "docker build & push => success\nYour image: codebenders/codedefenders:${env.GIT_COMMIT}",
                    detailsURL: "${env.RUN_DISPLAY_URL}",
                )*/
            }
            post{
                always{
                        publishChecks (
                        name: 'dockering', 
                        title: 'Docker build and push', 
                        summary: 'Build docker image and publish to dockerhub',
                        text: "docker build & push => success\nYour image: codebenders/codedefenders:${env.GIT_COMMIT}",
                        detailsURL: "${env.RUN_DISPLAY_URL}",
                    )
                }
            }
        }
        
    }
    post{
        success{
                discordSend (
                    description: "Hey ${env.CHANGE_AUTHOR}, job is successful on branch ${env.GIT_BRANCH}", 
                    footer: "Your image: codebenders/codedefenders:${env.GIT_COMMIT}, ${env.CHANGE_AUTHOR}", 
                    link: env.BUILD_URL, 
                    result: currentBuild.currentResult, 
                    title: JOB_NAME, 
                    webhookURL: DISCORD_WEBHOOK
                )
        } 
        unsuccessful {
                discordSend (
                        description: "Hey ${env.CHANGE_AUTHOR}, job is not successful on branch ${env.GIT_BRANCH}", 
                        footer: currentBuild.currentResult, 
                        link: env.BUILD_URL, 
                        result: currentBuild.currentResult, 
                        title: JOB_NAME, 
                        webhookURL: DISCORD_WEBHOOK
                    )
        }
    }
}

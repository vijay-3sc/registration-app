pipeline{
    agent any
    environment{
        DOCKER_CREDENTIALS_ID = credentials('auth_docker')
        DOCKER_REGISTRY_URL= 'https://hub.docker.com/u/vijay3sc'
        REGISTRY_NAME = 'vijay3sc'
        REPO_NAME = 'registraton'
    }
    stages{
        stage('git checkout'){
            steps{
                checkout scmGit(branches:[[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/vijay-3sc/registration-app']])
            }
        }
        stage('build docker image'){
            steps{
                sh 'docker buildx prune  --force'
                sh 'docker build -t ${DOCKER_REGISTRY_URL}/${REPO_NAME}:$BUILD_NUMBER .'
            }
        }
        stage('push image'){
            steps{
                withCredentials([usernamePassword(credentialsId: 'acr_creds', passwordVariable: 'password', usernameVariable:'username')]){
                sh 'docker login -u ${username} -p ${password} ${DOCKER_REGISTRY_URL}'
                sh 'docker push ${DOCKER_REGISTRY_URL}/${REPO_NAME}:$BUILD_NUMBER'

                }
                
            }
        }
        
    
    }
}

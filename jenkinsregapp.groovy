pipeline{
    agent any
    environment{
        ACR_AUTH = credentials('acr_creds')
        ACR_LOGIN_SERVER = 'vijay.azurecr.io'
        REGISTRY_NAME = 'vijay'
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
                sh 'docker build -t ${ACR_LOGIN_SERVER}/${REPO_NAME}:$BUILD_NUMBER .'
            }
        }
        stage('push image'){
            steps{
                withCredentials([usernamePassword(credentialsId: 'acr_creds', passwordVariable: 'password', usernameVariable:'username')]){
                sh 'docker login -u ${username} -p ${password} ${ACR_LOGIN_SERVER}'
                sh 'docker push ${ACR_LOGIN_SERVER}/${REPO_NAME}:$BUILD_NUMBER'

                }
                
            }
        }
        
        stage('Deploy image on VM') {
            steps{
                withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'vm_ssh_credentials', \
                                                             keyFileVariable: 'SSH_KEY_FOR_VM')]) {
                    sshPublisher (
                        alwaysPublishFromMaster: false, 
                        continueOnError: true, 
                        publishers: [
                            sshPublisherDesc(
                            configName: 'vm_dev_server', 
                            verbose: true,
                            transfers: [ 
                                sshTransfer(  execCommand: "sudo docker login ${ACR_LOGIN_SERVER} -u ${ACR_AUTH_USR} -p ${ACR_AUTH_PSW} && sudo docker pull ${REGISTRY_NAME}.azurecr.io/${REPO_NAME}:$BUILD_NUMBER"),
                                sshTransfer(  execCommand: "sudo docker run -dit -p 8000:8080 ${REGISTRY_NAME}.azurecr.io/${REPO_NAME}:$BUILD_NUMBER")
                            ]
                            )
                        ]
                    )
                }
            }
        }
    }
}

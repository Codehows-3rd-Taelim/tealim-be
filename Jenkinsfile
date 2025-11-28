pipeline {
    agent any
    
    environment {
        // 프로젝트 설정
        COMPOSE_PROJECT = 'spring-backend'
        // 호스트 설정 파일 경로
        HOST_CONFIG_PATH = '/var/jenkins_config/application.properties'
        // 컨테이너로 마운트될 경로
        WORKSPACE_CONFIG_DIR = 'config'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Prepare Config') {
            steps {
                script {
                    // 설정 파일 복사 (간소화)
                    sh """
                        mkdir -p ${WORKSPACE_CONFIG_DIR}
                        if [ -f ${HOST_CONFIG_PATH} ]; then
                            cp ${HOST_CONFIG_PATH} ${WORKSPACE_CONFIG_DIR}/application.properties
                        else
                            echo "❌ Error: Config file not found!"
                            exit 1
                        fi
                    """
                }
            }
        }
        
        stage('Build & Update App') {
            steps {
                script {
                    echo '🚀 Updating Spring Backend only...'
                    // 중요: down을 하지 않고 up -d --build로 변경된 이미지만 교체합니다.
                    // Milvus와 MySQL은 건드리지 않습니다.
                    sh """
                        docker compose up -d --build spring-backend
                    """
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    echo '❤️ Checking Spring Boot Health...'
                    // DB 체크는 생략하고(이미 떠있으므로), 스프링만 체크합니다.
                    sh """
                        timeout 60 sh -c 'until docker inspect --format="{{.State.Health.Status}}" spring-backend | grep -q healthy; do sleep 2; done'
                    """
                }
            }
        }
        
        stage('Cleanup') {
            steps {
                script {
                    // 공간 확보를 위해 <none> 태그가 된 댕글링 이미지만 삭제
                    sh "docker image prune -f"
                }
            }
        }
    }
    
    post {
        failure {
            echo '❌ Deployment failed!'
            sh "docker compose logs spring-backend --tail=50"
        }
        always {
            // 보안을 위해 작업 공간 내 설정 파일 삭제
            sh "rm -f ${WORKSPACE_CONFIG_DIR}/application.properties"
        }
    }
}
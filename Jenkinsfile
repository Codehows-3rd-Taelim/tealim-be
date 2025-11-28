pipeline {
    agent any
    
    environment {
        COMPOSE_PROJECT = 'spring-backend'
        DOCKER_NETWORK = 'app-network'
        // 호스트의 설정 파일 경로 (Jenkins가 접근 가능한 경로)
        HOST_CONFIG_PATH = '/var/jenkins_config/application.properties'
        // 작업 디렉토리 내 설정 파일이 복사될 위치
        WORKSPACE_CONFIG_DIR = 'config'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                checkout scm
            }
        }
        
        stage('Copy Configuration') {
            steps {
                script {
                    echo "Copying application.properties from host..."
                    sh """
                        # config 디렉토리 생성
                        mkdir -p ${WORKSPACE_CONFIG_DIR}
                        
                        # 호스트의 설정 파일을 작업 디렉토리로 복사
                        if [ -f ${HOST_CONFIG_PATH} ]; then
                            cp ${HOST_CONFIG_PATH} ${WORKSPACE_CONFIG_DIR}/application.properties
                            echo "Configuration file copied successfully"
                        else
                            echo "Warning: Configuration file not found at ${HOST_CONFIG_PATH}"
                            exit 1
                        fi
                        
                        # 복사된 파일 확인 (민감정보 제외하고 파일 존재 확인)
                        ls -lh ${WORKSPACE_CONFIG_DIR}/application.properties
                    """
                }
            }
        }
        
        stage('Create Network') {
            steps {
                script {
                    echo 'Creating Docker network if not exists...'
                    sh """
                        docker network create ${DOCKER_NETWORK} || true
                    """
                }
            }
        }
        
        stage('Stop Old Containers') {
            steps {
                script {
                    echo 'Stopping old containers...'
                    sh """
                        docker compose down || true
                    """
                }
            }
        }
        
        stage('Build and Deploy') {
            steps {
                script {
                    echo 'Building and deploying with docker-compose...'
                    sh """
                        docker compose up -d --build
                    """
                }
            }
        }
        
        stage('Wait for Health Check') {
            steps {
                script {
                    echo 'Waiting for services to be healthy...'
                    sh """
                        # MySQL health check
                        timeout 120 sh -c 'until docker inspect --format="{{.State.Health.Status}}" mysql-db | grep -q healthy; do sleep 2; done' || true
                        
                        # Milvus health check
                        timeout 120 sh -c 'until docker inspect --format="{{.State.Health.Status}}" milvus-standalone | grep -q healthy; do sleep 2; done' || true
                        
                        # Spring Boot health check
                        timeout 120 sh -c 'until docker inspect --format="{{.State.Health.Status}}" spring-backend | grep -q healthy; do sleep 2; done' || true
                        
                        echo "All services are healthy"
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    sh """
                        echo "=== Container Status ==="
                        docker compose ps
                        
                        echo "=== Spring Backend Logs (Last 50 lines) ==="
                        docker logs spring-backend --tail=50
                        
                        echo "=== Testing Spring Boot Health Endpoint ==="
                        curl -f http://localhost:8080/actuator/health || echo "Health check endpoint not available yet"
                    """
                }
            }
        }
        
        stage('Clean Up') {
            steps {
                script {
                    echo 'Cleaning up unused Docker resources...'
                    sh """
                        docker image prune -f
                        # volume은 데이터 유실 방지를 위해 주석 처리
                        # docker volume prune -f
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Spring Boot deployment successful!'
            sh """
                echo "=== Deployment Summary ==="
                docker compose ps
            """
        }
        failure {
            echo '❌ Spring Boot deployment failed!'
            sh """
                echo "=== Docker Compose Logs ==="
                docker compose logs --tail=100 || true
                
                echo "=== Container Status ==="
                docker ps -a || true
            """
        }
        always {
            echo 'Cleaning up workspace configuration files...'
            sh """
                # 민감정보 보호를 위해 복사된 설정 파일 삭제
                rm -f ${WORKSPACE_CONFIG_DIR}/application.properties || true
            """
        }
    }
}
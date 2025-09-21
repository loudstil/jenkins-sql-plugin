pipeline {
    agent any
    
    tools {
        maven 'Maven-3.8'
        jdk 'JDK-11'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                buildPlugin()
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.hpi', fingerprint: true
                }
            }
        }
        
        stage('Integration Test') {
            steps {
                script {
                    // Example integration test using the plugin
                    sqlQuery connectionId: 'test-h2', sql: '''
                        CREATE TABLE build_info (
                            build_number INT,
                            build_date TIMESTAMP,
                            status VARCHAR(20)
                        );
                        INSERT INTO build_info VALUES (${BUILD_NUMBER}, CURRENT_TIMESTAMP, 'SUCCESS');
                    '''
                    
                    def results = sqlQuery connectionId: 'test-h2', 
                                         sql: 'SELECT * FROM build_info WHERE build_number = ' + BUILD_NUMBER,
                                         returnResult: true
                    
                    echo "Build recorded: ${results}"
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}

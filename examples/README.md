# SQL Plugin Examples

This directory contains example configurations and usage scenarios for the SQL Plugin.

## Database Setup Examples

### MySQL Example
```sql
-- Create a database and user for Jenkins
CREATE DATABASE jenkins_ci;
CREATE USER 'jenkins'@'%' IDENTIFIED BY 'password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER ON jenkins_ci.* TO 'jenkins'@'%';
FLUSH PRIVILEGES;
```

### PostgreSQL Example
```sql
-- Create a database and user for Jenkins
CREATE DATABASE jenkins_ci;
CREATE USER jenkins WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE jenkins_ci TO jenkins;
```

### SQL Server Example
```sql
-- Create a database and user for Jenkins
CREATE DATABASE jenkins_ci;
CREATE LOGIN jenkins WITH PASSWORD = 'Password123!';
USE jenkins_ci;
CREATE USER jenkins FOR LOGIN jenkins;
ALTER ROLE db_datareader ADD MEMBER jenkins;
ALTER ROLE db_datawriter ADD MEMBER jenkins;
ALTER ROLE db_ddladmin ADD MEMBER jenkins;
```

## Pipeline Examples

### Example 1: Database Migration Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Database Migration') {
            steps {
                script {
                    // Check if migration is needed
                    def tables = sqlQuery connectionId: 'production-db', 
                                        sql: "SELECT table_name FROM information_schema.tables WHERE table_schema = 'myapp'",
                                        returnResult: true
                    
                    if (tables.size() == 0) {
                        echo "Running initial database setup..."
                        sqlQuery connectionId: 'production-db', file: 'migrations/001_initial_setup.sql'
                    }
                    
                    // Apply pending migrations
                    sqlQuery connectionId: 'production-db', file: 'migrations/002_add_user_preferences.sql'
                }
            }
        }
    }
}
```

### Example 2: Data Quality Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Data Quality Checks') {
            steps {
                script {
                    // Check for duplicate records
                    def duplicates = sqlQuery connectionId: 'data-warehouse', 
                                            sql: '''
                                                SELECT email, COUNT(*) as count 
                                                FROM users 
                                                GROUP BY email 
                                                HAVING COUNT(*) > 1
                                            ''',
                                            returnResult: true
                    
                    if (duplicates.size() > 0) {
                        error "Found ${duplicates.size()} duplicate email addresses!"
                    }
                    
                    // Check data freshness
                    def staleData = sqlQuery connectionId: 'data-warehouse',
                                           sql: '''
                                               SELECT COUNT(*) as count 
                                               FROM orders 
                                               WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 DAY)
                                           ''',
                                           returnResult: true
                    
                    echo "Found ${staleData[0].count} orders older than 1 day"
                }
            }
        }
    }
}
```

### Example 3: Environment Setup Pipeline

```groovy
pipeline {
    agent any
    
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'production'], description: 'Target environment')
    }
    
    stages {
        stage('Environment Setup') {
            steps {
                script {
                    def connectionId = "${params.ENVIRONMENT}-database"
                    
                    // Create application tables
                    sqlQuery connectionId: connectionId, sql: '''
                        CREATE TABLE IF NOT EXISTS users (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) UNIQUE NOT NULL,
                            email VARCHAR(100) UNIQUE NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        );
                        
                        CREATE TABLE IF NOT EXISTS posts (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT,
                            title VARCHAR(255) NOT NULL,
                            content TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id)
                        );
                    '''
                    
                    // Insert test data for non-production environments
                    if (params.ENVIRONMENT != 'production') {
                        sqlQuery connectionId: connectionId, sql: '''
                            INSERT IGNORE INTO users (username, email) VALUES 
                            ('testuser1', 'test1@example.com'),
                            ('testuser2', 'test2@example.com');
                            
                            INSERT IGNORE INTO posts (user_id, title, content) VALUES 
                            (1, 'First Post', 'This is a test post'),
                            (2, 'Second Post', 'Another test post');
                        '''
                    }
                    
                    // Verify setup
                    def userCount = sqlQuery connectionId: connectionId,
                                           sql: 'SELECT COUNT(*) as count FROM users',
                                           returnResult: true
                    
                    echo "Environment ${params.ENVIRONMENT} setup complete. User count: ${userCount[0].count}"
                }
            }
        }
    }
}
```

### Example 4: Backup and Restore Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Create Backup') {
            steps {
                script {
                    // Create backup table
                    sqlQuery connectionId: 'production-db', sql: '''
                        CREATE TABLE users_backup_${BUILD_NUMBER} AS 
                        SELECT * FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                    '''
                    
                    // Verify backup
                    def backupCount = sqlQuery connectionId: 'production-db',
                                             sql: "SELECT COUNT(*) as count FROM users_backup_${BUILD_NUMBER}",
                                             returnResult: true
                    
                    echo "Backup created with ${backupCount[0].count} records"
                }
            }
        }
        
        stage('Data Processing') {
            steps {
                // Your data processing steps here
                echo "Processing data..."
            }
        }
        
        stage('Cleanup Old Backups') {
            steps {
                script {
                    // Get list of old backup tables
                    def oldBackups = sqlQuery connectionId: 'production-db',
                                            sql: '''
                                                SELECT table_name 
                                                FROM information_schema.tables 
                                                WHERE table_name LIKE 'users_backup_%' 
                                                AND table_name != 'users_backup_${BUILD_NUMBER}'
                                            ''',
                                            returnResult: true
                    
                    // Drop old backup tables (keep only latest)
                    for (backup in oldBackups) {
                        sqlQuery connectionId: 'production-db',
                                sql: "DROP TABLE ${backup.table_name}"
                        echo "Dropped old backup table: ${backup.table_name}"
                    }
                }
            }
        }
    }
}
```

## Error Handling Examples

### Example with Try-Catch

```groovy
pipeline {
    agent any
    
    stages {
        stage('Database Operations') {
            steps {
                script {
                    try {
                        sqlQuery connectionId: 'my-database', sql: '''
                            INSERT INTO audit_log (action, timestamp) 
                            VALUES ('PIPELINE_START', NOW())
                        '''
                    } catch (Exception e) {
                        echo "Failed to log pipeline start: ${e.message}"
                        // Continue with pipeline even if logging fails
                    }
                    
                    // Critical database operation
                    try {
                        sqlQuery connectionId: 'my-database', sql: '''
                            UPDATE product_inventory 
                            SET quantity = quantity - 1 
                            WHERE product_id = 123
                        '''
                    } catch (Exception e) {
                        error "Critical database operation failed: ${e.message}"
                    }
                }
            }
        }
    }
}
```

## Configuration Templates

### Jenkins Global Configuration (Groovy Script)

```groovy
// Script to programmatically configure database connections
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration
import io.jenkins.plugins.sql.model.DatabaseConnection
import hudson.util.Secret

def config = SqlGlobalConfiguration.get()
def connections = []

// Add MySQL connection
connections.add(new DatabaseConnection(
    'mysql-prod',
    'Production MySQL',
    'com.mysql.cj.jdbc.Driver',
    'jdbc:mysql://mysql.example.com:3306/production',
    'jenkins',
    Secret.fromString('secure_password')
))

// Add PostgreSQL connection
connections.add(new DatabaseConnection(
    'postgres-analytics',
    'Analytics PostgreSQL',
    'org.postgresql.Driver', 
    'jdbc:postgresql://analytics.example.com:5432/warehouse',
    'jenkins_user',
    Secret.fromString('another_password')
))

config.setDatabaseConnections(connections)
config.save()

println "Database connections configured successfully"
```

## Testing Examples

### Unit Test for Custom SQL Functions

```groovy
@Test
void testCustomFunction() {
    // Setup test database
    sqlQuery connectionId: 'test-h2', sql: '''
        CREATE ALIAS GET_QUARTER FOR "java.lang.Math.ceil";
        CREATE TABLE sales (date DATE, amount DECIMAL(10,2));
        INSERT INTO sales VALUES ('2023-01-15', 1000.00), ('2023-04-10', 1500.00);
    '''
    
    // Test custom function
    def result = sqlQuery connectionId: 'test-h2',
                        sql: 'SELECT GET_QUARTER(MONTH(date)/3.0) as quarter, SUM(amount) as total FROM sales GROUP BY quarter',
                        returnResult: true
    
    assertEquals(2, result.size())
}

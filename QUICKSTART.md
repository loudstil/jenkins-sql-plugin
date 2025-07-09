# Quick Start Guide - SQL Plugin for Jenkins

## Prerequisites

- Jenkins 2.401.3 or later
- Maven 3.6 or later
- JDK 11 or later
- Access to a database (MySQL, PostgreSQL, SQL Server, Oracle, or H2)

## Building the Plugin

### Windows
```cmd
cd sql-plugin
build.bat
```

### Linux/Mac
```bash
cd sql-plugin
mvn clean package
```

## Installation

1. Build the plugin (see above)
2. Go to **Manage Jenkins** → **Plugin Manager** → **Advanced**
3. Upload `target/sql-plugin.hpi`
4. Restart Jenkins

## Quick Configuration

### Step 1: Configure Database Connections

1. Go to **Manage Jenkins** → **System**
2. Scroll to **SQL Plugin Configuration**
3. Click **Add** to create database connections (you can add multiple):

**Example: Production Database**
   - **Connection ID**: `prod-mysql`
   - **Connection Name**: `Production MySQL Database`
   - **Database Type**: MySQL
   - **JDBC URL**: `jdbc:mysql://prod-db.company.com:3306/app_production`
   - **Username**: `jenkins_prod`
   - **Password**: `secure_prod_password`

**Example: Test Database**
   - **Connection ID**: `test-h2`
   - **Connection Name**: `Test H2 Database`
   - **Database Type**: H2
   - **JDBC URL**: `jdbc:h2:mem:testdb`
   - **Username**: `sa`
   - **Password**: (leave empty)

**Example: Analytics Database**
   - **Connection ID**: `analytics-postgres`
   - **Connection Name**: `Analytics PostgreSQL`
   - **Database Type**: PostgreSQL
   - **JDBC URL**: `jdbc:postgresql://analytics.company.com:5432/warehouse`
   - **Username**: `jenkins_analytics`
   - **Password**: `analytics_password`

4. Click **Test Connection** for each to verify
5. Save the configuration

### Step 2: Create a Multi-Database Pipeline

Create a new Pipeline job that uses multiple database connections:

```groovy
pipeline {
    agent any
    stages {
        stage('Multi-Database Operations') {
            steps {
                script {
                    // Initialize test database
                    sqlQuery connectionId: 'test-h2', sql: '''
                        CREATE TABLE IF NOT EXISTS test_results (
                            id INT PRIMARY KEY,
                            test_name VARCHAR(100),
                            status VARCHAR(20),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    '''
                    
                    // Log test start in test database
                    sqlQuery connectionId: 'test-h2', sql: '''
                        INSERT INTO test_results (id, test_name, status) VALUES 
                        (1, 'Integration Test', 'STARTED')
                    '''
                    
                    // Query analytics data from PostgreSQL
                    def analyticsData = sqlQuery connectionId: 'analytics-postgres',
                                              sql: 'SELECT COUNT(*) as user_count FROM users WHERE active = true',
                                              returnResult: true
                    
                    echo "Active users in analytics DB: ${analyticsData[0].user_count}"
                    
                    // Update production database (be careful!)
                    if (env.BRANCH_NAME == 'main') {
                        sqlQuery connectionId: 'prod-mysql', sql: '''
                            INSERT INTO deployment_log (version, deployed_at) 
                            VALUES ('${BUILD_NUMBER}', NOW())
                        '''
                        echo "Logged deployment to production database"
                    }
                    
                    // Update test completion status
                    sqlQuery connectionId: 'test-h2', sql: '''
                        UPDATE test_results 
                        SET status = 'COMPLETED' 
                        WHERE id = 1
                    '''
                    
                    // Get final test results
                    def testResults = sqlQuery connectionId: 'test-h2',
                                            sql: 'SELECT * FROM test_results',
                                            returnResult: true
                    
                    echo "Test Results:"
                    for (result in testResults) {
                        echo "Test: ${result.test_name}, Status: ${result.status}"
                    }
                }
            }
        }
    }
}
```

### Step 3: Run the Pipeline

1. Save and run the pipeline
2. Check the console output for SQL execution results
3. Verify data in your database

## Managing Multiple Database Connections

### Best Practices for Multiple Connections

1. **Use Descriptive Connection IDs**: Use meaningful names like `prod-mysql`, `test-postgres`, `analytics-warehouse`
2. **Environment-Based Naming**: Consider patterns like `{env}-{dbtype}` (e.g., `dev-mysql`, `staging-postgres`, `prod-oracle`)
3. **Security Considerations**: Use different credentials with appropriate permissions for each environment
4. **Connection Pooling**: Each connection maintains its own connection pool for optimal performance

### Example Multi-Environment Setup

```groovy
// Development Environment Pipeline
pipeline {
    agent any
    stages {
        stage('Development Database Setup') {
            steps {
                // Use development database
                sqlQuery connectionId: 'dev-mysql', sql: '''
                    CREATE DATABASE IF NOT EXISTS myapp_dev;
                    USE myapp_dev;
                    CREATE TABLE IF NOT EXISTS users (id INT, name VARCHAR(100));
                '''
            }
        }
    }
}

// Production Deployment Pipeline
pipeline {
    agent any
    stages {
        stage('Production Migration') {
            when { branch 'main' }
            steps {
                // Use production database with more careful operations
                sqlQuery connectionId: 'prod-mysql', file: 'migrations/prod-migration.sql'
                
                // Log to audit database
                sqlQuery connectionId: 'audit-postgres', sql: '''
                    INSERT INTO deployment_audit (app, version, deployed_by, deployed_at)
                    VALUES ('myapp', '${BUILD_NUMBER}', '${BUILD_USER}', NOW())
                '''
            }
        }
    }
}
```

### Connection Management Features

- **Connection Pooling**: Each database connection uses connection pooling for better performance
- **Connection Testing**: Built-in connection testing for each configured database
- **Credential Security**: Passwords are stored securely using Jenkins Secret management
- **Connection Caching**: Connections are cached and reused across pipeline steps
- **Error Isolation**: Issues with one database connection don't affect others

## Common Database URLs

| Database | JDBC URL Template |
|----------|-------------------|
| MySQL | `jdbc:mysql://localhost:3306/database_name` |
| PostgreSQL | `jdbc:postgresql://localhost:5432/database_name` |
| SQL Server | `jdbc:sqlserver://localhost:1433;databaseName=database_name` |
| Oracle | `jdbc:oracle:thin:@localhost:1521:xe` |
| H2 (embedded) | `jdbc:h2:mem:testdb` |

## Troubleshooting

### Issue: "Driver not found"
- **Solution**: The plugin includes common drivers. For custom drivers, add them to Jenkins classpath.

### Issue: "Connection timeout"
- **Solution**: Check network connectivity and database availability. Increase connection timeout in advanced settings.

### Issue: "Permission denied"
- **Solution**: Ensure the database user has necessary permissions (SELECT, INSERT, UPDATE, DELETE, CREATE, DROP as needed).

### Issue: "SQL syntax error"
- **Solution**: Check SQL syntax for your specific database type. Different databases have slightly different SQL dialects.

## Next Steps

- Explore the [examples](examples/README.md) for more complex use cases
- Set up multiple database connections for different environments
- Create reusable SQL scripts in your repository
- Integrate SQL operations into your CI/CD pipelines

## Support

- Check the Jenkins logs for detailed error messages
- Review the plugin documentation in the README.md
- Test connections using the built-in connection test feature

# SQL Plugin for Jenkins

A Jenkins plugin that allows you to execute SQL queries and scripts from Jenkins pipelines using configured database connections.

## Features

- Execute SQL statements and scripts from Jenkins pipelines
- Support for multiple database types (MySQL, PostgreSQL, SQL Server, Oracle, H2)
- Global configuration of database connections with connection pooling
- Secure password storage using Jenkins Secret management
- Return query results for use in subsequent pipeline steps
- Connection testing and validation
- Detailed logging and error reporting

## Installation

### From GitHub Releases (Recommended)

1. Go to the [Releases](../../releases) page
2. Download the latest `sql-plugin-*.hpi` file
3. In Jenkins, go to **Manage Jenkins** → **Plugin Manager** → **Advanced**
4. Upload the `.hpi` file
5. Restart Jenkins

### Build from Source

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Install the `.hpi` file from the `target` directory in Jenkins through:
   - Manage Jenkins → Plugin Manager → Advanced → Upload Plugin

## Configuration

### Global Configuration

1. Go to **Manage Jenkins** → **System**
2. Scroll down to **SQL Plugin Configuration**
3. Add database connections with the following details:
   - **Connection ID**: Unique identifier for the connection
   - **Connection Name**: Human-readable name
   - **Database Type**: Select from predefined types or choose Custom
   - **JDBC URL**: Database connection URL
   - **Username**: Database username
   - **Password**: Database password (stored securely)
   - **Advanced settings**: Connection pool configuration

### Supported Database Types

- **MySQL**: `jdbc:mysql://localhost:3306/database`
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/database`
- **SQL Server**: `jdbc:sqlserver://localhost:1433;databaseName=database`
- **Oracle**: `jdbc:oracle:thin:@localhost:1521:xe`
- **H2**: `jdbc:h2:mem:testdb`

## Usage

### Pipeline Step: `sqlQuery`

The plugin provides a `sqlQuery` pipeline step with the following parameters:

#### Required Parameters

- `connectionId`: The ID of the database connection configured in global settings

#### Optional Parameters

- `sql`: SQL statement(s) to execute (use this OR `file`, not both)
- `file`: Path to SQL file relative to workspace (use this OR `sql`, not both)
- `returnResult`: Boolean, whether to return SELECT query results (default: false)
- `maxRows`: Maximum number of rows to return/display (default: 1000)

### Examples

#### Execute a simple SQL statement:

```groovy
pipeline {
    agent any
    stages {
        stage('Database Setup') {
            steps {
                sqlQuery connectionId: 'my-database', sql: '''
                    CREATE TABLE IF NOT EXISTS users (
                        id INT PRIMARY KEY,
                        name VARCHAR(100),
                        email VARCHAR(100)
                    )
                '''
            }
        }
    }
}
```

#### Insert data and get results:

```groovy
pipeline {
    agent any
    stages {
        stage('Database Operations') {
            steps {
                // Insert data
                sqlQuery connectionId: 'my-database', sql: '''
                    INSERT INTO users (id, name, email) VALUES 
                    (1, 'John Doe', 'john@example.com'),
                    (2, 'Jane Smith', 'jane@example.com')
                '''
                
                // Query data and use results
                script {
                    def results = sqlQuery connectionId: 'my-database', 
                                         sql: 'SELECT * FROM users WHERE id > 0',
                                         returnResult: true
                    
                    echo "Found ${results.size()} users"
                    for (user in results) {
                        echo "User: ${user.name} (${user.email})"
                    }
                }
            }
        }
    }
}
```

#### Execute SQL from a file:

```groovy
pipeline {
    agent any
    stages {
        stage('Run Migration') {
            steps {
                // Assume migration.sql exists in workspace
                sqlQuery connectionId: 'my-database', file: 'migration.sql'
            }
        }
    }
}
```

#### Multiple statements:

```groovy
pipeline {
    agent any
    stages {
        stage('Database Migration') {
            steps {
                sqlQuery connectionId: 'my-database', sql: '''
                    DROP TABLE IF EXISTS temp_table;
                    CREATE TABLE temp_table (id INT, data VARCHAR(255));
                    INSERT INTO temp_table VALUES (1, 'test data');
                    SELECT COUNT(*) as record_count FROM temp_table;
                '''
            }
        }
    }
}
```

## Security Considerations

- Database passwords are stored using Jenkins Secret management
- Connection pooling limits concurrent connections
- SQL injection protection depends on your SQL statements - use parameterized queries when possible
- Limit database user permissions to only what's necessary for your CI/CD operations

## Troubleshooting

### Common Issues

1. **Driver not found**: Ensure the JDBC driver is included in the plugin dependencies or Jenkins classpath
2. **Connection timeout**: Check network connectivity and database availability
3. **Permission denied**: Verify database user has necessary permissions
4. **SQL syntax errors**: Check SQL statement syntax for your specific database type

### Logging

The plugin logs detailed information about:
- Connection establishment and pooling
- SQL statement execution
- Query results and row counts
- Error messages and stack traces

Check Jenkins system logs for detailed error information.

## Development & Contributing

### Building and Testing

```bash
# Build the plugin
mvn clean compile

# Run tests
mvn test

# Package the plugin
mvn package

# Build and test everything
mvn clean verify
```

### Release Process

This project uses GitHub Actions for automated building and releasing:

1. **Continuous Integration**: Every push and pull request triggers:
   - Build verification on multiple Java versions (11, 17)
   - Test execution
   - Artifact generation

2. **Automated Releases**: When you push a version tag:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```
   
   GitHub Actions will automatically:
   - Build the plugin
   - Run all tests
   - Create a GitHub release
   - Attach the `.hpi` file to the release

3. **Manual Release** (using provided scripts):
   
   **Windows:**
   ```cmd
   release.bat 1.0.0
   ```
   
   **Linux/Mac:**
   ```bash
   chmod +x release.sh
   ./release.sh 1.0.0
   ```

### GitHub Actions Workflow

The `.github/workflows/build-and-release.yml` file defines:

- **Build Matrix**: Tests on Java 11 and 17
- **Artifact Upload**: Builds are saved as GitHub artifacts
- **Release Creation**: Automatic releases for version tags
- **Changelog Integration**: Uses `CHANGELOG.md` for release notes

### Project Structure

```
sql-plugin/
├── src/
│   ├── main/java/io/jenkins/plugins/sql/
│   │   ├── config/         # Global configuration
│   │   ├── model/          # Data models
│   │   ├── service/        # Database services
│   │   └── step/           # Pipeline steps
│   ├── main/resources/     # Jelly UI files and resources
│   └── test/java/          # Unit tests
├── examples/               # Usage examples
├── .github/workflows/      # GitHub Actions
├── pom.xml                 # Maven configuration
├── CHANGELOG.md           # Version history
└── README.md              # This file
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This plugin is licensed under the MIT License.

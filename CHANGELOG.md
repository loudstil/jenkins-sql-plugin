# Changelog

All notable changes to the SQL Plugin for Jenkins will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial development in progress

## [1.0.0] - TBD

### Added
- Initial release of SQL Plugin for Jenkins
- Support for multiple database connections (MySQL, PostgreSQL, SQL Server, Oracle, H2)
- Pipeline step `sqlQuery` for executing SQL statements and scripts
- Global configuration for database connections in Jenkins System Configuration
- Connection pooling and secure credential storage
- SQL script execution from files
- Query result return capability for pipeline use
- Connection testing and validation
- Comprehensive error handling and logging
- Support for multiple statements separated by semicolons
- Configurable row limits for query results

### Features
- **Multi-database support**: Connect to different database types simultaneously
- **Environment separation**: Use different connections for dev/staging/production
- **Security**: Passwords stored using Jenkins Secret management
- **Performance**: Connection pooling for optimal database performance
- **Flexibility**: Execute inline SQL or SQL files from workspace
- **Integration**: Results can be used in subsequent pipeline steps

### Dependencies
- Jenkins 2.401.3 or later
- JDK 11 or later
- Maven 3.6 or later

## Release Process

1. Update version in `pom.xml`
2. Update this changelog with release notes
3. Create and push a tag: `git tag -a v1.0.0 -m "Release v1.0.0"`
4. GitHub Actions will automatically build and create the release
5. The `.hpi` file will be attached to the GitHub release

## Installation

Download the latest `.hpi` file from the [Releases](../../releases) page and install it through:
- Jenkins → Manage Jenkins → Plugin Manager → Advanced → Upload Plugin

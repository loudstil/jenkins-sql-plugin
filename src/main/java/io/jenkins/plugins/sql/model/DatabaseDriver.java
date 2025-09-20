package io.jenkins.plugins.sql.model;

/**
 * Predefined database driver configurations
 */
public enum DatabaseDriver {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/database"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/database"),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://localhost:1433;databaseName=database"),
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@localhost:1521:xe"),
    H2("H2", "org.h2.Driver", "jdbc:h2:mem:testdb");
    
    private final String displayName;
    private final String driverClass;
    private final String urlTemplate;
    
    DatabaseDriver(String displayName, String driverClass, String urlTemplate) {
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlTemplate = urlTemplate;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDriverClass() {
        return driverClass;
    }
    
    public String getUrlTemplate() {
        return urlTemplate;
    }

}

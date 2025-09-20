package io.jenkins.plugins.sql.model;

import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a database connection configuration
 */
public class DatabaseConnection implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String name;
    private String driverClass;
    private String url;
    private String username;
    private Secret password;
    private int maxConnections = 10;
    private int connectionTimeout = 30;
    private boolean testOnBorrow = true;
    
    // Constructor for programmatic creation
    @DataBoundConstructor
    public DatabaseConnection(String id, String name, String driverClass, String url,
                              String username, String password,
                              int maxConnections, int connectionTimeout, boolean testOnBorrow) {
        this.id = id;
        this.name = name;
        this.driverClass = driverClass;
        this.url = url;
        this.username = username;
        this.password = password != null ? Secret.fromString(password) : null;
        this.maxConnections = maxConnections;
        this.connectionTimeout = connectionTimeout;
        this.testOnBorrow = testOnBorrow;
    }


    public String getId() {
        return id != null ? id : "";
    }

    public String getName() {
        return name;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConnection that = (DatabaseConnection) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "DatabaseConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}

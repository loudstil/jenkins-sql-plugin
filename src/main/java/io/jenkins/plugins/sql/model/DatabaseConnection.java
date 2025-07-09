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
    
    private String id;
    private String name;
    private String driverClass;
    private String customDriverClass;
    private String url;
    private String username;
    private Secret password;
    private int maxConnections = 10;
    private int connectionTimeout = 30;
    private boolean testOnBorrow = true;
    
    @DataBoundConstructor
    public DatabaseConnection(String id, String name, String driverClass, String url, String username, Secret password) {
        this.id = id;
        this.name = name;
        this.driverClass = driverClass;
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDriverClass() {
        // Return custom driver class if main driver class is empty and custom is provided
        if ((driverClass == null || driverClass.trim().isEmpty()) && customDriverClass != null && !customDriverClass.trim().isEmpty()) {
            return customDriverClass;
        }
        return driverClass;
    }
    
    public String getCustomDriverClass() {
        return customDriverClass;
    }
    
    @DataBoundSetter
    public void setCustomDriverClass(String customDriverClass) {
        this.customDriverClass = customDriverClass;
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
    
    @DataBoundSetter
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }
    
    @DataBoundSetter
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
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

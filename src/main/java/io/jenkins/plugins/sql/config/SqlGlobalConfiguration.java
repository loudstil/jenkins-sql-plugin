package io.jenkins.plugins.sql.config;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.sql.model.DatabaseConnection;
import io.jenkins.plugins.sql.model.DatabaseDriver;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Global configuration for SQL Plugin
 */
@Extension
public class SqlGlobalConfiguration extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(SqlGlobalConfiguration.class.getName());
    
    private List<DatabaseConnection> databaseConnections = new ArrayList<>();
    private final ConcurrentMap<String, javax.sql.DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    public SqlGlobalConfiguration() {
        load();
    }
    
    public static SqlGlobalConfiguration get() {
        return GlobalConfiguration.all().get(SqlGlobalConfiguration.class);
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        // Clear cache when configuration changes
        dataSourceCache.clear();
        return true;
    }
    
    public List<DatabaseConnection> getDatabaseConnections() {
        return databaseConnections;
    }
    
    public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) {
        this.databaseConnections = databaseConnections != null ? databaseConnections : new ArrayList<>();
    }
    
    @CheckForNull
    public DatabaseConnection getDatabaseConnection(String id) {
        if (id == null) {
            return null;
        }
        return databaseConnections.stream()
                .filter(conn -> id.equals(conn.getId()))
                .findFirst()
                .orElse(null);
    }
    
    public ListBoxModel doFillConnectionIdItems() {
        ListBoxModel items = new ListBoxModel();
        items.add("Select a database connection", "");
        for (DatabaseConnection conn : databaseConnections) {
            items.add(conn.getName() + " (" + conn.getId() + ")", conn.getId());
        }
        return items;
    }
    
    public ListBoxModel doFillDriverClassItems() {
        ListBoxModel items = new ListBoxModel();
        for (DatabaseDriver driver : DatabaseDriver.values()) {
            if (driver != DatabaseDriver.CUSTOM) {
                items.add(driver.getDisplayName(), driver.getDriverClass());
            }
        }
        items.add("Custom", "");
        return items;
    }
    
    public FormValidation doTestConnection(@QueryParameter String driverClass,
                                         @QueryParameter String url,
                                         @QueryParameter String username,
                                         @QueryParameter String password) {
        if (driverClass == null || driverClass.trim().isEmpty()) {
            return FormValidation.error("Driver class is required");
        }
        if (url == null || url.trim().isEmpty()) {
            return FormValidation.error("URL is required");
        }
        
        try {
            Class.forName(driverClass.trim());
            try (Connection conn = DriverManager.getConnection(url.trim(), username, password)) {
                if (conn.isValid(5)) {
                    return FormValidation.ok("Connection successful!");
                } else {
                    return FormValidation.error("Connection is not valid");
                }
            }
        } catch (ClassNotFoundException e) {
            return FormValidation.error("Driver class not found: " + driverClass + ". Make sure the driver is in the classpath.");
        } catch (SQLException e) {
            return FormValidation.error("Connection failed: " + e.getMessage());
        } catch (Exception e) {
            return FormValidation.error("Unexpected error: " + e.getMessage());
        }
    }
    
    public FormValidation doCheckId(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("ID is required");
        }
        if (!value.matches("[a-zA-Z0-9_-]+")) {
            return FormValidation.error("ID can only contain letters, numbers, underscores, and hyphens");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckName(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("Name is required");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckDriverClass(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("Driver class is required");
        }
        return FormValidation.ok();
    }
    
    public FormValidation doCheckUrl(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("URL is required");
        }
        if (!value.startsWith("jdbc:")) {
            return FormValidation.error("URL should start with 'jdbc:'");
        }
        return FormValidation.ok();
    }
    
    @Override
    public String getDisplayName() {
        return "SQL Plugin Configuration";
    }
}

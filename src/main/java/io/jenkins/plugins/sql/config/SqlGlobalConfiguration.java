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
        LOGGER.info("Loading configuration...");
        load();
        LOGGER.info("Configuration loaded successfully.");
    }
    
    public static SqlGlobalConfiguration get() {
        return GlobalConfiguration.all().get(SqlGlobalConfiguration.class);
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        super.configure(req, json);
        try {
            LOGGER.info("Configuring SQL plugin with JSON: " + json.toString());
            
            // Debug: print all keys in the JSON
            for (Object key : json.keySet()) {
                LOGGER.info("JSON key: " + key + " = " + json.get(key));
            }
            
            // Clear existing connections first
            this.databaseConnections = new ArrayList<>();
            
            // Handle repeatable form data - check for different possible structures
            if (json.has("databaseConnections")) {
                LOGGER.info("Found databaseConnections key");
                Object connectionsObj = json.get("databaseConnections");
                handleConnectionsObject(req, connectionsObj);
            } else if (json.has("connection")) {
                LOGGER.info("Found connection key (from var attribute)");
                Object connectionObj = json.get("connection");
                if (connectionObj instanceof net.sf.json.JSONArray) {
                    net.sf.json.JSONArray connectionsArray = (net.sf.json.JSONArray) connectionObj;
                    LOGGER.info("Processing " + connectionsArray.size() + " connections from connection array");
                    for (int i = 0; i < connectionsArray.size(); i++) {
                        JSONObject connJson = connectionsArray.getJSONObject(i);
                        DatabaseConnection conn = req.bindJSON(DatabaseConnection.class, connJson);
                        if (conn != null && conn.getId() != null && !conn.getId().trim().isEmpty()) {
                            this.databaseConnections.add(conn);
                            LOGGER.info("Added connection: " + conn.getId());
                        }
                    }
                } else if (connectionObj instanceof JSONObject) {
                    LOGGER.info("Processing single connection from connection object");
                    DatabaseConnection conn = req.bindJSON(DatabaseConnection.class, (JSONObject) connectionObj);
                    if (conn != null && conn.getId() != null && !conn.getId().trim().isEmpty()) {
                        this.databaseConnections.add(conn);
                        LOGGER.info("Added connection: " + conn.getId());
                    }
                }
            } else {
                LOGGER.info("No connection data found, trying standard binding");
                // Let Jenkins handle the data binding automatically as fallback
                req.bindJSON(this, json);
            }
            
            // Ensure databaseConnections is not null
            if (this.databaseConnections == null) {
                this.databaseConnections = new ArrayList<>();
            }
            
            LOGGER.info("Final result: Configured " + this.databaseConnections.size() + " database connections");
            for (DatabaseConnection conn : this.databaseConnections) {
                if (conn != null) {
                    LOGGER.info("Connection: " + conn.getId() + " - " + conn.getName());
                }
            }
            
            // Save configuration
            save();
            
            // Clear cache when configuration changes
            dataSourceCache.clear();
            
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to save SQL plugin configuration: " + e.getMessage());
            e.printStackTrace();
            throw new Descriptor.FormException("Failed to save SQL plugin configuration: " + e.getMessage(), e, "databaseConnections");
        }
    }
    
    private void handleConnectionsObject(StaplerRequest req, Object connectionsObj) throws Exception {
        if (connectionsObj instanceof net.sf.json.JSONArray) {
            net.sf.json.JSONArray connectionsArray = (net.sf.json.JSONArray) connectionsObj;
            LOGGER.info("Processing " + connectionsArray.size() + " connections from databaseConnections array");
            for (int i = 0; i < connectionsArray.size(); i++) {
                JSONObject connJson = connectionsArray.getJSONObject(i);
                DatabaseConnection conn = req.bindJSON(DatabaseConnection.class, connJson);
                if (conn != null && conn.getId() != null && !conn.getId().trim().isEmpty()) {
                    this.databaseConnections.add(conn);
                    LOGGER.info("Added connection: " + conn.getId());
                }
            }
        } else if (connectionsObj instanceof JSONObject) {
            LOGGER.info("Processing single connection from databaseConnections object");
            DatabaseConnection conn = req.bindJSON(DatabaseConnection.class, (JSONObject) connectionsObj);
            if (conn != null && conn.getId() != null && !conn.getId().trim().isEmpty()) {
                this.databaseConnections.add(conn);
                LOGGER.info("Added connection: " + conn.getId());
            }
        }
    }

    public List<DatabaseConnection> getDatabaseConnections() {
        LOGGER.info("Retrieving database connections, current count: " + (databaseConnections != null ? databaseConnections.size() : 0));
        if (databaseConnections == null) {
            databaseConnections = new ArrayList<>();
        }
        return databaseConnections;
    }
    
    public void setDatabaseConnections(List<DatabaseConnection> databaseConnections) {
        this.databaseConnections = databaseConnections != null ? databaseConnections : new ArrayList<>();
        LOGGER.info("Setting database connections: " + this.databaseConnections.size() + " connections");
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
    
    public ListBoxModel doFillDatabaseConnectionIdItems() {
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
            items.add(driver.getDisplayName(), driver.getDriverClass());
        }
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
        LOGGER.info("Validating ID: " + value);
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

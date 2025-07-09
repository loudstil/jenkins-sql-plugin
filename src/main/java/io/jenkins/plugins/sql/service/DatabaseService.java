package io.jenkins.plugins.sql.service;

import hudson.util.Secret;
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration;
import io.jenkins.plugins.sql.model.DatabaseConnection;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Service for managing database connections and connection pools
 */
public class DatabaseService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseService.class.getName());
    private static final ConcurrentMap<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    private DatabaseService() {
        // Utility class
    }
    
    public static Connection getConnection(String connectionId) throws SQLException {
        DatabaseConnection dbConfig = SqlGlobalConfiguration.get().getDatabaseConnection(connectionId);
        if (dbConfig == null) {
            throw new SQLException("Database connection '" + connectionId + "' not found in global configuration");
        }
        
        DataSource dataSource = getOrCreateDataSource(dbConfig);
        return dataSource.getConnection();
    }
    
    private static DataSource getOrCreateDataSource(DatabaseConnection dbConfig) {
        return dataSourceCache.computeIfAbsent(dbConfig.getId(), id -> createDataSource(dbConfig));
    }
    
    private static DataSource createDataSource(DatabaseConnection dbConfig) {
        LOGGER.info("Creating data source for connection: " + dbConfig.getId());
        
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(dbConfig.getDriverClass());
        dataSource.setUrl(dbConfig.getUrl());
        dataSource.setUsername(dbConfig.getUsername());
        
        Secret password = dbConfig.getPassword();
        if (password != null) {
            dataSource.setPassword(password.getPlainText());
        }
        
        // Connection pool settings
        dataSource.setMaxTotal(dbConfig.getMaxConnections());
        dataSource.setMaxIdle(dbConfig.getMaxConnections() / 2);
        dataSource.setMinIdle(1);
        dataSource.setTestOnBorrow(dbConfig.isTestOnBorrow());
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setValidationQueryTimeout(dbConfig.getConnectionTimeout());
        
        return dataSource;
    }
    
    public static void clearCache() {
        LOGGER.info("Clearing database connection cache");
        for (DataSource dataSource : dataSourceCache.values()) {
            if (dataSource instanceof BasicDataSource) {
                try {
                    ((BasicDataSource) dataSource).close();
                } catch (SQLException e) {
                    LOGGER.warning("Error closing data source: " + e.getMessage());
                }
            }
        }
        dataSourceCache.clear();
    }
    
    public static void removeCachedConnection(String connectionId) {
        DataSource dataSource = dataSourceCache.remove(connectionId);
        if (dataSource instanceof BasicDataSource) {
            try {
                ((BasicDataSource) dataSource).close();
            } catch (SQLException e) {
                LOGGER.warning("Error closing data source for connection " + connectionId + ": " + e.getMessage());
            }
        }
    }
}

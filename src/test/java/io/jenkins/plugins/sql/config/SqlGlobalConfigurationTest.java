package io.jenkins.plugins.sql.config;

import hudson.util.Secret;
import io.jenkins.plugins.sql.model.DatabaseConnection;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SqlGlobalConfigurationTest {
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testGlobalConfiguration() {
        SqlGlobalConfiguration config = SqlGlobalConfiguration.get();
        assertNotNull(config);
        
        // Test setting connections
        List<DatabaseConnection> connections = new ArrayList<>();
        DatabaseConnection conn = new DatabaseConnection("test-h2", "Test H2 Database", "org.h2.Driver", "jdbc:h2:mem:testdb", "sa", "", 5,6,true);
        connections.add(conn);
        
        config.setDatabaseConnections(connections);
        
        assertEquals(1, config.getDatabaseConnections().size());
        DatabaseConnection retrievedConn = config.getDatabaseConnection("test-h2");
        assertNotNull(retrievedConn);
        assertEquals("Test H2 Database", retrievedConn.getName());
        assertEquals("org.h2.Driver", retrievedConn.getDriverClass());
    }
    
    @Test
    public void testConnectionNotFound() {
        SqlGlobalConfiguration config = SqlGlobalConfiguration.get();
        DatabaseConnection conn = config.getDatabaseConnection("non-existent");
        assertNull(conn);
    }
}

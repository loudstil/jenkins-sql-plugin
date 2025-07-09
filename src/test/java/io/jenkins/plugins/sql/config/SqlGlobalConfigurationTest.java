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
        connections.add(new DatabaseConnection(
            "test-h2",
            "Test H2 Database",
            "org.h2.Driver",
            "jdbc:h2:mem:testdb",
            "sa",
            Secret.fromString("")
        ));
        
        config.setDatabaseConnections(connections);
        
        assertEquals(1, config.getDatabaseConnections().size());
        DatabaseConnection conn = config.getDatabaseConnection("test-h2");
        assertNotNull(conn);
        assertEquals("Test H2 Database", conn.getName());
        assertEquals("org.h2.Driver", conn.getDriverClass());
    }
    
    @Test
    public void testConnectionNotFound() {
        SqlGlobalConfiguration config = SqlGlobalConfiguration.get();
        DatabaseConnection conn = config.getDatabaseConnection("non-existent");
        assertNull(conn);
    }
}

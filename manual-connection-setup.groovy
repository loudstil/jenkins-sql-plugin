// Manual Connection Creation Script
// Run this in Manage Jenkins → Script Console to manually create test connections

import io.jenkins.plugins.sql.config.SqlGlobalConfiguration
import io.jenkins.plugins.sql.model.DatabaseConnection
import hudson.util.Secret

println "=== Manual Connection Creation ==="

try {
    // Get the configuration
    def config = SqlGlobalConfiguration.get()
    println "Got configuration: ${config}"
    
    // Create test connections
    def connections = []
    
    // H2 Test Connection
    connections.add(new DatabaseConnection(
        "test-h2",
        "Test H2 Database",
        "org.h2.Driver",
        "jdbc:h2:mem:testdb",
        "sa",
        Secret.fromString("")
    ))
    
    // MySQL Example Connection (you can modify these)
    connections.add(new DatabaseConnection(
        "example-mysql",
        "Example MySQL Database",
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://localhost:3306/test",
        "root",
        Secret.fromString("password")
    ))
    
    // Set the connections
    config.setDatabaseConnections(connections)
    
    // Save the configuration
    config.save()
    
    println "✓ Created and saved ${connections.size()} connections:"
    connections.each { conn ->
        println "  - ${conn.getId()}: ${conn.getName()}"
    }
    
    // Verify they were saved
    def savedConfig = SqlGlobalConfiguration.get()
    def savedConnections = savedConfig.getDatabaseConnections()
    
    println ""
    println "✓ Verification - Found ${savedConnections.size()} saved connections:"
    savedConnections.each { conn ->
        println "  - ${conn.getId()}: ${conn.getName()} (${conn.getDriverClass()})"
    }
    
    if (savedConnections.size() == connections.size()) {
        println ""
        println "🎉 SUCCESS! Connections were created and saved successfully!"
        println ""
        println "You should now see these connections in:"
        println "Manage Jenkins → System → SQL Plugin Configuration"
    } else {
        println ""
        println "⚠️  WARNING: Not all connections were saved properly"
    }
    
} catch (Exception e) {
    println "❌ ERROR: ${e.message}"
    e.printStackTrace()
}

// Instructions for next steps
println ""
println "=== Next Steps ==="
println "1. Go to Manage Jenkins → System"
println "2. Scroll to 'SQL Plugin Configuration'"
println "3. You should see the connections listed above"
println "4. You can edit, test, or delete them from there"
println "5. To use in a pipeline:"
println "   sqlQuery connectionId: 'test-h2', sql: 'SELECT 1'"

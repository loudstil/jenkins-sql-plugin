// Enhanced Diagnostic Script with Connection Creation Test
// Run this in Manage Jenkins → Script Console

import jenkins.model.Jenkins
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration
import io.jenkins.plugins.sql.model.DatabaseConnection
import hudson.util.Secret
import java.io.File

println "=== Enhanced SQL Plugin Diagnostic ==="
println "Date: ${new Date()}"
println ""

// 1. Check Jenkins Home and current state
def jenkinsHome = Jenkins.getInstance().getRootDir()
println "Jenkins Home: ${jenkinsHome.absolutePath}"
println ""

// 2. Check current plugin state
try {
    def config = SqlGlobalConfiguration.get()
    println "SQL Plugin Configuration Status: LOADED"
    println "Configuration Class: ${config.getClass().getName()}"
    
    def connections = config.getDatabaseConnections()
    println "Current connections count: ${connections.size()}"
    
    if (connections.size() > 0) {
        println "\nExisting Connections:"
        connections.eachWithIndex { conn, index ->
            println "  ${index + 1}. ID: '${conn.getId()}', Name: '${conn.getName()}'"
        }
    }
    
    println ""
    
    // 3. Test creating a connection programmatically
    println "=== Testing Connection Creation ==="
    
    def testConnection = new DatabaseConnection(
        "test-h2-diagnostic",
        "Test H2 Connection (Diagnostic)",
        "org.h2.Driver",
        "jdbc:h2:mem:testdb",
        "sa",
        Secret.fromString("")
    )
    
    println "Created test connection object: ${testConnection}"
    
    // Add to configuration
    def existingConnections = new ArrayList(config.getDatabaseConnections())
    
    // Remove any existing test connection
    existingConnections.removeIf { it.getId() == "test-h2-diagnostic" }
    
    // Add new test connection
    existingConnections.add(testConnection)
    
    // Update configuration
    config.setDatabaseConnections(existingConnections)
    
    println "Added test connection to configuration"
    println "New connections count: ${config.getDatabaseConnections().size()}"
    
    // 4. Try to save configuration
    try {
        config.save()
        println "Configuration save() called successfully"
        
        // Verify it was saved
        def newConfig = SqlGlobalConfiguration.get()
        def savedConnections = newConfig.getDatabaseConnections()
        println "After save - connections count: ${savedConnections.size()}"
        
        def testConn = savedConnections.find { it.getId() == "test-h2-diagnostic" }
        if (testConn) {
            println "✓ Test connection was saved successfully!"
            println "  ID: ${testConn.getId()}"
            println "  Name: ${testConn.getName()}"
            println "  Driver: ${testConn.getDriverClass()}"
            println "  URL: ${testConn.getUrl()}"
        } else {
            println "✗ Test connection was NOT saved"
        }
        
    } catch (Exception saveException) {
        println "✗ Error saving configuration: ${saveException.message}"
        saveException.printStackTrace()
    }
    
} catch (Exception e) {
    println "SQL Plugin Configuration Status: ERROR"
    println "Error: ${e.message}"
    e.printStackTrace()
}

println ""

// 5. Check configuration file after save attempt
println "=== Configuration File Check ==="
def configFile = new File(jenkinsHome, "config.xml")
if (configFile.exists()) {
    def content = configFile.text
    if (content.contains("SqlGlobalConfiguration")) {
        println "✓ config.xml contains SqlGlobalConfiguration"
        
        // Extract and show SQL config
        def startMarker = "<io.jenkins.plugins.sql.config.SqlGlobalConfiguration"
        def endMarker = "</io.jenkins.plugins.sql.config.SqlGlobalConfiguration>"
        
        def startIndex = content.indexOf(startMarker)
        if (startIndex >= 0) {
            def endIndex = content.indexOf(endMarker, startIndex)
            if (endIndex >= 0) {
                def sqlConfigSection = content.substring(startIndex, endIndex + endMarker.length())
                println "SQL Configuration in config.xml:"
                println sqlConfigSection
            }
        }
    } else {
        println "✗ config.xml does NOT contain SqlGlobalConfiguration"
        println "This suggests the configuration is not being saved to the main config file"
    }
} else {
    println "✗ config.xml file does not exist!"
}

// 6. Check for separate configuration file
def sqlConfigFile = new File(jenkinsHome, "io.jenkins.plugins.sql.config.SqlGlobalConfiguration.xml")
println ""
println "Separate SQL config file (${sqlConfigFile.name}):"
println "  Exists: ${sqlConfigFile.exists()}"
if (sqlConfigFile.exists()) {
    println "  Content:"
    println sqlConfigFile.text
}

println ""
println "=== Troubleshooting Steps ==="
println "If connections are still not saving:"
println "1. Check Jenkins logs for errors"
println "2. Verify the plugin is properly installed and loaded"
println "3. Try restarting Jenkins"
println "4. Check file permissions on Jenkins home directory"
println "5. Manually create a connection through the UI and check if it appears"

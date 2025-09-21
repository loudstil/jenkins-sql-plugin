// Jenkins Script Console - Diagnostic Script for SQL Plugin Connections
// Run this in Manage Jenkins → Script Console

import jenkins.model.Jenkins
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration
import java.io.File

println "=== SQL Plugin Connection Diagnostic ==="
println "Date: ${new Date()}"
println ""

// 1. Check Jenkins Home
def jenkinsHome = Jenkins.getInstance().getRootDir()
println "Jenkins Home: ${jenkinsHome.absolutePath}"
println ""

// 2. Check if SQL Plugin Configuration exists
try {
    def config = SqlGlobalConfiguration.get()
    println "SQL Plugin Configuration Status: LOADED"
    
    def connections = config.getDatabaseConnections()
    println "Number of connections found: ${connections.size()}"
    
    if (connections.size() > 0) {
        println "\nConfigured Connections:"
        connections.eachWithIndex { conn, index ->
            println "  ${index + 1}. ID: '${conn.getUuid()}'"
            println "     Name: '${conn.getName()}'"
            println "     Driver: '${conn.getDriverClass()}'"
            println "     URL: '${conn.getUrl()}'"
            println "     Username: '${conn.getUsername()}'"
            println "     Max Connections: ${conn.getMaxConnections()}"
            println ""
        }
    } else {
        println "No connections are currently configured."
    }
} catch (Exception e) {
    println "SQL Plugin Configuration Status: ERROR"
    println "Error: ${e.message}"
    println "This might indicate the plugin is not properly installed or loaded."
}

println ""

// 3. Check for configuration files
def configFiles = [
    "config.xml",
    "io.jenkins.plugins.sql.config.SqlGlobalConfiguration.xml"
]

println "=== Configuration Files Check ==="
configFiles.each { fileName ->
    def configFile = new File(jenkinsHome, fileName)
    println "File: ${fileName}"
    println "  Exists: ${configFile.exists()}"
    println "  Path: ${configFile.absolutePath}"
    
    if (configFile.exists()) {
        println "  Size: ${configFile.length()} bytes"
        println "  Last Modified: ${new Date(configFile.lastModified())}"
        
        // Check if file contains SQL plugin configuration
        def content = configFile.text
        if (content.contains("SqlGlobalConfiguration") || content.contains("sql.config")) {
            println "  Contains SQL Plugin Config: YES"
            
            // Extract SQL config section
            def startIndex = content.indexOf("<io.jenkins.plugins.sql.config.SqlGlobalConfiguration")
            if (startIndex >= 0) {
                def endIndex = content.indexOf("</io.jenkins.plugins.sql.config.SqlGlobalConfiguration>", startIndex)
                if (endIndex >= 0) {
                    def sqlConfig = content.substring(startIndex, endIndex + 56)
                    println "  SQL Configuration Section:"
                    println "    ${sqlConfig.split('\n').join('\n    ')}"
                }
            }
        } else {
            println "  Contains SQL Plugin Config: NO"
        }
    }
    println ""
}

// 4. Check plugins directory
def pluginsDir = new File(jenkinsHome, "plugins")
println "=== Plugins Directory Check ==="
println "Plugins directory: ${pluginsDir.absolutePath}"

if (pluginsDir.exists()) {
    def sqlPluginFiles = pluginsDir.listFiles().findAll { 
        it.name.toLowerCase().contains("sql") 
    }
    
    println "SQL-related plugin files:"
    sqlPluginFiles.each { file ->
        println "  ${file.name} (${file.isDirectory() ? 'directory' : 'file'})"
    }
    
    if (sqlPluginFiles.isEmpty()) {
        println "  No SQL-related plugin files found"
        println "  This suggests the SQL plugin may not be installed"
    }
} else {
    println "Plugins directory does not exist!"
}

println ""
println "=== Summary ==="
println "If no connections were found:"
println "1. The SQL plugin may not be installed"
println "2. Connections may not have been saved properly"
println "3. There may be permission issues with config files"
println "4. Jenkins may need to be restarted after plugin installation"

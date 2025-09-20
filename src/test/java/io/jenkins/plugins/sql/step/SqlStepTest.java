package io.jenkins.plugins.sql.step;

import hudson.util.Secret;
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration;
import io.jenkins.plugins.sql.model.DatabaseConnection;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SqlStepTest {
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testSqlStepExecution() throws Exception {
        // Setup H2 database connection
        SqlGlobalConfiguration config = SqlGlobalConfiguration.get();
        List<DatabaseConnection> connections = new ArrayList<>();
        DatabaseConnection conn = new DatabaseConnection("test-h2", "Test H2 Database", "org.h2.Driver", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "", 10,30,true);
        connections.add(conn);
        config.setDatabaseConnections(connections);
        
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition(
            "node {\n" +
            "  sqlQuery connectionId: 'test-h2', sql: 'CREATE TABLE test (id INT, name VARCHAR(50))'\n" +
            "  sqlQuery connectionId: 'test-h2', sql: \"INSERT INTO test VALUES (1, 'John'), (2, 'Jane')\"\n" +
            "  def result = sqlQuery connectionId: 'test-h2', sql: 'SELECT * FROM test', returnResult: true\n" +
            "  echo \"Result: ${result}\"\n" +
            "}", true));
        
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Successfully executed", run);
        jenkins.assertLogContains("Rows affected: 2", run);
    }
    
    @Test
    public void testSqlStepWithFile() throws Exception {
        // Setup H2 database connection
        SqlGlobalConfiguration config = SqlGlobalConfiguration.get();
        List<DatabaseConnection> connections = new ArrayList<>();
        DatabaseConnection conn = new DatabaseConnection("test-h2", "Test H2 Database", "org.h2.Driver", "jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1", "sa", "", 10, 30, true);
        connections.add(conn);
        config.setDatabaseConnections(connections);
        
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-file");
        job.setDefinition(new CpsFlowDefinition(
            "node {\n" +
            "  writeFile file: 'test.sql', text: 'CREATE TABLE users (id INT, email VARCHAR(100));'\n" +
            "  sqlQuery connectionId: 'test-h2', file: 'test.sql'\n" +
            "}", true));
        
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Executing SQL from file: test.sql", run);
        jenkins.assertLogContains("Successfully executed", run);
    }
}

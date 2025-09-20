package io.jenkins.plugins.sql.step;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import io.jenkins.plugins.sql.config.SqlGlobalConfiguration;
import io.jenkins.plugins.sql.service.DatabaseService;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Pipeline step for executing SQL queries
 */
public class SqlStep extends Step {
    
    private final String connectionId;
    private String sql;
    private String file;
    private boolean returnResult = false;
    private int maxRows = 1000;
    
    @DataBoundConstructor
    public SqlStep(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public String getSql() {
        return sql;
    }
    
    @DataBoundSetter
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    public String getFile() {
        return file;
    }
    
    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }
    
    public boolean isReturnResult() {
        return returnResult;
    }
    
    @DataBoundSetter
    public void setReturnResult(boolean returnResult) {
        this.returnResult = returnResult;
    }
    
    public int getMaxRows() {
        return maxRows;
    }
    
    @DataBoundSetter
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }
    
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SqlStepExecution(context, this);
    }
    
    public static class SqlStepExecution extends SynchronousStepExecution<Object> {
        private final SqlStep step;
        
        SqlStepExecution(StepContext context, SqlStep step) {
            super(context);
            this.step = step;
        }
        
        @Override
        protected Object run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath workspace = getContext().get(FilePath.class);
            PrintStream logger = listener.getLogger();
            
            // Validate input
            if (step.sql == null && step.file == null) {
                throw new IllegalArgumentException("Either 'sql' or 'file' parameter must be provided");
            }
            
            if (step.sql != null && step.file != null) {
                throw new IllegalArgumentException("Only one of 'sql' or 'file' parameter can be provided");
            }
            
            // Get SQL content
            String sqlContent;
            if (step.sql != null) {
                sqlContent = step.sql;
                logger.println("Executing SQL statement...");
            } else {
                FilePath sqlFile = workspace.child(step.file);
                if (!sqlFile.exists()) {
                    throw new IllegalArgumentException("SQL file not found: " + step.file);
                }
                sqlContent = sqlFile.readToString();
                logger.println("Executing SQL from file: " + step.file);
            }
            
            logger.println("Using database connection: " + step.connectionId);
            
            // Execute SQL
            try (Connection connection = DatabaseService.getConnection(step.connectionId)) {
                return executeSql(connection, sqlContent, logger);
            } catch (SQLException e) {
                logger.println("SQL execution failed: " + e.getMessage());
                throw new RuntimeException("SQL execution failed", e);
            }
        }
        
        private Object executeSql(Connection connection, String sqlContent, PrintStream logger) throws SQLException {
            // Split SQL content into individual statements
            String[] statements = sqlContent.split(";");
            List<Map<String, Object>> allResults = new ArrayList<>();
            int executedStatements = 0;
            
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    sql = sql.trim();
                    if (sql.isEmpty()) {
                        continue;
                    }
                    
                    logger.println("Executing: " + sql);
                    executedStatements++;
                    
                    boolean hasResultSet = statement.execute(sql);
                    
                    if (hasResultSet && step.returnResult) {
                        try (ResultSet resultSet = statement.getResultSet()) {
                            List<Map<String, Object>> results = processResultSet(resultSet, logger);
                            allResults.addAll(results);
                        }
                    } else {
                        int updateCount = statement.getUpdateCount();
                        if (updateCount >= 0) {
                            logger.println("Rows affected: " + updateCount);
                        }
                    }
                }
            }
            
            logger.println("Successfully executed " + executedStatements + " statement(s)");
            
            return step.returnResult ? allResults : null;
        }
        
        private List<Map<String, Object>> processResultSet(ResultSet resultSet, PrintStream logger) throws SQLException {
            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Print header
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) header.append("\t");
                header.append(metaData.getColumnName(i));
            }
            logger.println(header.toString());
            
            int rowCount = 0;
            while (resultSet.next() && rowCount < step.maxRows) {
                Map<String, Object> row = new LinkedHashMap<>();
                StringBuilder rowOutput = new StringBuilder();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                    
                    if (i > 1) rowOutput.append("\t");
                    rowOutput.append(value != null ? value.toString() : "NULL");
                }
                
                logger.println(rowOutput.toString());
                results.add(row);
                rowCount++;
            }
            
            if (rowCount == step.maxRows && resultSet.next()) {
                logger.println("... (output truncated at " + step.maxRows + " rows)");
            }
            
            logger.println("Retrieved " + rowCount + " row(s)");
            return results;
        }
    }
    
    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        
        @Override
        public Set<Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            context.add(TaskListener.class);
            context.add(FilePath.class);
            return context;
        }
        
        @Override
        public String getFunctionName() {
            return "sqlQuery";
        }
        
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Execute SQL Query";
        }
        
        public hudson.util.ListBoxModel doFillConnectionIdItems() {
            return SqlGlobalConfiguration.get().doFillDatabaseConnectionIdItems();
        }
    }
}

package oracle.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import oracle.jdbc.dcn.DatabaseChangeRegistration;

/**
 * A mocking class used as a placeholder for the real Oracle JDBC drivers to prevent build errors.
 */
public class OracleConnection implements Connection {

    public static String DCN_NOTIFY_ROWIDS;
    public static String DCN_QUERY_CHANGE_NOTIFICATION;

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        //  Auto-generated method stub
        return false;
    }

    @Override
    public Statement createStatement() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        //  Auto-generated method stub
        return false;
    }

    @Override
    public void commit() throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public void rollback() throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public void close() throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public boolean isClosed() throws SQLException {
        //  Auto-generated method stub
        return false;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        //  Auto-generated method stub
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public String getCatalog() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        //  Auto-generated method stub
        return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public int getHoldability() throws SQLException {
        //  Auto-generated method stub
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        //  Auto-generated method stub
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        //  Auto-generated method stub

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        //  Auto-generated method stub

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public String getSchema() throws SQLException {
        //  Auto-generated method stub
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        //  Auto-generated method stub

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        //  Auto-generated method stub
        return 0;
    }

    public DatabaseChangeRegistration registerDatabaseChangeNotification(@SuppressWarnings("unused") Properties properties) {
        return new DatabaseChangeRegistration();
    }

    public void unregisterDatabaseChangeNotification(@SuppressWarnings("unused") DatabaseChangeRegistration databaseChangeRegistration) {
        // do nothing
    }
}

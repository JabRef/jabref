package net.sf.jabref.remote;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

public class DBConnectorTest {

    @Test
    public void testGetNewMySQLConnection() {
        try {
            DBConnector.getNewConnection(DBType.MYSQL, "localhost", "jabref", "root", "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNewPostgreSQLConnection() {
        try {
            DBConnector.getNewConnection(DBType.POSTGRESQL, "localhost", "jabref", "postgres", "");

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNewMySQLConnectionFail() {
        try {
            DBConnector.getNewConnection(DBType.MYSQL, "XXXX", "XXXX", "XXXX", "XXXX");
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (SQLException e) {
            Assert.assertEquals(0, e.getErrorCode());
        }
    }

    @Test
    public void testGetNewPostgreSQLConnectionFail() {
        try {
            DBConnector.getNewConnection(DBType.POSTGRESQL, "XXXX", "XXXX", "XXXX", "XXXX");
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (SQLException e) {
            Assert.assertEquals(0, e.getErrorCode());
        }
    }

    @Test
    public void testGetDefaultPort() {
        Assert.assertEquals(3306, DBConnector.getDefaultPort(DBType.MYSQL));
        Assert.assertEquals(5432, DBConnector.getDefaultPort(DBType.POSTGRESQL));
        Assert.assertEquals(1521, DBConnector.getDefaultPort(DBType.ORACLE));
        Assert.assertEquals(-1, DBConnector.getDefaultPort(null));
    }
}

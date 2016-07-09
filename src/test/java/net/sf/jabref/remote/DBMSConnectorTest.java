package net.sf.jabref.remote;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;

public class DBMSConnectorTest {

    @Test
    public void testGetNewMySQLConnection() {
        try {
            DBMSConnector.getNewConnection(DBMSType.MYSQL, "localhost", "jabref", "root", "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNewPostgreSQLConnection() {
        try {
            DBMSConnector.getNewConnection(DBMSType.POSTGRESQL, "localhost", "jabref", "postgres", "");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNewMySQLConnectionFail() {
        try {
            DBMSConnector.getNewConnection(DBMSType.MYSQL, "XXXX", "XXXX", "XXXX", "XXXX");
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (SQLException e) {
            Assert.assertEquals(0, e.getErrorCode());
        }
    }

    @Test
    public void testGetNewPostgreSQLConnectionFail() {
        try {
            DBMSConnector.getNewConnection(DBMSType.POSTGRESQL, "XXXX", "XXXX", "XXXX", "XXXX");
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (SQLException e) {
            Assert.assertEquals(0, e.getErrorCode());
        }
    }

}

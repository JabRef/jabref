package net.sf.jabref.remote;

import java.sql.SQLException;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DBMSConnectorTest {

    @Parameter
    public DBMSType dbmsType;


    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return DBMSConnector.getAvailableDBMSTypes();
    }

    @Test
    public void testGetNewConnection() {
        TestConnectionData connectionData = TestConnector.getTestConnectionData(dbmsType);

        try {
            DBMSConnector.getNewConnection(dbmsType, connectionData.getHost(), connectionData.getDatabase(),
                    connectionData.getUser(), connectionData.getPassord());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNewConnectionFail() {
        try {
            DBMSConnector.getNewConnection(dbmsType, "XXXX", "XXXX", "XXXX", "XXXX");
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (SQLException e) {
            Assert.assertEquals(0, e.getErrorCode());
        }
    }
}

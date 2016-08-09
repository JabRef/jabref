package net.sf.jabref.shared;

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
    public void testGetNewConnection() throws ClassNotFoundException, SQLException {
        DBMSConnectionProperties properties = TestConnector.getConnectionProperties(dbmsType);

        try {
            DBMSConnector.getNewConnection(properties);
        } catch (ClassNotFoundException | SQLException e) {
            Assert.fail();
            throw e;
        }
    }

    @Test(expected = SQLException.class)
    public void testGetNewConnectionFail() throws SQLException, ClassNotFoundException {
        DBMSConnector.getNewConnection(new DBMSConnectionProperties(dbmsType, "XXXX", 0, "XXXX", "XXXX", "XXXX"));
    }
}

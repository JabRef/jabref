package net.sf.jabref.shared;

import java.sql.SQLException;
import java.util.Collection;

import net.sf.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DBMSConnectionTest {

    @Parameter
    public DBMSType dbmsType;


    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return TestManager.getDBMSTypeTestParameter();
    }

    @Test
    public void testGetConnection() throws SQLException, InvalidDBMSConnectionPropertiesException {
        DBMSConnectionProperties properties = TestConnector.getTestConnectionProperties(dbmsType);
        Assert.assertNotNull(new DBMSConnection(properties).getConnection());
    }

    @Test(expected = SQLException.class)
    public void testGetConnectionFail() throws SQLException, InvalidDBMSConnectionPropertiesException {
        new DBMSConnection(new DBMSConnectionProperties(dbmsType, "XXXX", 0, "XXXX", "XXXX", "XXXX")).getConnection();
    }
}

package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DatabaseTest
public class DBMSConnectionTest {

    @ParameterizedTest
    @EnumSource(DBMSType.class)
    public void testGetConnection(DBMSType dbmsType) throws SQLException, InvalidDBMSConnectionPropertiesException {
        DBMSConnectionProperties properties = TestConnector.getTestConnectionProperties(dbmsType);
        assertNotNull(new DBMSConnection(properties).getConnection());
    }

    @Test
    public void testGetConnectionFail(DBMSType dbmsType) throws SQLException, InvalidDBMSConnectionPropertiesException {
        assertThrows(SQLException.class,
                () -> new DBMSConnection(new DBMSConnectionProperties(dbmsType, "XXXX", 0, "XXXX", "XXXX", "XXXX")).getConnection());
    }
}

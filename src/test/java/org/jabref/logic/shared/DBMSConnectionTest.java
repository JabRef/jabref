package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DatabaseTest
public class DBMSConnectionTest {

    @ParameterizedTest
    @EnumSource(DBMSType.class)
    public void getConnectionFailsWhenconnectingToInvalidHost(DBMSType dbmsType) {
        assertThrows(SQLException.class,
                () -> new DBMSConnection(new DBMSConnectionPropertiesBuilder().setType(dbmsType).setHost("XXXX").setPort(33778).setDatabase("XXXX").setUser("XXXX").setPassword("XXXX").setUseSSL(false).setServerTimezone("XXXX").createDBMSConnectionProperties()).getConnection());
    }
}

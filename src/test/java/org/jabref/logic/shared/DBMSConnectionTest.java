package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.model.database.shared.DBMSType;
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
                () -> new DBMSConnection(new DBMSConnectionProperties(dbmsType, "XXXX", 33778, "XXXX", "XXXX", "XXXX", false, "XXXX")).getConnection());
    }
}

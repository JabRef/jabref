package org.jabref.logic.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedDatabaseIdentityTest {

    @Test
    void ignoresCredentialsAndConnectionMode() {
        // [utest->req~shared-database.single-tab~1]
        DBMSConnectionProperties fields = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("DB.Example.org")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("alice")
                .setPassword("first")
                .createDBMSConnectionProperties();
        DBMSConnectionProperties url = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("")
                .setPort(5432)
                .setDatabase("")
                .setUser("bob")
                .setPassword("second")
                .setExpertMode(true)
                .setJdbcUrl("jdbc:postgresql://db.example.org/jabref?sslmode=verify-full")
                .createDBMSConnectionProperties();

        assertEquals(SharedDatabaseIdentity.from(fields), SharedDatabaseIdentity.from(url));
    }
}

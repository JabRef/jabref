package org.jabref.logic.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    @Test
    void toleratesMissingHost() {
        DBMSConnectionProperties withoutHost = new DBMSConnectionPropertiesBuilder().setType(DBMSType.POSTGRESQL).setDatabase("jabref").createDBMSConnectionProperties();
        DBMSConnectionProperties withHost = new DBMSConnectionPropertiesBuilder().setType(DBMSType.POSTGRESQL).setHost("localhost").setDatabase("jabref").createDBMSConnectionProperties();

        assertEquals(SharedDatabaseIdentity.from(withoutHost), SharedDatabaseIdentity.from(withoutHost));
        assertNotEquals(SharedDatabaseIdentity.from(withoutHost), SharedDatabaseIdentity.from(withHost));
    }
}

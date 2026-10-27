package org.jabref.logic.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DBMSConnectionPropertiesTest {

    @Test
    void connectionsAreKeptAlive() {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("user")
                .setPassword("password")
                .setUseSSL(false)
                .createDBMSConnectionProperties();

        // Issue #11211: without keepalives, NAT/firewall timeouts silently kill idle connections
        assertEquals("true", properties.asProperties().getProperty("tcpKeepAlive"));
    }

    @Test
    void halfDeadConnectionsFailFast() {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("user")
                .setPassword("password")
                .setUseSSL(false)
                .createDBMSConnectionProperties();

        assertEquals("10", properties.asProperties().getProperty("connectTimeout"));
        // Must stay above the notification listener's 12 s poll interval
        assertEquals("30", properties.asProperties().getProperty("socketTimeout"));
    }

    @Test
    void sslMeansEncryptionWithoutServerAuthentication() {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("user")
                .setPassword("password")
                .setUseSSL(true)
                .createDBMSConnectionProperties();

        // psql/libpq parity: managed providers use private CAs that strict validation rejects
        assertEquals("require", properties.asProperties().getProperty("sslmode"));
    }

    @Test
    void sameDatabaseRegardlessOfEntryMode() {
        DBMSConnectionProperties fields = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL).setHost("Db.Example.org").setPort(5432).setDatabase("jabref").setUser("alice").setPassword("x")
                .createDBMSConnectionProperties();
        DBMSConnectionProperties url = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL).setHost("").setPort(5432).setDatabase("").setUser("alice").setPassword("y").setUseSSL(true)
                .setExpertMode(true).setJdbcUrl("jdbc:postgresql://db.example.org/jabref?sslmode=verify-full")
                .createDBMSConnectionProperties();
        DBMSConnectionProperties otherDatabase = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL).setHost("db.example.org").setPort(5432).setDatabase("other").setUser("alice").setPassword("x")
                .createDBMSConnectionProperties();

        assertTrue(DBMSConnectionProperties.isSameDatabase(fields, url));
        assertFalse(DBMSConnectionProperties.isSameDatabase(fields, otherDatabase));
    }
}

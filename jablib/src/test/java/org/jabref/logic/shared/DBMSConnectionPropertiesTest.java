package org.jabref.logic.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

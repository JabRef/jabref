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
    void sslUsesJabRefsCertificateStore() {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("jabref")
                .setUser("user")
                .setPassword("password")
                .setUseSSL(true)
                .createDBMSConnectionProperties();

        assertEquals("org.postgresql.ssl.DefaultJavaSSLFactory", properties.asProperties().getProperty("sslfactory"));
    }
}

package org.jabref.logic.shared;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DBMSConnectionPropertiesTest {
    @Test
    void asPropertiesIncludesTcpKeepAlive() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("test")
                .setUser("user")
                .setPassword("root")
                .createDBMSConnectionProperties();

        Properties props = connectionProperties.asProperties();

        assertEquals("true", props.getProperty("tcpKeepAlive"),
                "TCP keep-alive should be enabled to prevent idle connection drops");
        assertEquals("30", props.getProperty("socketTimeout"),
                "Socket timeout should be set to avoid hanging when the connection is lost");
    }
}


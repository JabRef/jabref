package org.jabref.logic.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBMSConnectionPropertiesTest {

    @Test
    void urlForMySqlDoesNotIncludeSslConfig() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder().setType(DBMSType.MYSQL).setHost("localhost").setPort(3108).setDatabase("jabref").setUser("user").setPassword("password").setUseSSL(false).setAllowPublicKeyRetrieval(true).setServerTimezone("").createDBMSConnectionProperties();
        assertEquals("jdbc:mariadb://localhost:3108/jabref", connectionProperties.getUrl());
    }

    @Test
    void urlForOracle() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder().setType(DBMSType.ORACLE).setHost("localhost").setPort(3108).setDatabase("jabref").setUser("user").setPassword("password").setUseSSL(false).setServerTimezone("").createDBMSConnectionProperties();
        assertEquals("jdbc:oracle:thin:@localhost:3108/jabref", connectionProperties.getUrl());
    }
}

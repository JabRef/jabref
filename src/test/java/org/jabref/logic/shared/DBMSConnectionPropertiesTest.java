package org.jabref.logic.shared;

import org.jabref.model.database.shared.DBMSType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBMSConnectionPropertiesTest {

    @Test
    void urlForMySqlIncludesSslConfig() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionPropertiesBuilder().setType(DBMSType.MYSQL).setHost("localhost").setPort(3108).setDatabase("jabref").setUser("user").setPassword("password").setUseSSL(false).setAllowPublicKeyRetrieval(true).setServerTimezone("").createDBMSConnectionProperties();
        assertEquals("jdbc:mariadb://localhost:3108/jabref", connectionProperties.getUrl());
    }
}

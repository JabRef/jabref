package org.jabref.logic.shared;

import org.jabref.model.database.shared.DBMSType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBMSConnectionPropertiesTest {

    @Test
    void urlForMySqlIncludesSslConfig() {
        DBMSConnectionProperties connectionProperties = new DBMSConnectionProperties(DBMSType.MYSQL, "localhost", 3108, "jabref", "user", "password", false, "");
        assertEquals("jdbc:mariadb://localhost:3108/jabref?allowPublicKeyRetrieval=true&useSSL=false", connectionProperties.getUrl());
    }
}

package org.jabref.logic.shared;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DBMSConnectionUrlTest {

    @Test
    void parsesProviderUrl() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("postgres://avnadmin:s%3Acret@pg-123.h.aivencloud.com:27372/defaultdb?sslmode=require").orElseThrow();

        assertEquals(new DBMSConnectionUrl(DBMSType.POSTGRESQL, "pg-123.h.aivencloud.com", 27372, "defaultdb",
                Optional.of("avnadmin"), Optional.of("s:cret"), true, "sslmode=require"), url);
        assertEquals("jdbc:postgresql://pg-123.h.aivencloud.com:27372/defaultdb?sslmode=require", url.toJdbcUrl());
    }

    @Test
    void parsesJdbcUrlWithCredentialsInQuery() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse(" jdbc:postgresql://localhost/jabref?user=me&password=a%2Bb&ssl=true ").orElseThrow();

        assertEquals(new DBMSConnectionUrl(DBMSType.POSTGRESQL, "localhost", 5432, "jabref",
                Optional.of("me"), Optional.of("a+b"), true, ""), url);
        assertEquals("jdbc:postgresql://localhost:5432/jabref", url.toJdbcUrl());
    }

    @Test
    void urlWithoutCredentialsLeavesThemEmpty() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("postgresql://db.example.org:5433/lib").orElseThrow();

        assertEquals(Optional.empty(), url.user());
        assertEquals(Optional.empty(), url.password());
        assertEquals(5433, url.port());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "localhost", "mysql://localhost/db", "postgres://", "jdbc:postgresql:db", "postgres://a b/db"})
    void rejectsNonPostgresUrls(String text) {
        assertTrue(DBMSConnectionUrl.parse(text).isEmpty());
    }

    @Test
    void rejectsNull() {
        assertTrue(DBMSConnectionUrl.parse(null).isEmpty());
    }
}

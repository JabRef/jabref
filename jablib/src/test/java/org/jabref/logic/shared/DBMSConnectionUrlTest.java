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
                Optional.of("avnadmin"), Optional.of("s:cret"), true, ""), url);
        assertEquals("jdbc:postgresql://pg-123.h.aivencloud.com:27372/defaultdb", url.toJdbcUrl());
    }

    @Test
    void parsesPsqlCommandLine() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("psql 'postgres://avnadmin:secret@pg-123.h.aivencloud.com:27372/defaultdb?sslmode=require'").orElseThrow();

        assertEquals(new DBMSConnectionUrl(DBMSType.POSTGRESQL, "pg-123.h.aivencloud.com", 27372, "defaultdb",
                Optional.of("avnadmin"), Optional.of("secret"), true, ""), url);
    }

    @Test
    void parsesJdbcUrlWithCredentialsInQuery() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse(" jdbc:postgresql://localhost/jabref?user=me&password=a%2Bb&ssl=true ").orElseThrow();

        assertEquals(new DBMSConnectionUrl(DBMSType.POSTGRESQL, "localhost", 5432, "jabref",
                Optional.of("me"), Optional.of("a+b"), true, ""), url);
        assertEquals("jdbc:postgresql://localhost:5432/jabref", url.toJdbcUrl());
    }

    @Test
    void strictSslModeIsKeptForTheDriver() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("postgres://db.example.org/lib?sslmode=verify-full&sslrootcert=ca.pem").orElseThrow();

        assertTrue(url.useSSL());
        assertEquals("jdbc:postgresql://db.example.org:5432/lib?sslmode=verify-full&sslrootcert=ca.pem", url.toJdbcUrl());
    }

    @Test
    void parsesLibpqKeywordForm() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("host=db.example.org port=6543 dbname=lib user=me password='it\\'s a b' sslmode=require").orElseThrow();

        assertEquals(new DBMSConnectionUrl(DBMSType.POSTGRESQL, "db.example.org", 6543, "lib",
                Optional.of("me"), Optional.of("it's a b"), true, ""), url);
    }

    @Test
    void keywordFormKeepsUnknownParametersForTheDriver() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("host=localhost dbname=lib sslmode=verify-full sslrootcert='/tmp/ca cert.pem'").orElseThrow();

        assertEquals("jdbc:postgresql://localhost:5432/lib?sslmode=verify-full&sslrootcert=%2Ftmp%2Fca%20cert.pem", url.toJdbcUrl());
    }

    @Test
    void unterminatedQuoteWithManyBackslashesIsHandledQuickly() {
        String hostile = "host=localhost password='" + "\\\\&".repeat(5000);
        long start = System.nanoTime();
        DBMSConnectionUrl url = DBMSConnectionUrl.parse(hostile).orElseThrow();
        assertTrue(System.nanoTime() - start < 1_000_000_000L);
        assertEquals("localhost", url.host());
    }

    @Test
    void urlWithoutCredentialsLeavesThemEmpty() {
        DBMSConnectionUrl url = DBMSConnectionUrl.parse("postgresql://db.example.org:5433/lib").orElseThrow();

        assertEquals(Optional.empty(), url.user());
        assertEquals(Optional.empty(), url.password());
        assertEquals(5433, url.port());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "localhost", "mysql://localhost/db", "postgres://", "jdbc:postgresql:db", "user=me dbname=lib"})
    void rejectsNonPostgresUrls(String text) {
        assertTrue(DBMSConnectionUrl.parse(text).isEmpty());
    }

    @Test
    void rejectsNull() {
        assertTrue(DBMSConnectionUrl.parse(null).isEmpty());
    }
}

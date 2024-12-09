package org.jabref.logic.shared;

import java.util.Optional;

import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DatabaseTest
class DBMSTypeTest {

    @Test
    void toStringCorrectForPostgres() {
        assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }

    @Test
    void fromStringWorksForPostgreSQL() {
        assertEquals(Optional.of(DBMSType.POSTGRESQL), DBMSType.fromString("PostgreSQL"));
    }

    @Test
    void fromStringWorksForNullString() {
        assertEquals(Optional.empty(), DBMSType.fromString(null));
    }

    @Test
    void fromStringWorksForEmptyString() {
        assertEquals(Optional.empty(), DBMSType.fromString(""));
    }

    @Test
    void fromStringWorksForUnkownString() {
        assertEquals(Optional.empty(), DBMSType.fromString("unknown"));
    }

    @Test
    void driverClassForPostgresIsCorrect() {
        assertEquals("org.postgresql.Driver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    void fromStringForPostgresReturnsCorrectValue() {
        assertEquals(DBMSType.POSTGRESQL, DBMSType.fromString("PostgreSQL").get());
    }

    @Test
    void fromStringFromInvalidStringReturnsOptionalEmpty() {
        assertFalse(DBMSType.fromString("XXX").isPresent());
    }

    @Test
    void getUrlForPostgresHasCorrectFormat() {
        assertEquals("jdbc:postgresql://localhost:5432/xe", DBMSType.POSTGRESQL.getUrl("localhost", 5432, "xe"));
    }

    @Test
    void getDefaultPortForPostgresHasCorrectValue() {
        assertEquals(5432, DBMSType.POSTGRESQL.getDefaultPort());
    }
}

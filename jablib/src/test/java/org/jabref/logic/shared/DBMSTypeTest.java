package org.jabref.logic.shared;

import java.util.Optional;

import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DatabaseTest
class DBMSTypeTest {

    @Test
    void toStringCorrectForMysql() {
        assertEquals("MySQL", DBMSType.MYSQL.toString());
    }

    @Test
    void toStringCorrectForOracle() {
        assertEquals("Oracle", DBMSType.ORACLE.toString());
    }

    @Test
    void toStringCorrectForPostgres() {
        assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }

    @Test
    void fromStringWorksForMySQL() {
        assertEquals(Optional.of(DBMSType.MYSQL), DBMSType.fromString("MySQL"));
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
    void driverClassForMysqlIsCorrect() {
        assertEquals("org.mariadb.jdbc.Driver", DBMSType.MYSQL.getDriverClassPath());
    }

    @Test
    void driverClassForOracleIsCorrect() {
        assertEquals("oracle.jdbc.driver.OracleDriver", DBMSType.ORACLE.getDriverClassPath());
    }

    @Test
    void driverClassForPostgresIsCorrect() {
        assertEquals("org.postgresql.Driver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    void fromStringForMysqlReturnsCorrectValue() {
        assertEquals(DBMSType.MYSQL, DBMSType.fromString("MySQL").get());
    }

    @Test
    void fromStringForOracleRturnsCorrectValue() {
        assertEquals(DBMSType.ORACLE, DBMSType.fromString("Oracle").get());
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
    void getUrlForMysqlHasCorrectFormat() {
        assertEquals("jdbc:mariadb://localhost:3306/xe", DBMSType.MYSQL.getUrl("localhost", 3306, "xe"));
    }

    @Test
    void getUrlForOracleHasCorrectFormat() {
        assertEquals("jdbc:oracle:thin:@localhost:1521/xe", DBMSType.ORACLE.getUrl("localhost", 1521, "xe"));
    }

    @Test
    void getUrlForPostgresHasCorrectFormat() {
        assertEquals("jdbc:postgresql://localhost:5432/xe", DBMSType.POSTGRESQL.getUrl("localhost", 5432, "xe"));
    }

    @Test
    void getDefaultPortForMysqlHasCorrectValue() {
        assertEquals(3306, DBMSType.MYSQL.getDefaultPort());
    }

    @Test
    void getDefaultPortForOracleHasCorrectValue() {
        assertEquals(1521, DBMSType.ORACLE.getDefaultPort());
    }

    @Test
    void getDefaultPortForPostgresHasCorrectValue() {
        assertEquals(5432, DBMSType.POSTGRESQL.getDefaultPort());
    }
}

package org.jabref.logic.shared;

import java.util.Optional;

import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DatabaseTest
public class DBMSTypeTest {

    @Test
    public void toStringCorrectForMysql() {
        assertEquals("MySQL", DBMSType.MYSQL.toString());
    }

    @Test
    public void toStringCorrectForOracle() {
        assertEquals("Oracle", DBMSType.ORACLE.toString());
    }

    @Test
    public void toStringCorrectForPostgres() {
        assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }

    @Test
    public void fromStringWorksForMySQL() {
        assertEquals(Optional.of(DBMSType.MYSQL), DBMSType.fromString("MySQL"));
    }

    @Test
    public void fromStringWorksForPostgreSQL() {
        assertEquals(Optional.of(DBMSType.POSTGRESQL), DBMSType.fromString("PostgreSQL"));
    }

    @Test
    public void fromStringWorksForNullString() {
        assertEquals(Optional.empty(), DBMSType.fromString(null));
    }

    @Test
    public void fromStringWorksForEmptyString() {
        assertEquals(Optional.empty(), DBMSType.fromString(""));
    }

    @Test
    public void fromStringWorksForUnkownString() {
        assertEquals(Optional.empty(), DBMSType.fromString("unknown"));
    }

    @Test
    public void driverClassForMysqlIsCorrect() {
        assertEquals("org.mariadb.jdbc.Driver", DBMSType.MYSQL.getDriverClassPath());
    }

    @Test
    public void driverClassForOracleIsCorrect() {
        assertEquals("oracle.jdbc.driver.OracleDriver", DBMSType.ORACLE.getDriverClassPath());
    }

    @Test
    public void driverClassForPostgresIsCorrect() {
        assertEquals("org.postgresql.Driver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    public void fromStringForMysqlReturnsCorrectValue() {
        assertEquals(DBMSType.MYSQL, DBMSType.fromString("MySQL").get());
    }

    @Test
    public void fromStringForOracleRturnsCorrectValue() {
        assertEquals(DBMSType.ORACLE, DBMSType.fromString("Oracle").get());
    }

    @Test
    public void fromStringForPostgresReturnsCorrectValue() {
        assertEquals(DBMSType.POSTGRESQL, DBMSType.fromString("PostgreSQL").get());
    }

    @Test
    public void fromStringFromInvalidStringReturnsOptionalEmpty() {
        assertFalse(DBMSType.fromString("XXX").isPresent());
    }

    @Test
    public void getUrlForMysqlHasCorrectFormat() {
        assertEquals("jdbc:mariadb://localhost:3306/xe", DBMSType.MYSQL.getUrl("localhost", 3306, "xe"));
    }

    @Test
    public void getUrlForOracleHasCorrectFormat() {
        assertEquals("jdbc:oracle:thin:@localhost:1521/xe", DBMSType.ORACLE.getUrl("localhost", 1521, "xe"));
    }

    @Test
    public void getUrlForPostgresHasCorrectFormat() {
        assertEquals("jdbc:postgresql://localhost:5432/xe", DBMSType.POSTGRESQL.getUrl("localhost", 5432, "xe"));
    }

    @Test
    public void getDefaultPortForMysqlHasCorrectValue() {
        assertEquals(3306, DBMSType.MYSQL.getDefaultPort());
    }

    @Test
    public void getDefaultPortForOracleHasCorrectValue() {
        assertEquals(1521, DBMSType.ORACLE.getDefaultPort());
    }

    @Test
    public void getDefaultPortForPostgresHasCorrectValue() {
        assertEquals(5432, DBMSType.POSTGRESQL.getDefaultPort());
    }
}

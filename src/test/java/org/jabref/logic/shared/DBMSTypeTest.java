package org.jabref.logic.shared;

import org.jabref.model.database.shared.DBMSType;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DatabaseTest
public class DBMSTypeTest {

    @Test
    public void testToString() {
        assertEquals("MySQL", DBMSType.MYSQL.toString());
        assertEquals("Oracle", DBMSType.ORACLE.toString());
        assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }

    @Test
    public void testGetDriverClassPath() {
        assertEquals("com.mysql.jdbc.Driver", DBMSType.MYSQL.getDriverClassPath());
        assertEquals("oracle.jdbc.driver.OracleDriver", DBMSType.ORACLE.getDriverClassPath());
        assertEquals("com.impossibl.postgres.jdbc.PGDriver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    public void testFromString() {
        assertEquals(DBMSType.MYSQL, DBMSType.fromString("MySQL").get());
        assertEquals(DBMSType.ORACLE, DBMSType.fromString("Oracle").get());
        assertEquals(DBMSType.POSTGRESQL, DBMSType.fromString("PostgreSQL").get());
        assertFalse(DBMSType.fromString("XXX").isPresent());
    }

    @Test
    public void testGetUrl() {
        assertEquals("jdbc:mysql://localhost:3306/xe", DBMSType.MYSQL.getUrl("localhost", 3306, "xe"));
        assertEquals("jdbc:oracle:thin:@localhost:1521:xe", DBMSType.ORACLE.getUrl("localhost", 1521, "xe"));
        assertEquals("jdbc:pgsql://localhost:5432/xe", DBMSType.POSTGRESQL.getUrl("localhost", 5432, "xe"));
    }

    @Test
    public void testGetDefaultPort() {
        assertEquals(3306, DBMSType.MYSQL.getDefaultPort());
        assertEquals(5432, DBMSType.POSTGRESQL.getDefaultPort());
        assertEquals(1521, DBMSType.ORACLE.getDefaultPort());
    }

}

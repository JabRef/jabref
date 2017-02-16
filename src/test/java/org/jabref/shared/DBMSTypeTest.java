package org.jabref.shared;

import org.jabref.testutils.category.DatabaseTests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(DatabaseTests.class)
public class DBMSTypeTest {

    @Test
    public void testToString() {
        Assert.assertEquals("MySQL", DBMSType.MYSQL.toString());
        Assert.assertEquals("Oracle", DBMSType.ORACLE.toString());
        Assert.assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }

    @Test
    public void testGetDriverClassPath() {
        Assert.assertEquals("com.mysql.jdbc.Driver", DBMSType.MYSQL.getDriverClassPath());
        Assert.assertEquals("oracle.jdbc.driver.OracleDriver", DBMSType.ORACLE.getDriverClassPath());
        Assert.assertEquals("com.impossibl.postgres.jdbc.PGDriver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    public void testFromString() {
        Assert.assertEquals(DBMSType.MYSQL, DBMSType.fromString("MySQL").get());
        Assert.assertEquals(DBMSType.ORACLE, DBMSType.fromString("Oracle").get());
        Assert.assertEquals(DBMSType.POSTGRESQL, DBMSType.fromString("PostgreSQL").get());
        Assert.assertFalse(DBMSType.fromString("XXX").isPresent());
    }

    @Test
    public void testGetUrl() {
        Assert.assertEquals("jdbc:mysql://localhost:3306/xe", DBMSType.MYSQL.getUrl("localhost", 3306, "xe"));
        Assert.assertEquals("jdbc:oracle:thin:@localhost:1521:xe", DBMSType.ORACLE.getUrl("localhost", 1521, "xe"));
        Assert.assertEquals("jdbc:pgsql://localhost:5432/xe", DBMSType.POSTGRESQL.getUrl("localhost", 5432, "xe"));
    }

    @Test
    public void testGetDefaultPort() {
        Assert.assertEquals(3306, DBMSType.MYSQL.getDefaultPort());
        Assert.assertEquals(5432, DBMSType.POSTGRESQL.getDefaultPort());
        Assert.assertEquals(1521, DBMSType.ORACLE.getDefaultPort());
    }

}

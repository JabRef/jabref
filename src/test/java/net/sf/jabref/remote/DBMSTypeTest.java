package net.sf.jabref.remote;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("org.postgresql.Driver", DBMSType.POSTGRESQL.getDriverClassPath());
    }

    @Test
    public void testFromString() {
        Assert.assertEquals(DBMSType.MYSQL, DBMSType.fromString("MySQL").get());
        Assert.assertEquals(DBMSType.ORACLE, DBMSType.fromString("Oracle").get());
        Assert.assertEquals(DBMSType.POSTGRESQL, DBMSType.fromString("PostgreSQL").get());
        Assert.assertEquals(Optional.empty(), DBMSType.fromString("XXX"));
    }
}

package net.sf.jabref.remote;

import org.junit.Assert;
import org.junit.Test;

public class DBMSTypeTest {

    @Test
    public void testToString() {
        Assert.assertEquals("MySQL", DBMSType.MYSQL.toString());
        Assert.assertEquals("Oracle", DBMSType.ORACLE.toString());
        Assert.assertEquals("PostgreSQL", DBMSType.POSTGRESQL.toString());
    }
}

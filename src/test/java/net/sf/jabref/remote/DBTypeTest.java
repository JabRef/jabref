package net.sf.jabref.remote;

import org.junit.Assert;
import org.junit.Test;

public class DBTypeTest {

    @Test
    public void testToString() {
        Assert.assertEquals("MySQL", DBType.MYSQL.toString());
        Assert.assertEquals("Oracle", DBType.ORACLE.toString());
        Assert.assertEquals("PostgreSQL", DBType.POSTGRESQL.toString());
    }
}

package net.sf.jabref.util;

import org.junit.Assert;
import org.junit.Test;

public class DoiTest {
    @Test
    public void plainDoi() {
        Assert.assertEquals("10.1006/jmbi.1998.2354", new Doi("10.1006/jmbi.1998.2354").getDoi());
        Assert.assertEquals("10.231/JIM.0b013e31820bab4c", new Doi("10.231/JIM.0b013e31820bab4c").getDoi());
    }

    @Test
    public void prefixedDoi() {
        // Doi prefix
        Assert.assertEquals("10.1006/jmbi.1998.2354", new Doi("doi:10.1006/jmbi.1998.2354").getDoi());
        // TODO: URN prefix
        //Assert.assertEquals("10.1006/jmbi.1998.2354", new Doi("http://doi.org/urn:doi:10.123:456").getDoi());
    }

    @Test
    public void uriDoi() {
        // URI Doi
        Assert.assertEquals("10.1006/jmbi.1998.2354", new Doi("http://doi.org/10.1006/jmbi.1998.2354").getDoi());
        Assert.assertEquals("10.1145/1294928.1294933", new Doi("http://doi.acm.org/10.1145/1294928.1294933").getDoi());
        Assert.assertEquals("10.1145/1294928.1294933", new Doi("http://doi.acm.net/10.1145/1294928.1294933").getDoi());
        Assert.assertEquals("10.1145/1294928.1294933", new Doi("http://doi.acm.com/10.1145/1294928.1294933").getDoi());
        Assert.assertEquals("10.1145/1294928.1294933", new Doi("http://doi.acm.de/10.1145/1294928.1294933").getDoi());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new Doi("http://dx.doi.org/10.1007/978-3-642-15618-2_19").getDoi());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new Doi("http://dx.doi.net/10.1007/978-3-642-15618-2_19").getDoi());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new Doi("http://dx.doi.com/10.1007/978-3-642-15618-2_19").getDoi());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new Doi("http://dx.doi.de/10.1007/978-3-642-15618-2_19").getDoi());
        Assert.assertEquals("10.1000/182", new Doi("http://doi.org/10.1000/182").getDoi());
        Assert.assertEquals("10.4108/ICST.COLLABORATECOM2009.8275", new Doi("http://dx.doi.org/10.4108/ICST.COLLABORATECOM2009.8275").getDoi());
        Assert.assertEquals("10.1109/MIC.2012.43", new Doi("http://doi.ieeecomputersociety.org/10.1109/MIC.2012.43").getDoi());
        // TODO: Encoded URI Doi
        //Assert.assertEquals("10.1006/rwei.1999\".0001", new Doi("http://doi.org/10.1006/rwei.1999%22.0001").getDoi());
        //Assert.assertEquals("http://doi.org/10.1000/456#789", new Doi("http://doi.org/10.1000/456%23789").getDoi());
    }

    @Test
    public void detectHttpDoi() {
        Assert.assertTrue(Doi.containsHttpDoi("http://doi.acm.org/10.1145/1294928.1294933"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.doi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.doi.org/10.1000/182"));

        Assert.assertFalse(Doi.containsHttpDoi("http://www.xyz.com"));
        Assert.assertFalse(Doi.containsHttpDoi("http://dx.doing.org/fjdlfdsjfdlfdj.htm"));
        Assert.assertFalse(Doi.containsHttpDoi("thfjtfjglkjjlkk�lm5476576658796"));

        Assert.assertTrue(Doi.containsHttpDoi("http://doii.acm.org/10.1145/1294928.1294933"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.ddoi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.eoi.org/10.1000/182"));
    }

    @Test
    public void detectUrnDoi() {
        Assert.assertTrue(Doi.containsHttpDoi("http://doi.acm.org/10.1145/1294928.1294933"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.doi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.doi.org/10.1000/182"));

        Assert.assertFalse(Doi.containsHttpDoi("http://www.xyz.com"));
        Assert.assertFalse(Doi.containsHttpDoi("http://dx.doing.org/fjdlfdsjfdlfdj.htm"));
        Assert.assertFalse(Doi.containsHttpDoi("thfjtfjglkjjlkk�lm5476576658796"));

        Assert.assertTrue(Doi.containsHttpDoi("http://doii.acm.org/10.1145/1294928.1294933"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.ddoi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertTrue(Doi.containsHttpDoi("http://dx.eoi.org/10.1000/182"));
    }
}
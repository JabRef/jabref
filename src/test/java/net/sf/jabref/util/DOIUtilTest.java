package net.sf.jabref.util;

import net.sf.jabref.util.DOIUtil;
import org.junit.Assert;
import org.junit.Test;

public class DOIUtilTest {

    /**
     * Tests Util.checkForDOI(...) for right functionality
     */
    @Test
    public void testCheckForDoi() {
        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://doi.acm.org/10.1145/1294928.1294933"));
        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://dx.doi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://dx.doi.org/10.1000/182"));

        Assert.assertEquals(false, DOIUtil.checkForDOIwithHTTPprefix("http://www.xyz.com"));
        Assert.assertEquals(false, DOIUtil.checkForDOIwithHTTPprefix("http://dx.doing.org/fjdlfdsjfdlfdj.htm"));
        Assert.assertEquals(false, DOIUtil.checkForDOIwithHTTPprefix("thfjtfjglkjjlkk�lm5476576658796"));

        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://doii.acm.org/10.1145/1294928.1294933"));
        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://dx.ddoi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertEquals(true, DOIUtil.checkForDOIwithHTTPprefix("http://dx.eoi.org/10.1000/182"));
    }

    /**
     * Tests Util.parseDOI(...) for right functionality
     */
    @Test
    public void testParseDoi() {
        Assert.assertEquals("10.1145/1294928.1294933", DOIUtil.getDOI("http://doi.acm.org/10.1145/1294928.1294933"));
        Assert.assertEquals("10.1145/1294928.1294933", DOIUtil.getDOI("http://doi.acm.net/10.1145/1294928.1294933"));
        Assert.assertEquals("10.1145/1294928.1294933", DOIUtil.getDOI("http://doi.acm.com/10.1145/1294928.1294933"));
        Assert.assertEquals("10.1145/1294928.1294933", DOIUtil.getDOI("http://doi.acm.de/10.1145/1294928.1294933"));

        Assert.assertEquals("10.1007/978-3-642-15618-2_19", DOIUtil.getDOI("http://dx.doi.org/10.1007/978-3-642-15618-2_19"));
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", DOIUtil.getDOI("http://dx.doi.net/10.1007/978-3-642-15618-2_19"));
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", DOIUtil.getDOI("http://dx.doi.com/10.1007/978-3-642-15618-2_19"));
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", DOIUtil.getDOI("http://dx.doi.de/10.1007/978-3-642-15618-2_19"));

        Assert.assertEquals("10.1000/182", DOIUtil.getDOI("http://dx.doi.org/10.1000/182"));

        Assert.assertEquals("10.4108/ICST.COLLABORATECOM2009.8275", DOIUtil.getDOI("http://dx.doi.org/10.4108/ICST.COLLABORATECOM2009.8275"));
        Assert.assertEquals("10.1109/MIC.2012.43", DOIUtil.getDOI("http://doi.ieeecomputersociety.org/10.1109/MIC.2012.43"));
    }

}
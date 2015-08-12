/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.util;

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
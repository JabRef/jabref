package net.sf.jabref.logic.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;


public class DOITest {
    @Test
    public void acceptPlainDoi() {
        Assert.assertEquals("10.1006/jmbi.1998.2354", new DOI("10.1006/jmbi.1998.2354").getDOI());
        Assert.assertEquals("10.231/JIM.0b013e31820bab4c", new DOI("10.231/JIM.0b013e31820bab4c").getDOI());
        Assert.assertEquals("10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S", new DOI("10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S").getDOI());
        Assert.assertEquals("10.1126/sciadv.1500214", new DOI("10.1126/sciadv.1500214").getDOI());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespaces() {
        Assert.assertEquals("10.1006/jmbi.1998.2354", new DOI("  10.1006/jmbi.1998.2354 ").getDOI());
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectEmbeddedDoi() {
        new DOI("other stuff 10.1006/jmbi.1998.2354 end");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectInvalidDirectoryIndicator() {
        // wrong directory indicator
        new DOI("12.1006/jmbi.1998.2354 end");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidDoiUri() {
        new DOI("https://thisisnouri");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectMissingDivider() {
        // missing divider
        new DOI("10.1006jmbi.1998.2354 end");
    }

    @Test
    public void acceptDoiPrefix() {
        // Doi prefix
        Assert.assertEquals("10.1006/jmbi.1998.2354", new DOI("doi:10.1006/jmbi.1998.2354").getDOI());
    }

    @Test
    public void acceptURNPrefix() {
        Assert.assertEquals("10.123/456", new DOI("urn:10.123/456").getDOI());
        Assert.assertEquals("10.123/456", new DOI("urn:doi:10.123/456").getDOI());
        Assert.assertEquals("10.123/456", new DOI("http://doi.org/urn:doi:10.123/456").getDOI());
        // : is also allowed as divider, will be replaced by RESOLVER
        Assert.assertEquals("10.123:456ABC/zyz", new DOI("http://doi.org/urn:doi:10.123:456ABC%2Fzyz").getDOI());
    }

    @Test
    public void acceptURLDoi() {
        // http
        Assert.assertEquals("10.1006/jmbi.1998.2354", new DOI("http://doi.org/10.1006/jmbi.1998.2354").getDOI());
        // https
        Assert.assertEquals("10.1006/jmbi.1998.2354", new DOI("https://doi.org/10.1006/jmbi.1998.2354").getDOI());
        // other domains
        Assert.assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.org/10.1145/1294928.1294933").getDOI());
        Assert.assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.net/10.1145/1294928.1294933").getDOI());
        Assert.assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.com/10.1145/1294928.1294933").getDOI());
        Assert.assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.de/10.1145/1294928.1294933").getDOI());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new DOI("http://dx.doi.org/10.1007/978-3-642-15618-2_19").getDOI());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new DOI("http://dx.doi.net/10.1007/978-3-642-15618-2_19").getDOI());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new DOI("http://dx.doi.com/10.1007/978-3-642-15618-2_19").getDOI());
        Assert.assertEquals("10.1007/978-3-642-15618-2_19", new DOI("http://dx.doi.de/10.1007/978-3-642-15618-2_19").getDOI());
        Assert.assertEquals("10.4108/ICST.COLLABORATECOM2009.8275", new DOI("http://dx.doi.org/10.4108/ICST.COLLABORATECOM2009.8275").getDOI());
        Assert.assertEquals("10.1109/MIC.2012.43", new DOI("http://doi.ieeecomputersociety.org/10.1109/MIC.2012.43").getDOI());
    }

    @Test
    public void correctlyDecodeHttpDOIs() {
        // See http://www.doi.org/doi_handbook/2_Numbering.html#2.5.2.4
        // % -> (%25)
        Assert.assertEquals("10.1006/rwei.1999%.0001", new DOI("http://doi.org/10.1006/rwei.1999%25.0001").getDOI());
        // " -> (%22)
        Assert.assertEquals("10.1006/rwei.1999\".0001", new DOI("http://doi.org/10.1006/rwei.1999%22.0001").getDOI());
        // # -> (%23)
        Assert.assertEquals("10.1006/rwei.1999#.0001", new DOI("http://doi.org/10.1006/rwei.1999%23.0001").getDOI());
        // SPACE -> (%20)
        Assert.assertEquals("10.1006/rwei.1999 .0001", new DOI("http://doi.org/10.1006/rwei.1999%20.0001").getDOI());
        // ? -> (%3F)
        Assert.assertEquals("10.1006/rwei.1999?.0001", new DOI("http://doi.org/10.1006/rwei.1999%3F.0001").getDOI());
    }

    @Test
    public void correctlyEncodeDOIs() {
        // See http://www.doi.org/doi_handbook/2_Numbering.html#2.5.2.4
        // % -> (%25)
        Assert.assertEquals("http://doi.org/10.1006/rwei.1999%25.0001", new DOI("http://doi.org/10.1006/rwei.1999%25.0001").getURLAsASCIIString());
        // " -> (%22)
        Assert.assertEquals("http://doi.org/10.1006/rwei.1999%22.0001", new DOI("http://doi.org/10.1006/rwei.1999%22.0001").getURLAsASCIIString());
        // # -> (%23)
        Assert.assertEquals("http://doi.org/10.1006/rwei.1999%23.0001", new DOI("http://doi.org/10.1006/rwei.1999%23.0001").getURLAsASCIIString());
        // SPACE -> (%20)
        Assert.assertEquals("http://doi.org/10.1006/rwei.1999%20.0001", new DOI("http://doi.org/10.1006/rwei.1999%20.0001").getURLAsASCIIString());
        // ? -> (%3F)
        Assert.assertEquals("http://doi.org/10.1006/rwei.1999%3F.0001", new DOI("http://doi.org/10.1006/rwei.1999%3F.0001").getURLAsASCIIString());
    }

    @Test
    public void constructCorrectURLForDoi() {
        // add / to RESOLVER url if missing
        Assert.assertEquals("http://doi.org/10.1006/jmbi.1998.2354", new DOI("10.1006/jmbi.1998.2354").getURLAsASCIIString());
        Assert.assertEquals("http://doi.org/10.1006/jmbi.1998.2354", new DOI("http://doi.org/10.1006/jmbi.1998.2354").getURLAsASCIIString());
        Assert.assertEquals("http://doi.org/10.1109/VLHCC.2004.20", new DOI("doi:10.1109/VLHCC.2004.20").getURLAsASCIIString());
    }

    @Test
    public void findDoiInsideArbitraryText() {
        Assert.assertEquals("10.1006/jmbi.1998.2354", DOI.findInText("other stuff 10.1006/jmbi.1998.2354 end").get().getDOI());
    }

    @Test
    public void noDOIFoundInsideArbitraryText() {
        Assert.assertEquals(Optional.empty(), DOI.findInText("text without 28282 a doi"));
    }
}

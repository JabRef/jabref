package org.jabref.model.entry.identifier;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DOITest {

    @Test
    public void acceptPlainDoi() {
        assertEquals("10.1006/jmbi.1998.2354", new DOI("10.1006/jmbi.1998.2354").getDOI());
        assertEquals("10.231/JIM.0b013e31820bab4c", new DOI("10.231/JIM.0b013e31820bab4c").getDOI());
        assertEquals("10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S",
                new DOI("10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S").getDOI());
        assertEquals("10.1126/sciadv.1500214", new DOI("10.1126/sciadv.1500214").getDOI());
    }

    @Test
    public void acceptPlainShortDoi() {
        assertEquals("10/gf4gqc", new DOI("10/gf4gqc").getDOI());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespaces() {
        assertEquals("10.1006/jmbi.1998.2354", new DOI("  10.1006/jmbi.1998.2354 ").getDOI());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespacesInShortDoi() {
        assertEquals("10/gf4gqc", new DOI("   10/gf4gqc ").getDOI());
    }

    @Test
    public void rejectEmbeddedDoi() {
        assertThrows(IllegalArgumentException.class, () -> new DOI("other stuff 10.1006/jmbi.1998.2354 end"));
    }

    @Test
    public void rejectEmbeddedShortDoi() {
        assertThrows(IllegalArgumentException.class, () -> new DOI("other stuff 10/gf4gqc end"));
    }

    @Test
    public void rejectInvalidDirectoryIndicator() {
        // wrong directory indicator
        assertThrows(IllegalArgumentException.class, () -> new DOI("12.1006/jmbi.1998.2354 end"));
    }

    @Test
    public void rejectInvalidDirectoryIndicatorInShortDoi() {
        assertThrows(IllegalArgumentException.class, () -> new DOI("20/abcd"));
    }

    @Test
    public void rejectInvalidDoiUri() {
        assertThrows(IllegalArgumentException.class, () -> new DOI("https://thisisnouri"));
    }

    @Test
    public void rejectMissingDivider() {
        // missing divider
        assertThrows(IllegalArgumentException.class, () -> new DOI("10.1006jmbi.1998.2354 end"));
    }

    @Test
    public void rejectMissingDividerInShortDoi() {
        assertThrows(IllegalArgumentException.class, () -> new DOI("10gf4gqc end"));
    }

    @Test
    public void acceptDoiPrefix() {
        // Doi prefix
        assertEquals("10.1006/jmbi.1998.2354", new DOI("doi:10.1006/jmbi.1998.2354").getDOI());
    }

    @Test
    public void acceptDoiPrefixInShortDoi() {
        assertEquals("10/gf4gqc", new DOI("doi:10/gf4gqc").getDOI());
    }

    @Test
    public void acceptURNPrefix() {
        assertEquals("10.123/456", new DOI("urn:10.123/456").getDOI());
        assertEquals("10.123/456", new DOI("urn:doi:10.123/456").getDOI());
        assertEquals("10.123/456", new DOI("http://doi.org/urn:doi:10.123/456").getDOI());
        // : is also allowed as divider, will be replaced by RESOLVER
        assertEquals("10.123:456ABC/zyz", new DOI("http://doi.org/urn:doi:10.123:456ABC%2Fzyz").getDOI());
    }

    @Test
    public void acceptURNPrefixInShortDoi() {
        assertEquals("10/gf4gqc", new DOI("urn:10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("urn:doi:10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://doi.org/urn:doi:10/gf4gqc").getDOI());
        // : is also allowed as divider, will be replaced by RESOLVER
        assertEquals("10:gf4gqc", new DOI("http://doi.org/urn:doi:10:gf4gqc").getDOI());
    }

    @Test
    public void acceptURLDoi() {
        // http
        assertEquals("10.1006/jmbi.1998.2354", new DOI("http://doi.org/10.1006/jmbi.1998.2354").getDOI());
        // https
        assertEquals("10.1006/jmbi.1998.2354", new DOI("https://doi.org/10.1006/jmbi.1998.2354").getDOI());
        // https with % divider
        assertEquals("10.2307/1990888", new DOI("https://dx.doi.org/10.2307%2F1990888").getDOI());
        // other domains
        assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.org/10.1145/1294928.1294933").getDOI());
        assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.net/10.1145/1294928.1294933").getDOI());
        assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.com/10.1145/1294928.1294933").getDOI());
        assertEquals("10.1145/1294928.1294933", new DOI("http://doi.acm.de/10.1145/1294928.1294933").getDOI());
        assertEquals("10.1007/978-3-642-15618-2_19",
                new DOI("http://dx.doi.org/10.1007/978-3-642-15618-2_19").getDOI());
        assertEquals("10.1007/978-3-642-15618-2_19",
                new DOI("http://dx.doi.net/10.1007/978-3-642-15618-2_19").getDOI());
        assertEquals("10.1007/978-3-642-15618-2_19",
                new DOI("http://dx.doi.com/10.1007/978-3-642-15618-2_19").getDOI());
        assertEquals("10.1007/978-3-642-15618-2_19",
                new DOI("http://dx.doi.de/10.1007/978-3-642-15618-2_19").getDOI());
        assertEquals("10.4108/ICST.COLLABORATECOM2009.8275",
                new DOI("http://dx.doi.org/10.4108/ICST.COLLABORATECOM2009.8275").getDOI());
        assertEquals("10.1109/MIC.2012.43",
                new DOI("http://doi.ieeecomputersociety.org/10.1109/MIC.2012.43").getDOI());
    }

    @Test
    public void acceptURLShortDoi() {
        // http
        assertEquals("10/gf4gqc", new DOI("http://doi.org/10/gf4gqc").getDOI());
        // https
        assertEquals("10/gf4gqc", new DOI("https://doi.org/10/gf4gqc").getDOI());
        // https with % divider
        assertEquals("10/gf4gqc", new DOI("https://dx.doi.org/10%2Fgf4gqc").getDOI());
        // other domains
        assertEquals("10/gf4gqc", new DOI("http://doi.acm.org/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://doi.acm.net/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://doi.acm.com/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://doi.acm.de/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://dx.doi.org/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://dx.doi.net/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://dx.doi.com/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://dx.doi.de/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://dx.doi.org/10/gf4gqc").getDOI());
        assertEquals("10/gf4gqc", new DOI("http://doi.ieeecomputersociety.org/10/gf4gqc").getDOI());
    }

    @Test
    public void correctlyDecodeHttpDOIs() {
        // See http://www.doi.org/doi_handbook/2_Numbering.html#2.5.2.4
        // % -> (%25)
        assertEquals("10.1006/rwei.1999%.0001", new DOI("http://doi.org/10.1006/rwei.1999%25.0001").getDOI());
        // " -> (%22)
        assertEquals("10.1006/rwei.1999\".0001", new DOI("http://doi.org/10.1006/rwei.1999%22.0001").getDOI());
        // # -> (%23)
        assertEquals("10.1006/rwei.1999#.0001", new DOI("http://doi.org/10.1006/rwei.1999%23.0001").getDOI());
        // SPACE -> (%20)
        assertEquals("10.1006/rwei.1999 .0001", new DOI("http://doi.org/10.1006/rwei.1999%20.0001").getDOI());
        // ? -> (%3F)
        assertEquals("10.1006/rwei.1999?.0001", new DOI("http://doi.org/10.1006/rwei.1999%3F.0001").getDOI());
    }

    @Test
    public void correctlyEncodeDOIs() {
        // See http://www.doi.org/doi_handbook/2_Numbering.html#2.5.2.4
        // % -> (%25)
        assertEquals("https://doi.org/10.1006/rwei.1999%25.0001",
                new DOI("https://doi.org/10.1006/rwei.1999%25.0001").getURIAsASCIIString());
        // " -> (%22)
        assertEquals("https://doi.org/10.1006/rwei.1999%22.0001",
                new DOI("https://doi.org/10.1006/rwei.1999%22.0001").getURIAsASCIIString());
        // # -> (%23)
        assertEquals("https://doi.org/10.1006/rwei.1999%23.0001",
                new DOI("https://doi.org/10.1006/rwei.1999%23.0001").getURIAsASCIIString());
        // SPACE -> (%20)
        assertEquals("https://doi.org/10.1006/rwei.1999%20.0001",
                new DOI("https://doi.org/10.1006/rwei.1999%20.0001").getURIAsASCIIString());
        // ? -> (%3F)
        assertEquals("https://doi.org/10.1006/rwei.1999%3F.0001",
                new DOI("https://doi.org/10.1006/rwei.1999%3F.0001").getURIAsASCIIString());
    }

    @Test
    public void constructCorrectURLForDoi() {
        // add / to RESOLVER url if missing
        assertEquals("https://doi.org/10.1006/jmbi.1998.2354",
                new DOI("10.1006/jmbi.1998.2354").getURIAsASCIIString());
        assertEquals("https://doi.org/10.1006/jmbi.1998.2354",
                new DOI("https://doi.org/10.1006/jmbi.1998.2354").getURIAsASCIIString());
        assertEquals("https://doi.org/10.1109/VLHCC.2004.20",
                new DOI("doi:10.1109/VLHCC.2004.20").getURIAsASCIIString());
    }

    @Test
    public void constructCorrectURLForShortDoi() {
        assertEquals("https://doi.org/10/gf4gqc", new DOI("10/gf4gqc").getURIAsASCIIString());
    }

    @Test
    public void findDoiInsideArbitraryText() {
        assertEquals("10.1006/jmbi.1998.2354",
                DOI.findInText("other stuff 10.1006/jmbi.1998.2354 end").get().getDOI());
    }

    @Test
    public void findShortDoiInsideArbitraryText() {
        assertEquals("10/gf4gqc", DOI.findInText("other stuff 10/gf4gqc end").get().getDOI());
    }

    @Test
    public void noDOIFoundInsideArbitraryText() {
        assertEquals(Optional.empty(), DOI.findInText("text without 28282 a doi"));
    }

    @Test
    public void parseDOIWithWhiteSpace() {
        String doiWithSpace = "https : / / doi.org / 10 .1109 /V LHCC.20 04.20";
        assertEquals("https://doi.org/10.1109/VLHCC.2004.20", DOI.parse(doiWithSpace).get().getURIAsASCIIString());
    }

    @Test
    public void parseShortDOIWithWhiteSpace() {
        String shortDoiWithSpace = "https : / / doi.org / 10 / gf4gqc";
        assertEquals("https://doi.org/10/gf4gqc", DOI.parse(shortDoiWithSpace).get().getURIAsASCIIString());
    }

    @Test
    public void isShortDoiShouldReturnTrueWhenItIsShortDoi() {
        assertTrue(new DOI("10/abcde").isShortDoi());
    }

    @Test
    public void isShortDoiShouldReturnFalseWhenItIsDoi() {
        assertFalse(new DOI("10.1006/jmbi.1998.2354").isShortDoi());
    }

    @Test
    public void equalsWorksFor2017Doi() {
        assertTrue(new DOI("10.1109/cloud.2017.89").equals(new DOI("10.1109/CLOUD.2017.89")));
    }
}

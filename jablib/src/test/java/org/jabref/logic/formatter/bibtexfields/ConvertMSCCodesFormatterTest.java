package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvertMSCCodesFormatterTest {

    private ConvertMSCCodesFormatter formatter;

    @BeforeEach
    void setup() {
        formatter = new ConvertMSCCodesFormatter();
    }

    /* Reference:
    "34B60": "Applications of boundary value problems involving ordinary differential equations",
    "53C70": "Direct methods ( G-spaces of Busemann etc.)",
    "53C75": "Geometric orders order geometry [See also 51Lxx]",
    "53C80": "Applications of global differential geometry to the sciences",
    "53C99": "53C99 None of the above but in this section",
    "76Z10": "Biopropulsion in water and in air",
    "90B40": "Search theory",
    */

    @Test
    void convertSingleMSCCode() {
        assertEquals("Applications of boundary value problems involving ordinary differential equations",
                formatter.format("34B60"));
    }

    @Test
    void convertMultipleMSCCodes() {
        assertEquals(
                "53C99 None of the above but in this section,Biopropulsion in water and in air,Search theory",
                formatter.format("53C99,76Z10,90B40"));
    }

    @Test
    void preserveNonMSCKeyword() {
        assertEquals(
                "Jabref123,Search theory,Hello Jabref,Direct methods ( G-spaces of Busemann etc.)",
                formatter.format("Jabref123,90B40,Hello Jabref,53C70"));
    }

    @Test
    void noCodesPresentInKeywordsField() {
        assertEquals(
                "Jabref Here,Jabref There,Jabref Everywhere",
                formatter.format("Jabref Here,Jabref There,Jabref Everywhere"));
    }
}

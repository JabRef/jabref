package org.jabref.logic.formatter.bibtexfields;

import java.util.List;

import org.jabref.logic.msc.MscCodeEntry;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.entry.BibEntryPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConvertMSCCodesFormatterTest {

    private JabRefCliPreferences cliPreferences;
    private ConvertMSCCodesFormatter formatter;

    @BeforeEach
    void setup() {
        cliPreferences = mock(JabRefCliPreferences.class);
        when(cliPreferences.shouldEnableMscKeywordDescriptions()).thenReturn(true);
        when(cliPreferences.getBibEntryPreferences()).thenReturn(BibEntryPreferences.getDefault());
        formatter = new ConvertMSCCodesFormatter(cliPreferences);

        MscCodeRepository repository = new MscCodeRepository(List.of(
                new MscCodeEntry("34B60", "Applications of boundary value problems involving ordinary differential equations", "Applications of boundary value problems involving ordinary differential equations"),
                new MscCodeEntry("53C70", "Direct methods (\\(G\\)-spaces of Busemann, etc.)", "Direct methods (\\(G\\)-spaces of Busemann, etc.)"),
                new MscCodeEntry("53C99", "Global differential geometry", "None of the above, but in this section"),
                new MscCodeEntry("76Z10", "Biopropulsion in water and in air", "Biopropulsion in water and in air"),
                new MscCodeEntry("90B40", "Search theory", "Search theory")
        ));
        ConvertMSCCodesFormatter.setMscCodes(repository);
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
                "53C99 None of the above, but in this section,Biopropulsion in water and in air,Search theory",
                formatter.format("53C99,76Z10,90B40"));
    }

    @Test
    void preserveNonMSCKeywordAndConvertOthers() {
        assertEquals(
                "Jabref123,Search theory,Hello Jabref,Direct methods (\\\\(G\\\\)-spaces of Busemann\\, etc.)",
                formatter.format("Jabref123,90B40,Hello Jabref,53C70"));
    }

    @Test
    void noCodesPresentInKeywordsField() {
        assertEquals(
                "Jabref Here,Jabref There,Jabref Everywhere",
                formatter.format("Jabref Here,Jabref There,Jabref Everywhere"));
    }

    @Test
    void disabledFeatureLeavesKeywordsUntouched() {
        when(cliPreferences.shouldEnableMscKeywordDescriptions()).thenReturn(false);

        assertEquals("34B60,90B40", formatter.format("34B60,90B40"));
    }
}

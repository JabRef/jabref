package org.jabref.logic.journals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbbreviationsTest {

    private JournalAbbreviationPreferences prefs;
    private JournalAbbreviationLoader abbreviations;

    @BeforeEach
    public void setUp() throws Exception {
        prefs = mock(JournalAbbreviationPreferences.class);
        abbreviations = new JournalAbbreviationLoader();
    }

    @Test
    public void getNextAbbreviationAbbreviatesIEEEJournalTitle() {
        when(prefs.useIEEEAbbreviations()).thenReturn(true);

        assertEquals("#IEEE_J_PROC#",
                abbreviations.getRepository(prefs)
                        .getNextAbbreviation("Proceedings of the IEEE").get());
    }

    @Test
    public void getNextAbbreviationExpandsIEEEAbbreviation() {
        when(prefs.useIEEEAbbreviations()).thenReturn(true);

        assertEquals("Proceedings of the IEEE",
                abbreviations.getRepository(prefs)
                        .getNextAbbreviation("#IEEE_J_PROC#").get());
    }

    @Test
    public void getNextAbbreviationAbbreviatesJournalTitle() {
        assertEquals("Proc. IEEE",
                abbreviations.getRepository(prefs)
                        .getNextAbbreviation("Proceedings of the IEEE").get());
    }

    @Test
    public void getNextAbbreviationRemovesPoint() {
        assertEquals("Proc IEEE",
                abbreviations.getRepository(prefs)
                        .getNextAbbreviation("Proc. IEEE").get());
    }

    @Test
    public void getNextAbbreviationExpandsAbbreviation() {
        assertEquals("Proceedings of the IEEE",
                abbreviations.getRepository(prefs)
                        .getNextAbbreviation("Proc IEEE").get());
    }
}

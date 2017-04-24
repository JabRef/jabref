package org.jabref.logic.journals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbbreviationsTest {

    @Mock
    private JournalAbbreviationPreferences prefs;
    private JournalAbbreviationLoader abbreviations;

    @Before
    public void setUp() throws Exception {
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

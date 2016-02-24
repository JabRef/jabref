package net.sf.jabref.logic.journals;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import net.sf.jabref.JabRefPreferences;

public class AbbreviationsTest {

    @Test
    public void getNextAbbreviationAbbreviatesIEEEJournalTitle() {
        JabRefPreferences prefs = mock(JabRefPreferences.class);
        when(prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)).thenReturn(true);

        JournalAbbreviationLoader abbreviations = new JournalAbbreviationLoader(prefs);
        assertEquals("#IEEE_J_PROC#",
                abbreviations.getRepository().getNextAbbreviation("Proceedings of the IEEE").get());
    }

    @Test
    public void getNextAbbreviationExpandsIEEEAbbreviation() {
        JabRefPreferences prefs = mock(JabRefPreferences.class);
        when(prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)).thenReturn(true);

        JournalAbbreviationLoader abbreviations = new JournalAbbreviationLoader(prefs);
        assertEquals("Proceedings of the IEEE",
                abbreviations.getRepository().getNextAbbreviation("#IEEE_J_PROC#").get());
    }

    @Test
    public void getNextAbbreviationAbbreviatesJournalTitle() {
        JabRefPreferences prefs = mock(JabRefPreferences.class);
        JournalAbbreviationLoader abbreviations = new JournalAbbreviationLoader(prefs);
        assertEquals("Proc. IEEE",
                abbreviations.getRepository().getNextAbbreviation("Proceedings of the IEEE").get());
    }

    @Test
    public void getNextAbbreviationRemovesPoint() {
        JabRefPreferences prefs = mock(JabRefPreferences.class);
        JournalAbbreviationLoader abbreviations = new JournalAbbreviationLoader(prefs);
        assertEquals("Proc IEEE", abbreviations.getRepository().getNextAbbreviation("Proc. IEEE").get());
    }

    @Test
    public void getNextAbbreviationExpandsAbbreviation() {
        JabRefPreferences prefs = mock(JabRefPreferences.class);
        JournalAbbreviationLoader abbreviations = new JournalAbbreviationLoader(prefs);
        assertEquals("Proceedings of the IEEE",
                abbreviations.getRepository().getNextAbbreviation("Proc IEEE").get());
    }

}

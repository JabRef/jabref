package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class INSPIREFetcherTexkeysTest {

    @Test
    void setTexkeys_appliesFirstKeyAndClearsField() {
        ImportFormatPreferences prefs = org.mockito.Mockito.mock(ImportFormatPreferences.class);
        INSPIREFetcher fetcher = new INSPIREFetcher(prefs);

        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("texkeys"), "Smith2020,AnotherKey,ThirdKey");

        fetcher.setTexkeys(entry);

        // citation key should be Smith2020
        assertTrue(entry.getCitationKey().isPresent(), "Citation key should be present");
        assertEquals("Smith2020", entry.getCitationKey().get());

        // texkeys field should be cleared
        assertTrue(entry.getField(new UnknownField("texkeys")).isEmpty(),
                "texkeys field should be cleared after processing");
    }

    @Test
    void setTexkeys_handlesEmptyOrMissingFieldGracefully() {
        ImportFormatPreferences prefs = org.mockito.Mockito.mock(ImportFormatPreferences.class);
        INSPIREFetcher fetcher = new INSPIREFetcher(prefs);

        BibEntry entry = new BibEntry(); // no texkeys field
        fetcher.setTexkeys(entry);

        assertTrue(entry.getCitationKey().isEmpty(), "No citation key should be set");
        assertTrue(entry.getField(new UnknownField("texkeys")).isEmpty(),
                "texkeys field should remain empty");
    }

    @Test
    void setTexkeys_ignoresBlankValues() {
        ImportFormatPreferences prefs = org.mockito.Mockito.mock(ImportFormatPreferences.class);
        INSPIREFetcher fetcher = new INSPIREFetcher(prefs);

        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("texkeys"), ", , RealKey");

        fetcher.setTexkeys(entry);

        assertTrue(entry.getCitationKey().isPresent(), "Citation key should be set");
        assertEquals("RealKey", entry.getCitationKey().get(), "The first non-empty texkey should be used");
        assertTrue(entry.getField(new UnknownField("texkeys")).isEmpty(),
                "texkeys field should be cleared after applying citation key");
    }
}

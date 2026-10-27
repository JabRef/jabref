package org.jabref.logic.importer.fileformat.pdf;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntryPreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CitationsFromPdfTest {

    @Test
    void withOverriddenGrobidUrlKeepsOtherPreferencesAndEnablesGrobid() {
        ImportFormatPreferences original = new ImportFormatPreferences(
                BibEntryPreferences.getDefault(),
                new CitationKeyPatternPreferences(
                        false, false, false, false,
                        CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                        "", "",
                        CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS,
                        GlobalCitationKeyPatterns.fromPattern("[auth][year]"),
                        new SimpleObjectProperty<>(',')),
                FieldPreferences.getDefault(),
                XmpPreferences.getDefault(),
                DOIPreferences.getDefault(),
                new GrobidPreferences(false, false, "http://original.example:8070"),
                FilePreferences.getDefault());

        ImportFormatPreferences overridden = CitationsFromPdf.withOverriddenGrobidUrl(original, "http://override.example:8070");

        assertEquals("http://override.example:8070", overridden.grobidPreferences().getGrobidURL());
        assertFalse(overridden.grobidPreferences().isGrobidUseAsked());
        assertEquals(original.bibEntryPreferences(), overridden.bibEntryPreferences());
        assertEquals(original.citationKeyPatternPreferences(), overridden.citationKeyPatternPreferences());
        assertEquals(original.fieldPreferences(), overridden.fieldPreferences());
        assertEquals(original.xmpPreferences(), overridden.xmpPreferences());
        assertEquals(original.doiPreferences(), overridden.doiPreferences());
        assertEquals(original.filePreferences(), overridden.filePreferences());
    }
}

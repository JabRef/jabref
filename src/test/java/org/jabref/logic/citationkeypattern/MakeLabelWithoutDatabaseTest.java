package org.jabref.logic.citationkeypattern;

import java.util.Collections;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MakeLabelWithoutDatabaseTest {

    private CitationKeyGenerator citationKeyGenerator;

    @BeforeEach
    void setUp() {
        GlobalCitationKeyPattern keyPattern = new GlobalCitationKeyPattern(Collections.emptyList());
        keyPattern.setDefaultValue("[auth]");
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                "",
                new SimpleObjectProperty<>(','));

        citationKeyGenerator = new CitationKeyGenerator(keyPattern, new BibDatabase(), patternPreferences);
    }

    @Test
    void makeAuthorLabelForFileSearch() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "John Doe")
        .withField(StandardField.YEAR, "2016")
        .withField(StandardField.TITLE, "An awesome paper on JabRef");

        String label = citationKeyGenerator.generateKey(entry);
        assertEquals("Doe", label);
    }

    @Test
    void makeEditorLabelForFileSearch() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.EDITOR, "John Doe")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.TITLE, "An awesome paper on JabRef");

        String label = citationKeyGenerator.generateKey(entry);
        assertEquals("Doe", label);
    }
}

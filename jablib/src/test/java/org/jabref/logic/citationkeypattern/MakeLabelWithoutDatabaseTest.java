package org.jabref.logic.citationkeypattern;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class MakeLabelWithoutDatabaseTest {

    private CitationKeyGenerator citationKeyGenerator;
    private CitationKeyPatternPreferences patternPreferences;

    @BeforeEach
    void setUp() {
        GlobalCitationKeyPatterns keyPattern = new GlobalCitationKeyPatterns(CitationKeyPattern.NULL_CITATION_KEY_PATTERN);
        keyPattern.setDefaultValue("[auth]");
        patternPreferences = new CitationKeyPatternPreferences(
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

    @Test
    void bamford_1972_Comprehensive_Reaction_V_7_EN() {
        // Example taken from https://github.com/JabRef/jabref/issues/11367#issuecomment-2162250948
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Bamford_1972_Comprehensive_Reaction_V_7_EN")
                .withField(StandardField.LANGUAGE, "english")
                .withField(StandardField.MAINTITLE, "Comprehensive Chemical Kinetics")
                .withField(StandardField.TITLE, "Reaction of Metallic Salts and Complexes, and Organometallic Compounds")
                .withField(StandardField.VOLUME, "7")
                .withField(StandardField.YEAR, "1972")
                .withField(StandardField.EDITOR, "Bamford, C. H. and Tipper, C. F. H.");
        citationKeyGenerator = new CitationKeyGenerator(GlobalCitationKeyPatterns.fromPattern("[edtr]_[YEAR]_[MAINTITLE:regex(\"(\\w+).*\", \"$1\")]_[TITLE:regex(\"(\\w+).*\", \"$1\")]_V_[VOLUME]_[LANGUAGE:regex(\"english\", \"EN\"):regex(\"french\", \"FR\")]"), new BibDatabase(), patternPreferences);
        String label = citationKeyGenerator.generateKey(entry);
        assertEquals("Bamford_1972_Comprehensive_Reaction_V_7_EN", label);
    }

    @Test
    void frenchRegEx() {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.LANGUAGE, "french");
        citationKeyGenerator = new CitationKeyGenerator(GlobalCitationKeyPatterns.fromPattern("[LANGUAGE:regex(\"english\", \"EN\"):regex(\"french\", \"FR\")]"), new BibDatabase(), patternPreferences);
        String label = citationKeyGenerator.generateKey(entry);
        assertEquals("FR", label);
    }
}

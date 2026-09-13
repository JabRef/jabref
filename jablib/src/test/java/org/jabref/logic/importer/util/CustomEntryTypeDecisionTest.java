package org.jabref.logic.importer.util;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@NullMarked
class CustomEntryTypeDecisionTest {

    private static final BibDatabaseMode MODE = BibDatabaseMode.BIBLATEX;

    private static BibEntryType parse(String comment) {
        return MetaDataParser.parseCustomEntryType(comment).orElseThrow();
    }

    /// A preference key holds at most 80 characters, a value at most 8192 - a definition can be longer than both
    @Test
    void fingerprintOfALongDefinitionFitsIntoAPreferenceKey() {
        String manyFields = IntStream.range(0, 2000).mapToObj(i -> "field" + i).collect(Collectors.joining(";"));
        BibEntryType longType = parse("jabref-entrytype: longtype: req[title] opt[" + manyFields + "]");

        assertEquals(64, CustomEntryTypeDecision.fingerprint(longType, Optional.of(longType), MODE).length());
    }

    @Test
    void reorderedFieldsKeepTheFingerprint() {
        BibEntryType inFile = parse("jabref-entrytype: audio: req[author/editor;title;date] opt[url;urldate]");
        BibEntryType reordered = parse("jabref-entrytype: audio: req[date;title;editor/author] opt[urldate;url]");
        BibEntryType stored = parse("jabref-entrytype: audio: req[title] opt[doi;url]");
        BibEntryType storedReordered = parse("jabref-entrytype: audio: req[title] opt[url;doi]");

        assertEquals(CustomEntryTypeDecision.fingerprint(inFile, Optional.of(stored), MODE),
                CustomEntryTypeDecision.fingerprint(reordered, Optional.of(storedReordered), MODE));
    }

    @Test
    void changedFieldPriorityChangesTheFingerprint() {
        BibEntryType urlDateAsDetail = new BibEntryTypeBuilder()
                .withType(BiblatexNonStandardEntryType.Audio)
                .withRequiredFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.URL)
                .withDetailFields(StandardField.URLDATE)
                .build();
        BibEntryType urlDateAsImportant = new BibEntryTypeBuilder()
                .withType(BiblatexNonStandardEntryType.Audio)
                .withRequiredFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.URL, StandardField.URLDATE)
                .build();

        assertNotEquals(CustomEntryTypeDecision.fingerprint(urlDateAsDetail, Optional.empty(), MODE),
                CustomEntryTypeDecision.fingerprint(urlDateAsImportant, Optional.empty(), MODE));
    }

    @Test
    void differentStoredDefinitionChangesTheFingerprint() {
        BibEntryType inFile = parse("jabref-entrytype: audio: req[author] opt[url]");

        assertNotEquals(CustomEntryTypeDecision.fingerprint(inFile, Optional.empty(), MODE),
                CustomEntryTypeDecision.fingerprint(inFile, Optional.of(parse("jabref-entrytype: audio: req[title] opt[url]")), MODE));
    }
}

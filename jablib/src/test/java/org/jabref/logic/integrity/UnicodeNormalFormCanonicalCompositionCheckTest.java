package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class UnicodeNormalFormCanonicalCompositionCheckTest {
    UnicodeNormalFormCanonicalCompositionCheck checker = new UnicodeNormalFormCanonicalCompositionCheck();

    @Test
    void asciiStringShouldReturnEmptyList() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.AUTHOR, "John Doe");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void normalizedStringShouldReturnEmptyList() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Caf́é")
                .withField(StandardField.AUTHOR, "John Doe");
        assertEquals(List.of(), checker.check(entry));
    }

    @Test
    void nonNormalizedLetterAWithAcuteShouldReturnIntegrityMessage() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "\u0041\u0301");
        assertEquals(List.of(new IntegrityMessage("Value is not in Unicode's Normalization Form \"Canonical Composition\" (NFC) format", entry, StandardField.TITLE)), checker.check(entry));
    }

    @Test
    void checkWithNormalizedLetterAWithAcuteShouldReturnIntegrityMessage() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "\u00C1");
        assertEquals(List.of(), checker.check(entry));
    }
}

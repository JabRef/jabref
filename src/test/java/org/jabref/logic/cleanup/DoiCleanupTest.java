package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jabref.model.entry.field.UnknownField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoiCleanupTest {

    private BibEntry expected;

    @BeforeEach
    public void setUp() {
        expected = new BibEntry()
                .withField(StandardField.DOI, "10.1145/2594455");
    }

    @Test
    void cleanupDoiEntryJustDoi() {

        BibEntry input = new BibEntry()
                .withField(StandardField.DOI, "10.1145/2594455");


        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanupDoiEntryJustDoiAllEntries() {

        UnknownField unknownField = new UnknownField("ee");

        BibEntry input = new BibEntry()
                .withField(StandardField.DOI, "10.1145/2594455")
                .withField(StandardField.URL, "https://doi.org/10.1145/2594455")
                .withField(StandardField.NOTE, "https://doi.org/10.1145/2594455")
                .withField(unknownField, "https://doi.org/10.1145/2594455");


        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

}

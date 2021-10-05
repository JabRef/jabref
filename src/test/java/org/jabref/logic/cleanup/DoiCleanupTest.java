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

    @Test
    void cleanupDoiEntryDoiFieldsWithoutHttp() {

        UnknownField unknownField = new UnknownField("ee");

        BibEntry input = new BibEntry()
                .withField(StandardField.DOI, "10.1145/2594455")
                .withField(StandardField.NOTE, "This is a random note to this Doi")
                .withField(unknownField, "This is a random ee field for this Doi");

        BibEntry output = new BibEntry()
                .withField(StandardField.DOI, "10.1145/2594455")
                .withField(StandardField.NOTE, "This is a random note to this Doi")
                .withField(unknownField, "This is a random ee field for this Doi");

        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(output, input);
    }

    @Test
    void cleanupDoiEntryDoiWithSpaces() {

        BibEntry input = new BibEntry()
                .withField(StandardField.DOI, "10.1145 / 2594455");

        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanupDoiJustUrl() {

        BibEntry input = new BibEntry()
                .withField(StandardField.URL, "https://doi.org/10.1145/2594455");

        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanupDoiJustNote() {

        BibEntry input = new BibEntry()
                .withField(StandardField.NOTE, "https://doi.org/10.1145/2594455");

        DoiCleanup cleanup = new DoiCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }


}

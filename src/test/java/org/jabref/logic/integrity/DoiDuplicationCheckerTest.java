package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoiDuplicationCheckerTest {

    private final DoiDuplicationChecker checker = new DoiDuplicationChecker();
    private BibEntry entry1;
    private BibEntry entry2;
    private BibEntry entry3;
    private BibEntry entry4;
    private BibEntry entry5;

    @BeforeEach
    void setUp() {
        // entry1 and entry2 have duplicate DOI
        entry1 = new BibEntry().withField(StandardField.DOI, "10.1023/A:1022883727209");
        entry2 = new BibEntry().withField(StandardField.DOI, "10.1023/A:1022883727209");

        // entry3 and entry4 have duplicate DOI
        entry3 = new BibEntry().withField(StandardField.DOI, "10.1177/1461444811422887");
        entry4 = new BibEntry().withField(StandardField.DOI, "10.1177/1461444811422887");

        // entry5 has a different DOI than the above
        entry5 = new BibEntry().withField(StandardField.DOI, "10.1145/2568225.2568315");

    }

    @Test
    public void testOnePairDuplicateDOI() {
        List<BibEntry> entries = List.of(entry1, entry2, entry5);
        BibDatabase database = new BibDatabase(entries);
        List<IntegrityMessage> results = List.of(new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry1, StandardField.DOI),
        new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry2, StandardField.DOI));
        assertEquals(results, checker.check(database));
    }

    @Test
    public void testMultiPairsDuplicateDOI() {
        List<BibEntry> entries = List.of(entry1, entry2, entry3, entry4, entry5);
        BibDatabase database = new BibDatabase(entries);
        List<IntegrityMessage> results = List.of(new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry1, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry2, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry3, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), entry4, StandardField.DOI));
        assertEquals(results, checker.check(database));
    }

    @Test
    public void testNoDuplicateDOI() {
        List<BibEntry> entries = List.of(entry1, entry3, entry5);
        BibDatabase database = new BibDatabase(entries);
        assertEquals(Collections.emptyList(), checker.check(database));
    }
}

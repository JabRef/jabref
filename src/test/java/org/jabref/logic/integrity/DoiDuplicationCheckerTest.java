package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoiDuplicationCheckerTest {

    private final DoiDuplicationChecker checker = new DoiDuplicationChecker();
    private String doiA = "10.1023/A:1022883727209";
    private String doiB = "10.1177/1461444811422887";
    private String doiC = "10.1145/2568225.2568315";
    private BibEntry doiA_entry1 = new BibEntry().withField(StandardField.DOI, doiA);
    private BibEntry doiA_entry2 = new BibEntry().withField(StandardField.DOI, doiA);
    private BibEntry doiB_entry1 = new BibEntry().withField(StandardField.DOI, doiB);
    private BibEntry doiB_entry2 = new BibEntry().withField(StandardField.DOI, doiB);
    private BibEntry doiC_entry1 = new BibEntry().withField(StandardField.DOI, doiC);

    @Test
    public void testOnePairDuplicateDOI() {
        List<BibEntry> entries = List.of(doiA_entry1, doiA_entry2, doiC_entry1);
        BibDatabase database = new BibDatabase(entries);
        List<IntegrityMessage> results = List.of(new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiA_entry1, StandardField.DOI),
        new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiA_entry2, StandardField.DOI));
        assertEquals(results, checker.check(database));
    }

    @Test
    public void testMultiPairsDuplicateDOI() {
        List<BibEntry> entries = List.of(doiA_entry1, doiA_entry2, doiB_entry1, doiB_entry2, doiC_entry1);
        BibDatabase database = new BibDatabase(entries);
        List<IntegrityMessage> results = List.of(new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiA_entry1, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiA_entry2, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiB_entry1, StandardField.DOI),
                new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), doiB_entry2, StandardField.DOI));
        assertEquals(results, checker.check(database));
    }

    @Test
    public void testNoDuplicateDOI() {
        List<BibEntry> entries = List.of(doiA_entry1, doiB_entry1, doiC_entry1);
        BibDatabase database = new BibDatabase(entries);
        assertEquals(Collections.emptyList(), checker.check(database));
    }
}

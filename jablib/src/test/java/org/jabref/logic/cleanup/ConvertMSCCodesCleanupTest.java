package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvertMSCCodesCleanupTest {

    private ConvertMSCCodesCleanup worker;
    private ConvertMSCCodesCleanup reverseWorker;

    @BeforeEach
    void setUp() {
        worker = new ConvertMSCCodesCleanup(
                new BibEntryPreferences(','),
                true
        );
        reverseWorker = new ConvertMSCCodesCleanup(
                new BibEntryPreferences(','),
                false
        );
    }

    @Test
    void cleanupConvertsSingleMSCCodeToDescription() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "00B60");

        worker.cleanup(entry);

        assertEquals(
                Optional.of("Collections of reprinted articles [See also 01A75]"),
                entry.getField(StandardField.KEYWORDS)
        );
    }

    @Test
    void cleanupConvertsMultipleMSCCodeToDescription() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "00B60,53A17");

        worker.cleanup(entry);

        assertEquals(
                Optional.of("Collections of reprinted articles [See also 01A75],Differential geometric aspects in kinematics"),
                entry.getField(StandardField.KEYWORDS)
        );
    }

    @Test
    void cleanupConvertsSingleMSCCodeToDescriptionAndIgnoreCustomKeys() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "00B60, CustomKey1, CustomKey2");

        worker.cleanup(entry);

        assertEquals(
                Optional.of(
                        "Collections of reprinted articles [See also 01A75],CustomKey1,CustomKey2"
                        ),
                entry.getField(StandardField.KEYWORDS)
        );
    }

    @Test
    void cleanupEmptyKeywordFieldDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void cleanupConvertsDescriptionToSingleMSCCode() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS,
                "Random dynamical systems [See also 15B52 34Fxx 47B80 70L05 82C05 93Exx],Affine differential geometry"
        );

        reverseWorker.cleanup(entry);

        assertEquals(Optional.of("37Hxx,53A15"), entry.getField(StandardField.KEYWORDS));
    }
}

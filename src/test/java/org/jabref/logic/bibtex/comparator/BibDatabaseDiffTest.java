package org.jabref.logic.bibtex.comparator;

import java.util.Collections;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibDatabaseDiffTest {

    @Test
    public void compareOfEmptyDatabasesReportsNoDifferences() throws Exception {
        BibDatabaseDiff diff = BibDatabaseDiff.compare(new BibDatabaseContext(), new BibDatabaseContext());
        assertEquals(Optional.empty(), diff.getPreambleDifferences());
        assertEquals(Optional.empty(), diff.getMetaDataDifferences());
        assertEquals(Collections.emptyList(), diff.getBibStringDifferences());
        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }

}

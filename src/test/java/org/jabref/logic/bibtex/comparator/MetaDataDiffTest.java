package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataDiffTest {
    @Test
    public void compareWithSameContentSelectorsDoesNotReportAnyDiffs() throws Exception {
        MetaData one = new MetaData();
        one.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));
        MetaData two = new MetaData();
        two.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }
}

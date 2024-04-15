package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataDiffTest {
    @Test
    public void compareWithSameContentSelectorsDoesNotReportAnyDiffs() {
        MetaData one = new MetaData();
        one.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));
        MetaData two = new MetaData();
        two.addContentSelector(new ContentSelector(StandardField.AUTHOR, "first", "second"));

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }

    @Test
    @Disabled
    public void defaultSettingEqualsEmptySetting() {
        MetaData one = new MetaData();
        // Field list is from {@link org.jabref.gui.libraryproperties.contentselectors.ContentSelectorViewModel.DEFAULT_FIELD_NAMES}
        one.addContentSelector(new ContentSelector(StandardField.AUTHOR, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.JOURNAL, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.PUBLISHER, List.of()));
        one.addContentSelector(new ContentSelector(StandardField.KEYWORDS, List.of()));
        MetaData two = new MetaData();

        assertEquals(Optional.empty(), MetaDataDiff.compare(one, two));
    }
}

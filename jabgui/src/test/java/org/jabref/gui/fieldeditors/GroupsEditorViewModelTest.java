package org.jabref.gui.fieldeditors;

import org.jabref.model.entry.Keyword;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupsEditorViewModelTest {
    @Test
    void stringConverterWithHierarchicalGroups() {
        String hierarchicalString = "parent > node > child";
        Keyword group = Keyword.ofHierarchical(hierarchicalString);

        assertEquals(hierarchicalString, GroupsEditorViewModel.getStringConverter().toString(group));
    }
}

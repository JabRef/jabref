package org.jabref.gui.fieldeditors;

import org.jabref.model.entry.Keyword;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordsEditorViewModelTest {
    @Test
    void stringConverterWithHierarchicalKeywords() {
        String hierarchichalString = "parent > node > child";
        Keyword keyword = Keyword.of("parent", "node", "child");

        assertEquals(hierarchichalString, KeywordsEditorViewModel.getStringConverter().toString(keyword));
    }
}

package org.jabref.gui.openoffice;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class StyleSelectDialogViewModelTest {

    @Test
    void matchStyleSearchIgnoresPunctuationSeparators() {
        assertTrue(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "springer "));
    }

    @Test
    void matchStyleSearchFindsCapitalizedSubsetTerms() {
        assertTrue(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "Springer lecture"));
    }

    @Test
    void matchStyleSearchKeepsUnmatchedTermsHidden() {
        assertFalse(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "springer medicine"));
    }
}

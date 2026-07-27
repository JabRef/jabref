package org.jabref.logic.preview;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextBasedPreviewLayoutTest {

    private final LayoutFormatterPreferences layoutFormatterPreferences = LayoutFormatterPreferences.getDefault();
    private final JournalAbbreviationRepository abbreviationRepository = new JournalAbbreviationRepository();

    @Test
    void nameIsSetWhenExplicitlyGiven() {
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Custom Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        assertEquals("My Custom Style", layout.getName());
    }

    @Test
    void displayNameMatchesName() {
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Custom Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        assertEquals("My Custom Style", layout.getDisplayName());
    }

    @Test
    void shortTitleMatchesName() {
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Custom Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        assertEquals("My Custom Style", layout.getShortTitle());
    }

    @Test
    void setNameUpdatesName() {
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("My Custom Style", "some text", layoutFormatterPreferences, abbreviationRepository);
        layout.setName("Renamed name");
        assertEquals("Renamed name", layout.getName());
    }

    @Test
    void twoDistinctStylesHaveDifferentNames() {
        TextBasedPreviewLayout first = new TextBasedPreviewLayout("First", "some text", layoutFormatterPreferences, abbreviationRepository);
        TextBasedPreviewLayout second = new TextBasedPreviewLayout("Second", "some text", layoutFormatterPreferences, abbreviationRepository);
        assertEquals("First", first.getName());
        assertEquals("Second", second.getName());
    }

    @Test
    void threeArgsConstructorStillProducesValidDefaultName() {
        TextBasedPreviewLayout layout = new TextBasedPreviewLayout("some text", layoutFormatterPreferences, abbreviationRepository);
        assertNotNull(layout.getName());
        assertFalse(layout.getName().isBlank());
    }
}

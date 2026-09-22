package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.Optional;

import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MultiMergeEntriesViewTest extends JavaFxTest {

    private MultiMergeEntriesView dialog;

    @Override
    public void start(Stage stage) {
        Localization.setLanguage(Language.ENGLISH);
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        dialog = new MultiMergeEntriesView(preferences, new CurrentThreadTaskExecutor());
    }

    @Test
    void emptyCellOfEarlierSourceDoesNotClearFieldFromLaterSource() {
        interact(() -> {
            dialog.addSource("A", new BibEntry().withField(StandardField.TITLE, "Title"));
            dialog.addSource("B", new BibEntry().withField(StandardField.TITLE, "Title").withField(StandardField.YEAR, "2026"));
        });

        BibEntry merged = dialog.getResultConverter().call(ButtonType.OK);

        assertEquals(Optional.of("2026"), merged.getField(StandardField.YEAR));
    }

    @Test
    void fieldSetOnMergedEntryAfterClosingIsKept() {
        interact(() -> dialog.addSource("A", new BibEntry().withField(StandardField.TITLE, "Title")));
        BibEntry merged = dialog.getResultConverter().call(ButtonType.OK);

        interact(() -> merged.setField(StandardField.FILE, ":paper.pdf:PDF"));

        assertEquals(Optional.of(":paper.pdf:PDF"), merged.getField(StandardField.FILE));
    }
}

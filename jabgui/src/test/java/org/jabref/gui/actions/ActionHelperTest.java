package org.jabref.gui.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanExpression;
import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class ActionHelperTest {

    @TempDir
    private Path tempDir;

    // [utest->req~jabgui.documentviewer.open-crossref-pdf-at-page~1]
    @Test
    void crossrefPdfIsFoundOnceTheCrossrefIsSet() throws IOException {
        BibEntry volume = volumeWith(List.of(new LinkedFile("", Files.createFile(tempDir.resolve("volume.pdf")), "PDF")));
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings);
        BooleanExpression present = crossrefPdfPresentFor(paper, volume);
        boolean before = present.get();

        paper.setField(StandardField.CROSSREF, "volume");

        assertEquals(List.of(false, true), List.of(before, present.get()));
    }

    // [utest->req~jabgui.documentviewer.open-crossref-pdf-at-page~1]
    @Test
    void crossrefPdfIsFoundOnceTheVolumeGetsAPdf() throws IOException {
        BibEntry volume = volumeWith(List.of());
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.CROSSREF, "volume");
        BooleanExpression present = crossrefPdfPresentFor(paper, volume);
        boolean before = present.get();

        volume.addFile(new LinkedFile("", Files.createFile(tempDir.resolve("volume.pdf")), "PDF"));

        assertEquals(List.of(false, true), List.of(before, present.get()));
    }

    private static BibEntry volumeWith(List<LinkedFile> files) {
        return new BibEntry(StandardEntryType.Proceedings).withCitationKey("volume").withFiles(files);
    }

    private static BooleanExpression crossrefPdfPresentFor(BibEntry paper, BibEntry volume) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(paper));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(new BibDatabase(List.of(volume, paper)))));
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        return ActionHelper.isCrossrefPdfPresentForSelectedEntry(stateManager, preferences);
    }
}

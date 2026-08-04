package org.jabref.gui.util;

import java.nio.file.Path;
import java.util.List;

import javafx.scene.input.TransferMode;

import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@NullMarked
class DragDropTest {

    @Test
    void handleDropOfFilesIgnoresBibFiles() {
        ExternalFilesEntryLinker fileLinker = mock(ExternalFilesEntryLinker.class);
        BibEntry entry = new BibEntry();
        List<Path> bibFiles = List.of(Path.of("test.bib"), Path.of("another.bib"));

        DragDrop.handleDropOfFiles(bibFiles, TransferMode.MOVE, fileLinker, entry);

        verifyNoInteractions(fileLinker);
    }

    @Test
    void handleDropOfFilesPassesNonBibFilesToLinker() {
        ExternalFilesEntryLinker fileLinker = mock(ExternalFilesEntryLinker.class);
        BibEntry entry = new BibEntry();
        Path pdfFile = Path.of("paper.pdf");
        Path bibFile = Path.of("test.bib");
        List<Path> files = List.of(pdfFile, bibFile);

        DragDrop.handleDropOfFiles(files, TransferMode.MOVE, fileLinker, entry);

        verify(fileLinker).coveOrMoveFilesSteps(eq(entry), eq(List.of(pdfFile)), eq(true));
    }
}

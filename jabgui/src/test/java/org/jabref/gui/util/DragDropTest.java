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

@NullMarked
class DragDropTest {

    @Test
    void handleDropOfFilesWithMoveMode() {
        ExternalFilesEntryLinker fileLinker = mock(ExternalFilesEntryLinker.class);
        BibEntry entry = new BibEntry();
        Path pdfFile = Path.of("paper.pdf");
        List<Path> files = List.of(pdfFile);

        DragDrop.handleDropOfFiles(files, TransferMode.MOVE, fileLinker, entry);

        verify(fileLinker).coveOrMoveFilesSteps(eq(entry), eq(files), eq(true));
    }

    @Test
    void handleDropOfFilesWithCopyMode() {
        ExternalFilesEntryLinker fileLinker = mock(ExternalFilesEntryLinker.class);
        BibEntry entry = new BibEntry();
        Path pdfFile = Path.of("paper.pdf");
        List<Path> files = List.of(pdfFile);

        DragDrop.handleDropOfFiles(files, TransferMode.COPY, fileLinker, entry);

        verify(fileLinker).coveOrMoveFilesSteps(eq(entry), eq(files), eq(false));
    }

    @Test
    void handleDropOfFilesWithLinkMode() {
        ExternalFilesEntryLinker fileLinker = mock(ExternalFilesEntryLinker.class);
        BibEntry entry = new BibEntry();
        Path pdfFile = Path.of("paper.pdf");
        List<Path> files = List.of(pdfFile);

        DragDrop.handleDropOfFiles(files, TransferMode.LINK, fileLinker, entry);

        verify(fileLinker).linkFilesToEntry(eq(entry), eq(files));
    }
}

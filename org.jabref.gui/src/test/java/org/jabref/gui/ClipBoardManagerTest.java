package org.jabref.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClipBoardManagerTest {

    private ClipBoardManager clipBoardManager;
    private Transferable content;
    private ImportFormatReader importFormatReader;

    @BeforeEach
    public void setUp() throws Exception {
        importFormatReader = mock(ImportFormatReader.class);

        Clipboard clipboard = mock(Clipboard.class);
        clipBoardManager = new ClipBoardManager(clipboard, importFormatReader);
        content = mock(Transferable.class);
        when(clipboard.getContents(ArgumentMatchers.any())).thenReturn(content);
    }

    @Test
    public void extractBibEntriesFromClipboardParsesStringFlavor() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("canh05");
        expected.setField("author", "Crowston, K. and Annabi, H.");
        expected.setField("title", "Title A");

        when(content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)).thenReturn(false);
        when(content.isDataFlavorSupported(DataFlavor.stringFlavor)).thenReturn(true);
        String data = "@article{canh05,  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n";
        when(content.getTransferData(DataFlavor.stringFlavor)).thenReturn(data);
        when(importFormatReader.importUnknownFormat(data)).thenReturn(new UnknownFormatImport("abc", new ParserResult(Arrays.asList(expected))));

        List<BibEntry> actual = clipBoardManager.extractBibEntriesFromClipboard();

        assertEquals(Arrays.asList(expected), actual);
    }

    @Test
    public void extractBibEntriesFromClipboardReturnsEmptyIfStringparsingFailed() throws Exception {
        when(content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)).thenReturn(false);
        when(content.isDataFlavorSupported(DataFlavor.stringFlavor)).thenReturn(true);
        when(content.getTransferData(DataFlavor.stringFlavor)).thenReturn("testData");
        when(importFormatReader.importUnknownFormat("testData")).thenThrow(new ImportException(""));

        List<BibEntry> actual = clipBoardManager.extractBibEntriesFromClipboard();

        assertEquals(Arrays.asList(), actual);
    }

}

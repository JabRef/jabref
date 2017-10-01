package org.jabref.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.xmp.XMPPreferences;
import org.jabref.model.entry.BibEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClipBoardManagerTest {

    private ClipBoardManager clipBoardManager;
    private Clipboard clipboard;
    private Transferable content;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        XMPPreferences xmpPreferences = mock(XMPPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        ImportFormatReader importFormatReader = new ImportFormatReader();
        importFormatReader.resetImportFormats(importFormatPreferences, xmpPreferences);

        clipBoardManager = new ClipBoardManager(importFormatReader);
        clipboard = mock(Clipboard.class);
        Field field = ClipBoardManager.class.getDeclaredField("CLIPBOARD");
        try {
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(ClipBoardManager.class, clipboard);
        } finally {
            field.setAccessible(false);
        }

        content = mock(Transferable.class);
        when(clipboard.getContents(ArgumentMatchers.any())).thenReturn(content);
    }

    @Test
    public void testExtractBibEntriesFromClipboard_parseFromStringFlavor_bibtex() throws Exception {
        when(content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)).thenReturn(false);
        when(content.isDataFlavorSupported(DataFlavor.stringFlavor)).thenReturn(true);
        when(content.getTransferData(DataFlavor.stringFlavor)).thenReturn("@article{canh05,  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n");

        List<BibEntry> actual = clipBoardManager.extractBibEntriesFromClipboard();

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("canh05");
        expected.setField("author", "Crowston, K. and Annabi, H.");
        expected.setField("title", "Title A");
        assertEquals(Arrays.asList(expected), actual);
    }

    @Test
    public void testExtractBibEntriesFromClipboard_parseFromStringFlavor_ris() throws Exception {
        when(content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)).thenReturn(false);
        when(content.isDataFlavorSupported(DataFlavor.stringFlavor)).thenReturn(true);
        when(content.getTransferData(DataFlavor.stringFlavor)).thenReturn("TY  - JOUR\nTI  - Title A\nAU  - Crowston, K.\nAU  - Annabi, H.\nER  -");

        List<BibEntry> actual = clipBoardManager.extractBibEntriesFromClipboard();

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setField("author", "Crowston, K. and Annabi, H.");
        expected.setField("title", "Title A");
        assertEquals(Arrays.asList(expected), actual);
    }

    @Test
    public void testExtractBibEntriesFromClipboard_parseFromStringFlavor_notAnEntry() throws Exception {
        when(content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)).thenReturn(false);
        when(content.isDataFlavorSupported(DataFlavor.stringFlavor)).thenReturn(true);
        when(content.getTransferData(DataFlavor.stringFlavor)).thenReturn("this is not an entry");

        List<BibEntry> actual = clipBoardManager.extractBibEntriesFromClipboard();

        assertEquals(Arrays.asList(), actual);
    }

}

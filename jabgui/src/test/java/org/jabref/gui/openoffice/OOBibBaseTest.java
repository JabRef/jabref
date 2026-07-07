package org.jabref.gui.openoffice;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.oocsltext.CSLCitationOOAdapter;
import org.jabref.logic.openoffice.oocsltext.CSLUpdateBibliography;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.text.XTextDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OOBibBaseTest {

    @Test
    void updateCSLBibliographyDoesNotLockControllersWhenNoEntriesAreCited() throws Exception {
        DialogService dialogService = mock(DialogService.class);
        XTextDocument doc = mock(XTextDocument.class);
        FunctionalTextViewCursor fcursor = mock(FunctionalTextViewCursor.class);
        CSLCitationOOAdapter cslCitationOOAdapter = mock(CSLCitationOOAdapter.class);
        CSLUpdateBibliography cslUpdateBibliography = mock(CSLUpdateBibliography.class);
        CitationStyle citationStyle = mock(CitationStyle.class);

        doNothing().when(fcursor).restore(doc);

        OOVoidResult<OOError> result = OOBibBase.updateCSLBibliography(
                dialogService,
                List.of(new BibDatabase()),
                citationStyle,
                doc,
                OOResult.ok(fcursor),
                "Error title",
                cslCitationOOAdapter,
                cslUpdateBibliography);

        assertTrue(result.isOK());
        verify(doc, never()).lockControllers();
        verify(doc, never()).unlockControllers();
        verify(cslUpdateBibliography, never()).rebuildCSLBibliography(any(), any(), any(), any(), any(), any());
        verify(fcursor).restore(doc);
    }

    @Test
    void updateCSLBibliographyReturnsErrorWhenRebuildThrowsNoDocumentException() throws Exception {
        DialogService dialogService = mock(DialogService.class);
        XTextDocument doc = mock(XTextDocument.class);
        FunctionalTextViewCursor fcursor = mock(FunctionalTextViewCursor.class);
        CSLCitationOOAdapter cslCitationOOAdapter = mock(CSLCitationOOAdapter.class);
        CSLUpdateBibliography cslUpdateBibliography = mock(CSLUpdateBibliography.class);
        CitationStyle citationStyle = mock(CitationStyle.class);
        BibEntry bibEntry = new BibEntry();

        doNothing().when(fcursor).restore(doc);
        when(cslCitationOOAdapter.isCitedEntry(bibEntry)).thenReturn(true);
        doThrow(new NoDocumentException()).when(cslUpdateBibliography).rebuildCSLBibliography(
                eq(doc),
                eq(cslCitationOOAdapter),
                any(),
                eq(citationStyle),
                any(),
                any());

        OOVoidResult<OOError> result = OOBibBase.updateCSLBibliography(
                dialogService,
                List.of(new BibDatabase(List.of(bibEntry))),
                citationStyle,
                doc,
                OOResult.ok(fcursor),
                "Error title",
                cslCitationOOAdapter,
                cslUpdateBibliography);

        assertTrue(result.isError());
        assertEquals("Error title", result.getError().getTitle());
        verify(doc).lockControllers();
        verify(doc).unlockControllers();
        verify(fcursor).restore(doc);
    }
}

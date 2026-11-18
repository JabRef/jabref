package org.jabref.gui.openoffice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import static org.jabref.logic.l10n.Localization.lang;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
@ResourceLock("Localization.lang")
class OpenOfficePanelTest {

    private final OpenOfficePanel panel = mock(OpenOfficePanel.class, CALLS_REAL_METHODS);
    private final DialogService dialogs = mock(DialogService.class);
    private final LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
    private final LibraryTab targetTab = mock(LibraryTab.class);
    private final OOBibBase connection = mock(OOBibBase.class);
    private final XTextDocument document = mock(XTextDocument.class);

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        setField("dialogService", dialogs);
        setField("tabContainer", tabContainer);
    }

    @Test
    void importWithoutLibraryReportsMissingLibraryBeforeAccessingDocument() throws ReflectiveOperationException {
        setField("ooBase", connection);

        importEntries();

        verify(dialogs).notify(lang("No library open"));
        verifyNoInteractions(connection, targetTab);
    }

    @Test
    void importWithoutConnectionReportsMissingDocument() throws ReflectiveOperationException {
        when(tabContainer.getCurrentLibraryTab()).thenReturn(targetTab);

        importEntries();

        verify(dialogs).notify(lang("Not connected to document"));
        verifyNoInteractions(connection, targetTab);
    }

    @Test
    void importStopsWhenDocumentAccessReportsAnError() throws ReflectiveOperationException {
        connectDocument();
        when(connection.testDialog(anyString(), ArgumentMatchers.<OOVoidResult<OOError>>any())).thenReturn(true);

        importEntries();

        verify(connection).getXTextDocument();
        verifyNoInteractions(targetTab);
    }

    @Test
    void emptyDocumentDoesNotStartImport() throws ReflectiveOperationException {
        connectDocument();
        try (MockedStatic<UnoReferenceMark> marks = mockStatic(UnoReferenceMark.class)) {
            marks.when(() -> UnoReferenceMark.getListOfNames(document)).thenReturn(List.of());

            importEntries();

            verify(dialogs).notify(lang("No new cited entries to import"));
            verifyNoInteractions(targetTab);
        }
    }

    @Test
    void importPassesDocumentEntriesToDuplicateHandlerWithoutFilteringByLibraryKey() throws ReflectiveOperationException {
        connectDocument();
        String mark = ZoteroReferenceMark.PREFIX + """
                {"citationItems":[{"id":42,"uris":["http://www.jabref.org/Smith2024"],
                "itemData":{"id":42,"type":"article-journal","title":"Distinct paper"}}]}
                """ + " RNDabcdefghij";
        try (MockedStatic<UnoReferenceMark> marks = mockStatic(UnoReferenceMark.class)) {
            marks.when(() -> UnoReferenceMark.getListOfNames(document)).thenReturn(List.of(mark));

            importEntries();

            ArgumentCaptor<List<BibEntry>> entries = ArgumentCaptor.captor();
            verify(targetTab).importCitedEntriesWithDuplicateCheck(entries.capture());
            assertEquals(List.of(Optional.of("Smith2024")), entries.getValue().stream().map(BibEntry::getCitationKey).toList());
            assertEquals(List.of(Optional.of("Distinct paper")), entries.getValue().stream().map(entry -> entry.getField(StandardField.TITLE)).toList());
            verify(targetTab, never()).getDatabase();
        }
    }

    @Test
    void documentClosingBeforeMarkReadReportsDisconnection() throws ReflectiveOperationException {
        connectDocument();
        try (MockedStatic<UnoReferenceMark> marks = mockStatic(UnoReferenceMark.class)) {
            marks.when(() -> UnoReferenceMark.getListOfNames(document)).thenThrow(new NoDocumentException("Document closed"));

            importEntries();

            verify(dialogs).notify(lang("Not connected to document"));
            verifyNoInteractions(targetTab);
        }
    }

    private void connectDocument() throws ReflectiveOperationException {
        setField("ooBase", connection);
        when(tabContainer.getCurrentLibraryTab()).thenReturn(targetTab);
        when(connection.getXTextDocument()).thenReturn(OOResult.ok(document));
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        Field field = OpenOfficePanel.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(panel, value);
    }

    private void importEntries() throws ReflectiveOperationException {
        Method method = OpenOfficePanel.class.getDeclaredMethod("importEntries");
        method.setAccessible(true);
        method.invoke(panel);
    }
}

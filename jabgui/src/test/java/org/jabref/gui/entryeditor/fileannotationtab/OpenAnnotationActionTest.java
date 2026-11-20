package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Covers the paths through a configured reader. The document viewer branch is left to manual testing.
@DisabledOnOs(value = OS.MAC, disabledReason = "No reader can be told a page on macOS, so no command line is built")
class OpenAnnotationActionTest {

    private static final Path PDF_FILE = Path.of("C:/My Library/some paper.pdf");
    private static final Path SUMATRA = Path.of("C:/Program Files/SumatraPDF/SumatraPDF.exe");
    private static final Path READER_WITHOUT_PAGE_SUPPORT = Path.of("C:/Program Files/Some Reader/reader.exe");
    private static final int PAGE = 7;

    private final DialogService dialogService = mock(DialogService.class);
    private final NativeDesktop nativeDesktop = mock(NativeDesktop.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final ProcessBuilder processBuilder = mock(ProcessBuilder.class);
    private final Process process = mock(Process.class);
    private final OutputStream stdin = mock(OutputStream.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);

    @BeforeEach
    void setUp() throws IOException {
        ExternalFileType pdfType = mock(ExternalFileType.class);
        when(pdfType.getExtension()).thenReturn("pdf");
        when(pdfType.getOpenWithApplication()).thenReturn(SUMATRA.toString());

        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(pdfType));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);

        when(process.getOutputStream()).thenReturn(stdin);
        when(process.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(processBuilder.start()).thenReturn(process);
    }

    /// Both paths carry a space, so the command line also shows that neither is split into two
    /// arguments the way the removed 2017 implementation did.
    // [utest->feat~entry-editor.file-annotations.open-at-annotation~1]
    @Test
    void configuredKnownReaderIsStartedAtThePageOfTheAnnotation() {
        FileAnnotationViewModel annotation = annotationOnPage(PAGE);

        action().openAtAnnotation(PDF_FILE, annotation, processBuilder);

        verify(processBuilder).command(List.of(
                SUMATRA.toString(), "-reuse-instance", "-page", "7", PDF_FILE.toString()));
    }

    /// The reader is started detached, so that no pipe to it stays open
    @Test
    void readerIsStartedDetached() throws IOException {
        action().openAtAnnotation(PDF_FILE, annotationOnPage(PAGE), processBuilder);

        verify(stdin).close();
    }

    @Test
    void readerThatCannotBeStartedIsReportedToTheUser() throws IOException {
        when(processBuilder.start()).thenThrow(new IOException("no such executable"));

        action().openAtAnnotation(PDF_FILE, annotationOnPage(PAGE), processBuilder);

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("Could not call executable '%0'.", SUMATRA.toString()),
                Localization.lang("Please check the path in the preferences."));
    }

    @Test
    void readerThatCannotBeToldAPageOpensTheFileWithoutPage() throws IOException {
        ExternalFileType pdfType = mock(ExternalFileType.class);
        when(pdfType.getExtension()).thenReturn("pdf");
        when(pdfType.getOpenWithApplication()).thenReturn(READER_WITHOUT_PAGE_SUPPORT.toString());
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(pdfType));

        action().openAtAnnotation(PDF_FILE, annotationOnPage(PAGE), processBuilder);

        verify(nativeDesktop).openFile(PDF_FILE.toString(), "pdf");
        verify(dialogService).notify(Localization.lang("The configured PDF reader cannot open a PDF at a specific page."));
    }

    @Test
    void actionIsExecutableOnlyWithBothAFileAndAnAnnotation() {
        assertFalse(actionFor(new SimpleObjectProperty<>(), annotationProperty()).executableProperty().get());
        assertFalse(actionFor(new SimpleObjectProperty<>(PDF_FILE), new SimpleObjectProperty<>()).executableProperty().get());
        assertTrue(actionFor(new SimpleObjectProperty<>(PDF_FILE), annotationProperty()).executableProperty().get());
    }

    private OpenAnnotationAction action() {
        return actionFor(new SimpleObjectProperty<>(PDF_FILE), annotationProperty());
    }

    private OpenAnnotationAction actionFor(ObjectProperty<Path> currentFile,
                                           ObjectProperty<FileAnnotationViewModel> currentAnnotation) {
        return new OpenAnnotationAction(currentFile, currentAnnotation, new BibEntry(),
                stateManager, preferences, dialogService, nativeDesktop);
    }

    private static ObjectProperty<FileAnnotationViewModel> annotationProperty() {
        return new SimpleObjectProperty<>(annotationOnPage(PAGE));
    }

    private static FileAnnotationViewModel annotationOnPage(int page) {
        return new FileAnnotationViewModel(new FileAnnotation(
                "Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 17, 24), page, "A simple pop-up note",
                FileAnnotationType.TEXT, Optional.empty()));
    }
}

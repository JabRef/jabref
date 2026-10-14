package org.jabref.gui.entryeditor.fileannotationtab;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.ObjectExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.documentviewer.DocumentViewerView;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.pdf.reader.ExternalPdfReader;
import org.jabref.logic.pdf.reader.ExternalPdfReaders;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Opens the PDF of the selected file annotation at the page that annotation was imported from.
///
/// Which viewer is used depends on what the user configured for the PDF file type:
///
/// - a reader known to [ExternalPdfReaders] is launched at the page
/// - a reader JabRef does not know, and every reader on macOS, is launched without a position,
///   because no page argument may be guessed, and the user is told why
/// - with nothing configured JabRef cannot tell which reader the OS would pick, so its own
///   document viewer opens at the page, fitting the whole page into the window so that an
///   annotation in its lower half is visible
@NullMarked
public class OpenAnnotationAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAnnotationAction.class);

    private final ObjectExpression<@Nullable Path> currentFile;
    private final ObjectExpression<@Nullable FileAnnotationViewModel> currentAnnotation;
    private final BibEntry entry;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    /// Reused so that repeated clicks reuse one window instead of stacking a dialog per click,
    /// as [FulltextSearchResultsTab] does for the same viewer.
    private @Nullable DocumentViewerView documentViewer;

    public OpenAnnotationAction(ObjectExpression<@Nullable Path> currentFile,
                                ObjectExpression<@Nullable FileAnnotationViewModel> currentAnnotation,
                                BibEntry entry,
                                StateManager stateManager,
                                GuiPreferences preferences,
                                DialogService dialogService) {
        this.currentFile = currentFile;
        this.currentAnnotation = currentAnnotation;
        this.entry = entry;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;

        this.executable.bind(currentFile.isNotNull().and(currentAnnotation.isNotNull()));
    }

    /// Does nothing when nothing is selected: a double-click reaches this even then.
    @Override
    public void execute() {
        Optional.ofNullable(currentFile.get()).ifPresent(pdfFile ->
                Optional.ofNullable(currentAnnotation.get()).ifPresent(annotation ->
                        openAtAnnotation(pdfFile, annotation, new ProcessBuilder())));
    }

    // [impl->feat~entry-editor.file-annotations.open-at-annotation~1]
    @VisibleForTesting
    void openAtAnnotation(Path pdfFile, FileAnnotationViewModel annotation, ProcessBuilder processBuilder) {
        getConfiguredApplication().ifPresentOrElse(
                executable -> openInConfiguredApplication(executable, pdfFile, annotation, processBuilder),
                () -> {
                    LOGGER.debug("No application configured for the PDF file type, using the document viewer");
                    openInDocumentViewer(pdfFile, annotation);
                });
    }

    private void openInConfiguredApplication(Path executable, Path pdfFile, FileAnnotationViewModel annotation, ProcessBuilder processBuilder) {
        // On macOS the arguments never reach the viewer: `open -a` drops them and neither Preview
        // nor Acrobat can be told a page any other way.
        Optional<ExternalPdfReader> reader = OS.OS_X ? Optional.empty() : ExternalPdfReaders.getReader(executable);
        LOGGER.debug("Configured PDF application '{}' matched reader {}", executable, reader.map(ExternalPdfReader::getDisplayName));
        reader.ifPresentOrElse(
                knownReader -> openExternally(
                        knownReader.getCommandLine(executable, pdfFile, annotation.getPageNumber()),
                        processBuilder),
                () -> openWithoutPage(pdfFile, executable));
    }

    /// @return the application the user configured for the PDF file type, empty when the file is
    ///         handed to the operating system instead
    private Optional<Path> getConfiguredApplication() {
        return ExternalFileTypes.getExternalFileTypeByExt("pdf", preferences.getExternalApplicationsPreferences())
                                .map(ExternalFileType::getOpenWithApplication)
                                .filter(application -> !application.isBlank())
                                .flatMap(OpenAnnotationAction::toPath);
    }

    private static Optional<Path> toPath(String application) {
        try {
            return Optional.of(Path.of(application));
        } catch (InvalidPathException e) {
            LOGGER.debug("Configured PDF application '{}' is not a path", application, e);
            return Optional.empty();
        }
    }

    private void openExternally(List<String> commandLine, ProcessBuilder processBuilder) {
        processBuilder.command(commandLine);
        try {
            processBuilder.start();
        } catch (IOException e) {
            LOGGER.warn("Could not run {}", commandLine, e);
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Could not call executable '%0'.", commandLine.getFirst()),
                    Localization.lang("Please check the path in the preferences."));
        }
    }

    private void openWithoutPage(Path pdfFile, Path executable) {
        try {
            NativeDesktop.get().openFileWithApplication(pdfFile.toString(), executable.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not open '{}' with '{}'", pdfFile, executable, e);
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Unable to open file '%0'.", pdfFile.toString()),
                    Localization.lang("Please check the path in the preferences."));
            return;
        }
        dialogService.notify(Localization.lang("The configured PDF reader cannot open a PDF at a specific page."));
    }

    private void openInDocumentViewer(Path pdfFile, FileAnnotationViewModel annotation) {
        findLinkedFile(pdfFile).ifPresentOrElse(
                linkedFile -> showDocumentViewer(linkedFile, annotation.getPageNumber()),
                () -> {
                    LOGGER.debug("No linked file of {} resolves to '{}'", entry, pdfFile);
                    dialogService.notify(Localization.lang("Could not find file '%0'.", pdfFile.toString()));
                });
    }

    private void showDocumentViewer(LinkedFile linkedFile, int page) {
        if (documentViewer == null) {
            documentViewer = new DocumentViewerView();
        }
        documentViewer.switchToFile(linkedFile);
        // Without this the page opens at its top, which leaves an annotation in the lower half of
        // the page outside the viewport.
        documentViewer.showFullPage();
        documentViewer.gotoPage(page);
        documentViewer.disableLiveMode();
        dialogService.showCustomDialog(documentViewer);
    }

    /// The annotation tab works on paths already resolved against the library's file directories,
    /// while the document viewer addresses its documents by [LinkedFile].
    private Optional<LinkedFile> findLinkedFile(Path pdfFile) {
        return stateManager.getActiveDatabase()
                           .flatMap(databaseContext -> entry.getFiles().stream()
                                                            .filter(file -> file.findIn(databaseContext, preferences.getFilePreferences())
                                                                                .filter(pdfFile::equals)
                                                                                .isPresent())
                                                            .findFirst());
    }
}

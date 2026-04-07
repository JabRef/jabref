package org.jabref.gui.documentviewer;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.PdfPageLabelResolver;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentViewerViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("\\d+");

    private final StateManager stateManager;
    private final CliPreferences preferences;
    private final ObjectProperty<Path> currentDocument = new SimpleObjectProperty<>();
    private final ListProperty<LinkedFile> files = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty liveMode = new SimpleBooleanProperty(true);
    private final IntegerProperty currentPage = new SimpleIntegerProperty();
    private final StringProperty highlightText = new SimpleStringProperty();
    private final DialogService dialogService;

    public DocumentViewerViewModel(@NonNull StateManager stateManager,
                                   @NonNull CliPreferences preferences,
                                   @NonNull DialogService dialogService) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;

        this.stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) _ -> {
            // Switch to currently selected entry in live mode
            if (liveMode.get()) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        this.liveMode.addListener((_, oldValue, newValue) -> {
            // Switch to currently selected entry if mode is changed to live
            if ((oldValue != newValue) && newValue) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public ObjectProperty<Path> currentDocumentProperty() {
        return currentDocument;
    }

    public ListProperty<LinkedFile> filesProperty() {
        return files;
    }

    public StringProperty highlightTextProperty() {
        return highlightText;
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            files.clear();
            currentDocument.set(null);
            currentPage.set(0);
        } else {
            currentPage.set(getPageToShow(entries.getFirst()));

            Set<LinkedFile> pdfFiles = entries.stream()
                                              .map(this::getPdfFilesToDisplay)
                                              .flatMap(List::stream)
                                              .filter(this::isPdfFile)
                                              .collect(Collectors.toSet());

            if (pdfFiles.isEmpty()) {
                files.clear();
                currentDocument.set(null);
                dialogService.notify(Localization.lang("No PDF files available"));
            } else {
                files.setValue(FXCollections.observableArrayList(pdfFiles));
            }
        }
    }

    private int getPageToShow(BibEntry entry) {
        return entry.getField(StandardField.PAGES)
                    .flatMap(DocumentViewerViewModel::parseFirstPageNumber)
                    .filter(pageNumber -> pageNumber > 0)
                    .map(pageNumber -> pageNumber - 1)
                    .orElse(0);
    }

    private List<LinkedFile> getPdfFilesToDisplay(BibEntry entry) {
        List<LinkedFile> directPdfFiles = entry.getFiles().stream()
                                               .filter(this::isPdfFile)
                                               .toList();
        if (!directPdfFiles.isEmpty()) {
            return directPdfFiles;
        }

        return stateManager.getActiveDatabase()
                           .flatMap(databaseContext -> entry.getField(StandardField.CROSSREF)
                                                           .filter(crossref -> !crossref.isBlank())
                                                           .flatMap(databaseContext.getDatabase()::getEntryByCitationKey))
                           .map(BibEntry::getFiles)
                           .stream()
                           .flatMap(List::stream)
                           .filter(this::isPdfFile)
                           .toList();
    }

    private static Optional<Integer> parseFirstPageNumber(String pages) {
        Matcher matcher = PAGE_NUMBER_PATTERN.matcher(pages);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(matcher.group()));
        } catch (NumberFormatException exception) {
            LOGGER.debug("Could not parse first page number from '{}'", pages, exception);
            return Optional.empty();
        }
    }

    private void setCurrentDocument(Path path) {
        if (FileUtil.isPDFFile(path)) {
            currentDocument.set(path);
        }
    }

    private boolean isPdfFile(LinkedFile file) {
        if (file == null || file.getLink() == null || file.getLink().trim().isEmpty()) {
            return false;
        }

        try {
            Path filePath = Path.of(file.getLink());
            return FileUtil.isPDFFile(filePath);
        } catch (InvalidPathException | SecurityException e) {
            return false;
        }
    }

    public void switchToFile(LinkedFile file) {
        if (file != null) {
            setCurrentPageFromSelectedEntry();
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferences.getFilePreferences()))
                        .ifPresentOrElse(
                                resolvedPath -> {
                                    setCurrentPageFromSelectedEntry(resolvedPath);
                                    setCurrentDocument(resolvedPath);
                                },
                                () -> {
                                    currentDocument.set(null);
                                    LOGGER.warn("Could not find or access file: {}", file.getLink());
                                }
                        );
        } else {
            currentDocument.set(null);
        }
    }

    private void setCurrentPageFromSelectedEntry() {
        stateManager.getSelectedEntries().stream().findFirst().ifPresent(entry -> {
            int pageToShow = getPageToShow(entry);
            currentPage.set(pageToShow);
        });
    }

    private void setCurrentPageFromSelectedEntry(Path pdfPath) {
        stateManager.getSelectedEntries().stream().findFirst().ifPresent(entry -> {
            int logicalPageNumber = getPageToShow(entry) + 1;
            int physicalPageNumber = PdfPageLabelResolver.resolvePhysicalPageNumber(pdfPath, logicalPageNumber);
            int pageIndexToShow = Math.max(0, physicalPageNumber - 1);
            currentPage.set(pageIndexToShow);
        });
    }

    public void showPage(int pageNumber) {
        currentPage.set(pageNumber - 1);
    }

    public void setLiveMode(boolean value) {
        this.liveMode.set(value);
    }

    public void highlightText(String text) {
        this.highlightText.set(text);
    }
}

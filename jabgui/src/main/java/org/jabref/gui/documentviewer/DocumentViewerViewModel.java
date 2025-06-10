package org.jabref.gui.documentviewer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jabref.gui.StateManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentViewerViewModel extends AbstractViewModel {

    private final StateManager stateManager;
    private final CliPreferences preferences;
    private final ObjectProperty<Path> currentDocument = new SimpleObjectProperty<>();
    private final ListProperty<LinkedFile> files = new SimpleListProperty<>();
    private final BooleanProperty liveMode = new SimpleBooleanProperty(true);
    private final IntegerProperty currentPage = new SimpleIntegerProperty();
    private final StringProperty highlightText = new SimpleStringProperty();
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);

    public DocumentViewerViewModel(StateManager stateManager, CliPreferences preferences) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.preferences = Objects.requireNonNull(preferences);

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
            currentDocument.set(null); // Clear current document when no entries
        } else {
            Set<LinkedFile> linkedFiles = entries.stream()
                                                 .map(BibEntry::getFiles)
                                                 .flatMap(List::stream)
                                                 .collect(Collectors.toSet());

            // Filter to include only PDF files
            Set<LinkedFile> pdfFiles = linkedFiles.stream()
                                                  .filter(this::isPdfFile)
                                                  .collect(Collectors.toSet());

            if (pdfFiles.isEmpty()) {
                // No PDF files found - clear the list and current document
                files.clear();
                currentDocument.set(null);
                // The UI will automatically close the dialog when files list is empty
                // This provides better UX than showing technical errors
            } else {
                // We have PDF files - display them in the dropdown
                files.setValue(FXCollections.observableArrayList(pdfFiles));
                // The first file will be automatically selected by the UI
            }
        }
    }

    private void setCurrentDocument(Path path) {
        if (FileUtil.isPDFFile(path)) {
            currentDocument.set(path);
        }
    }

    /**
     * Checks if the given LinkedFile is a PDF file.
     * Uses multiple strategies: file extension, file type, and FileUtil if available.
     *
     * @param file the LinkedFile to check
     * @return true if the file is a PDF, false otherwise
     */
    private boolean isPdfFile(LinkedFile file) {
        if (file == null || file.getLink() == null || file.getLink().trim().isEmpty()) {
            return false;
        }

        // Strategy 1: Check file extension
        String fileName = file.getLink().toLowerCase().trim();
        if (fileName.endsWith(".pdf")) {
            return true;
        }

        // Strategy 2: Check file type property if available
        String fileType = file.getFileType();
        if (fileType != null && fileType.equalsIgnoreCase("pdf")) {
            return true;
        }

        // Strategy 3: Use existing FileUtil if the file path can be resolved
        try {
            Path filePath = Path.of(file.getLink());
            return FileUtil.isPDFFile(filePath);
        } catch (Exception e) {
            // If path resolution fails, fall back to extension check
            return false;
        }
    }

    public void switchToFile(LinkedFile file) {
        if (file != null) {
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferences.getFilePreferences()))
                        .ifPresentOrElse(
                                this::setCurrentDocument,
                                () -> {
                                    // File not found or cannot be accessed - clear current document
                                    currentDocument.set(null);
                                    LOGGER.warn("Could not find or access file: {}", file.getLink());
                                }
                        );
        } else {
            // File is null - clear current document to ensure UI consistency
            currentDocument.set(null);
        }
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

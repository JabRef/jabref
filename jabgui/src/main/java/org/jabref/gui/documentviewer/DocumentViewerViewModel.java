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
    private final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);

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
            Set<LinkedFile> pdfFiles = entries.stream()
                                              .map(BibEntry::getFiles)
                                              .flatMap(List::stream)
                                              .filter(this::isPdfFile)  // 直接在这里过滤
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

    private boolean isPdfFile(LinkedFile file) {
        if (file == null || file.getLink() == null || file.getLink().trim().isEmpty()) {
            return false;
        }

        try {
            Path filePath = Path.of(file.getLink());
            return FileUtil.isPDFFile(filePath);
        } catch (Exception e) {
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

package org.jabref.gui.documentviewer;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class LiveDocumentViewerViewModel extends DocumentViewerViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(LiveDocumentViewerViewModel.class);

    private final StateManager stateManager;
    private final CliPreferences preferences;
    private final DialogService dialogService;

    private final ListProperty<LinkedFile> files = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty liveMode = new SimpleBooleanProperty(true);

    public LiveDocumentViewerViewModel(StateManager stateManager,
                                       CliPreferences preferences,
                                       DialogService dialogService) {
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
            if (!oldValue && newValue) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    public ListProperty<LinkedFile> filesProperty() {
        return files;
    }

    public BooleanProperty liveModeProperty() {
        return liveMode;
    }

    public void setLiveMode(boolean value) {
        this.liveMode.set(value);
    }

    public boolean isLiveMode() {
        return this.liveMode.get();
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            files.clear();
            showDocument(null);
        } else {
            Set<LinkedFile> pdfFiles = entries.stream()
                                              .map(BibEntry::getFiles)
                                              .flatMap(List::stream)
                                              .filter(this::isPdfFile)
                                              .collect(Collectors.toSet());

            if (pdfFiles.isEmpty()) {
                files.clear();
                showDocument(null);
                dialogService.notify(Localization.lang("No PDF files available"));
            } else {
                files.setValue(FXCollections.observableArrayList(pdfFiles));
            }
        }
    }

    private boolean isPdfFile(@Nullable LinkedFile file) {
        if (file == null || file.getLink() == null || file.getLink().isBlank()) {
            return false;
        }

        try {
            Path filePath = Path.of(file.getLink());
            return FileUtil.isPDFFile(filePath);
        } catch (InvalidPathException | SecurityException _) {
            return false;
        }
    }

    public void switchToFile(@Nullable LinkedFile file) {
        if (file != null) {
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferences.getFilePreferences()))
                        .ifPresentOrElse(
                                this::showDocument,
                                () -> {
                                    showDocument(null);
                                    LOGGER.warn("Could not find or access file: {}", file.getLink());
                                }
                        );
        } else {
            showDocument(null);
        }
    }
}

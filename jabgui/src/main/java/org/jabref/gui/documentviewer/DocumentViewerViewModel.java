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

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);

    private final StateManager stateManager;
    private final CliPreferences preferences;
    private final ObjectProperty<Path> currentDocument = new SimpleObjectProperty<>();
    private final ListProperty<LinkedFile> files = new SimpleListProperty<>();
    private final BooleanProperty liveMode = new SimpleBooleanProperty(true);
    private final IntegerProperty currentPage = new SimpleIntegerProperty();
    private final IntegerProperty maxPages = new SimpleIntegerProperty();

    public DocumentViewerViewModel(StateManager stateManager, CliPreferences preferences) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.preferences = Objects.requireNonNull(preferences);

        this.stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) c -> {
            // Switch to currently selected entry in live mode
            if (liveMode.get()) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        this.liveMode.addListener((observable, oldValue, newValue) -> {
            // Switch to currently selected entry if mode is changed to live
            if ((oldValue != newValue) && newValue) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    private int getCurrentPage() {
        return currentPage.get();
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

    private void setCurrentEntries(List<BibEntry> entries) {
        if (entries.isEmpty()) {
            files.clear();
        } else {
            Set<LinkedFile> linkedFiles = entries.stream().map(BibEntry::getFiles).flatMap(List::stream).collect(Collectors.toSet());
            // We don't need to switch to the first file, this is done automatically in the UI part
            files.setValue(FXCollections.observableArrayList(linkedFiles));
        }
    }

    private void setCurrentDocument(Path path) {
        if (FileUtil.isPDFFile(path)) {
            currentDocument.set(path);
        }
    }

    public void switchToFile(LinkedFile file) {
        if (file != null) {
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferences.getFilePreferences()))
                        .ifPresent(this::setCurrentDocument);
            currentPage.set(1);
        }
    }

    public void showPage(int pageNumber) {
        if (pageNumber >= 1 && pageNumber <= maxPages.get()) {
            currentPage.set(pageNumber - 1); // we have to substract one page
        } else {
            currentPage.set(1);
        }
    }

    public void setLiveMode(boolean value) {
        this.liveMode.set(value);
    }
}

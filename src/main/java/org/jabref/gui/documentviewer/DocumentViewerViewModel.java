package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.application.Platform;
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
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentViewerViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);

    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final ObjectProperty<DocumentViewModel> currentDocument = new SimpleObjectProperty<>();
    private final ListProperty<LinkedFile> files = new SimpleListProperty<>();
    private final BooleanProperty liveMode = new SimpleBooleanProperty();
    private final ObjectProperty<Integer> currentPage = new SimpleObjectProperty<>();
    private final IntegerProperty maxPages = new SimpleIntegerProperty();

    public DocumentViewerViewModel(StateManager stateManager, PreferencesService preferencesService) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.preferencesService = Objects.requireNonNull(preferencesService);

        this.stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) c -> {
            // Switch to currently selected entry in live mode
            if (isLiveMode()) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        this.liveMode.addListener((observable, oldValue, newValue) -> {
            // Switch to currently selected entry if mode is changed to live
            if ((oldValue != newValue) && newValue) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        // we need to wrap this in run later so that the max pages number is correctly shown
        Platform.runLater(() -> maxPages.bindBidirectional(
            EasyBind.wrapNullable(currentDocument).selectProperty(DocumentViewModel::maxPagesProperty)));
        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    private int getCurrentPage() {
        return currentPage.get();
    }

    public ObjectProperty<Integer> currentPageProperty() {
        return currentPage;
    }

    public IntegerProperty maxPagesProperty() {
        return maxPages;
    }

    private boolean isLiveMode() {
        return liveMode.get();
    }

    public ObjectProperty<DocumentViewModel> currentDocumentProperty() {
        return currentDocument;
    }

    public ListProperty<LinkedFile> filesProperty() {
        return files;
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (!entries.isEmpty()) {
            BibEntry firstSelectedEntry = entries.get(0);
            setCurrentEntry(firstSelectedEntry);
        }
    }

    private void setCurrentEntry(BibEntry entry) {
        stateManager.getActiveDatabase().ifPresent(database -> {
            List<LinkedFile> linkedFiles = entry.getFiles();
            // We don't need to switch to the first file, this is done automatically in the UI part
            files.setValue(FXCollections.observableArrayList(linkedFiles));
        });
    }

    private void setCurrentDocument(Path path) {
        try {
            currentDocument.set(new PdfDocumentViewModel(PDDocument.load(path.toFile())));
        } catch (IOException e) {
            LOGGER.error("Could not set Document Viewer", e);
        }
    }

    public void switchToFile(LinkedFile file) {
        if (file != null) {
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferencesService.getFilePreferences()))
                        .ifPresent(this::setCurrentDocument);
        }
    }

    public BooleanProperty liveModeProperty() {
        return liveMode;
    }

    public void showPage(int pageNumber) {
        currentPage.set(pageNumber);
    }

    public void showNextPage() {
        currentPage.set(getCurrentPage() + 1);
    }

    public void showPreviousPage() {
        currentPage.set(getCurrentPage() - 1);
    }
}

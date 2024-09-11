package org.jabref.gui.documentviewer;

import java.io.IOException;
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
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentViewerViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentViewerViewModel.class);

    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final ObjectProperty<DocumentViewModel> currentDocument = new SimpleObjectProperty<>();
    private final ListProperty<LinkedFile> files = new SimpleListProperty<>();
    private final BooleanProperty liveMode = new SimpleBooleanProperty(true);
    private final ObjectProperty<Integer> currentPage = new SimpleObjectProperty<>();
    private final IntegerProperty maxPages = new SimpleIntegerProperty();

    public DocumentViewerViewModel(StateManager stateManager, PreferencesService preferencesService) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.preferencesService = Objects.requireNonNull(preferencesService);

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

        // we need to wrap this in run later so that the max pages number is correctly shown
        UiTaskExecutor.runInJavaFXThread(() -> maxPages.bind(
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

    public ObjectProperty<DocumentViewModel> currentDocumentProperty() {
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
        try {
            if (FileUtil.isPDFFile(path)) {
                PDDocument document = Loader.loadPDF(path.toFile());
                currentDocument.set(new PdfDocumentViewModel(document));
            }
        } catch (IOException e) {
            LOGGER.error("Could not set Document Viewer for path {}", path, e);
        }
    }

    public void switchToFile(LinkedFile file) {
        if (file != null) {
            stateManager.getActiveDatabase()
                        .flatMap(database -> file.findIn(database, preferencesService.getFilePreferences()))
                        .ifPresent(this::setCurrentDocument);
            currentPage.set(1);
        }
    }

    public void showPage(int pageNumber) {
        if (pageNumber >= 1 && pageNumber <= maxPages.get()) {
            currentPage.set(pageNumber);
        } else {
            currentPage.set(1);
        }
    }

    public void showNextPage() {
        if (getCurrentPage() < maxPages.get()) {
            currentPage.set(getCurrentPage() + 1);
        }
    }

    public void showPreviousPage() {
        if (getCurrentPage() > 1) {
            currentPage.set(getCurrentPage() - 1);
        }
    }

    public void setLiveMode(boolean value) {
        this.liveMode.set(value);
    }
}

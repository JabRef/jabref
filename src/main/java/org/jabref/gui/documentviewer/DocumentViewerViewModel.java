package org.jabref.gui.documentviewer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.TypedBibEntry;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;

import org.apache.pdfbox.pdmodel.PDDocument;

public class DocumentViewerViewModel extends AbstractViewModel {

    private StateManager stateManager;
    private ObjectProperty<DocumentViewModel> currentDocument = new SimpleObjectProperty<>();
    private ListProperty<ParsedFileField> files = new SimpleListProperty<>();
    private BooleanProperty liveMode = new SimpleBooleanProperty();

    public DocumentViewerViewModel(StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);

        this.stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) c -> {
            // Switch to currently selected entry in live mode
            if (isLiveMode()) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        this.liveMode.addListener((observable, oldValue, newValue) -> {
            // Switch to currently selected entry if mode is changed to live
            if (newValue) {
                setCurrentEntries(this.stateManager.getSelectedEntries());
            }
        });

        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    public boolean isLiveMode() {
        return liveMode.get();
    }

    public ObjectProperty<DocumentViewModel> currentDocumentProperty() {
        return currentDocument;
    }

    public ListProperty<ParsedFileField> filesProperty() {
        return files;
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (!entries.isEmpty()) {
            BibEntry firstSelectedEntry = entries.get(0);
            setCurrentEntry(firstSelectedEntry);
        }
    }

    private void setCurrentEntry(BibEntry rawEntry) {
        stateManager.getActiveDatabase().ifPresent(database -> {
            TypedBibEntry entry = new TypedBibEntry(rawEntry, database);
            List<ParsedFileField> linkedFiles = entry.getFiles();
            // We don't need to switch to the first file, this is done automatically in the UI part
            files.setValue(FXCollections.observableArrayList(linkedFiles));
        });
    }

    private void setCurrentDocument(Path path) {
        try {
            currentDocument.set(new PdfDocumentViewModel(PDDocument.load(path.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void switchToFile(ParsedFileField file) {
        if (file != null) {
            stateManager.getActiveDatabase().ifPresent(database ->
                    file.toPath(database, Globals.prefs.getFileDirectoryPreferences())
                            .ifPresent(this::setCurrentDocument));
        }
    }

    public BooleanProperty liveModeProperty() {
        return liveMode;
    }
}

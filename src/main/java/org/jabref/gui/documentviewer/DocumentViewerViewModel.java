package org.jabref.gui.documentviewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.StateManager;
import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;

import org.apache.pdfbox.pdmodel.PDDocument;

public class DocumentViewerViewModel extends AbstractViewModel {

    private StateManager stateManager;
    private ObjectProperty<DocumentViewModel> currentDocument = new SimpleObjectProperty<>();
    private ListProperty<String> files = new SimpleListProperty<>();

    public DocumentViewerViewModel(StateManager stateManager) {
        this.stateManager = Objects.requireNonNull(stateManager);

        this.stateManager.getSelectedEntries().addListener((ListChangeListener<? super BibEntry>) c -> setCurrentEntries(this.stateManager.getSelectedEntries()));
        setCurrentEntries(this.stateManager.getSelectedEntries());
    }

    ;

    public ObjectProperty<DocumentViewModel> currentDocumentProperty() {
        return currentDocument;
    }

    public ListProperty<String> filesProperty() {
        return files;
    }

    private void setCurrentEntries(List<BibEntry> entries) {
        if (!entries.isEmpty()) {
            BibEntry firstSelectedEntry = entries.get(0);
            setCurrentEntry(firstSelectedEntry);
        }
    }

    private void setCurrentEntry(BibEntry rawEntry) {
        BibDatabaseContext databaseContext = stateManager.activeDatabaseProperty().get().get();
        TypedBibEntry entry = new TypedBibEntry(rawEntry, databaseContext);
        List<ParsedFileField> linkedFiles = entry.getFiles();
        for (ParsedFileField linkedFile : linkedFiles) {
            // TODO: Find a better way to get the open database
            // TODO: It should be possible to simply write linkedFile.getFile()
            Optional<File> file = FileUtil.expandFilename(
                    databaseContext, linkedFile.getLink(), Globals.prefs.getFileDirectoryPreferences());
            if (file.isPresent()) {
                setCurrentDocument(file.get().toPath());
            }
        }

        files.setValue(
                FXCollections.observableArrayList(
                        linkedFiles.stream().map(ParsedFileField::getLink).collect(Collectors.toList())));
    }

    private void setCurrentDocument(Path path) {
        try {
            currentDocument.set(new PdfDocumentViewModel(PDDocument.load(path.toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

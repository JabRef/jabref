package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibliographyFromPdfImporter;
import org.jabref.preferences.PreferencesService;

public class NewLibraryFromPdfActionOffline extends NewLibraryFromPdfAction {

    private final BibliographyFromPdfImporter bibliographyFromPdfImporter;

    public NewLibraryFromPdfActionOffline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, PreferencesService preferencesService, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferencesService, taskExecutor);

        // Use the importer keeping the numbers (instead of generating keys; which is the other constructor)
        this.bibliographyFromPdfImporter = new BibliographyFromPdfImporter();
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> bibliographyFromPdfImporter.importDatabase(path);
    }
}

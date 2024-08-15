package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.preferences.PreferencesService;

public class NewLibraryFromPdfActionOnline extends NewLibraryFromPdfAction {

    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, PreferencesService preferencesService, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferencesService, taskExecutor);
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> new ParserResult(
                new GrobidService(this.preferencesService.getGrobidPreferences()).processReferences(path, preferencesService.getImportFormatPreferences()));
    }
}

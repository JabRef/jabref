package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;

public class NewLibraryFromPdfActionOnline extends NewLibraryFromPdfAction {

    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor);
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> new ParserResult(
                new GrobidService(this.preferences.getGrobidPreferences()).processReferences(path, preferences.getImportFormatPreferences()));
    }
}

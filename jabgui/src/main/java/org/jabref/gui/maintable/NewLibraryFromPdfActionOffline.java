package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;

public class NewLibraryFromPdfActionOffline extends NewLibraryFromPdfAction {

    private final RuleBasedBibliographyPdfImporter ruleBasedBibliographyPdfImporter;

    public NewLibraryFromPdfActionOffline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor);

        // Use the importer keeping the numbers (instead of generating keys; which is the other constructor)
        this.ruleBasedBibliographyPdfImporter = new RuleBasedBibliographyPdfImporter();
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> ruleBasedBibliographyPdfImporter.importDatabase(path);
    }
}

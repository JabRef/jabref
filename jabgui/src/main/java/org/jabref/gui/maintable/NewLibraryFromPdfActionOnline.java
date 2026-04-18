package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.CitationsFromPdf;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

public class NewLibraryFromPdfActionOnline extends NewLibraryFromPdfAction {

    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor, AiService aiService) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor, aiService);
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> {
            List<BibEntry> entries = new GrobidService(this.preferences.getGrobidPreferences()).processReferences(path, preferences.getImportFormatPreferences());
            if (entries.isEmpty()) {
                entries = CitationsFromPdf.extractCitationsUsingLLM(this.aiService, this.preferences.getImportFormatPreferences(), path).getDatabase().getEntries();
            }
            return new ParserResult(entries);
        };
    }
}


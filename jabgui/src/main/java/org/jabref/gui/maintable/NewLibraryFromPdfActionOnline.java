package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.CitationsFromPdf;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewLibraryFromPdfActionOnline extends NewLibraryFromPdfAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewLibraryFromPdfActionOnline.class);

    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor);
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> {
            List<BibEntry> entries = new GrobidService(this.preferences.getGrobidPreferences()).processReferences(path, preferences.getImportFormatPreferences());
            if (entries.isEmpty()) {
                entries = CitationsFromPdf.extractCitationsUsingLLM(this.preferences, LOGGER::info, path).getDatabase().getEntries();
            }
            return new ParserResult(entries);
        };
    }
}


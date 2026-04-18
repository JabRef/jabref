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

import jakarta.ws.rs.HEAD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewLibraryFromPdfActionOnline extends NewLibraryFromPdfAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewLibraryFromPdfActionOnline.class);

<<<<<<< HEAD
    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor, AiService aiService) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor, aiService);
=======
    public NewLibraryFromPdfActionOnline(LibraryTabContainer libraryTabContainer, StateManager stateManager, DialogService dialogService, CliPreferences preferences, TaskExecutor taskExecutor) {
        super(libraryTabContainer, stateManager, dialogService, preferences, taskExecutor);
>>>>>>> f362373cef372f3280cb57aa433998bc17fc53bf
    }

    @Override
    protected Callable<ParserResult> getParserResultCallable(Path path) {
        return () -> {
<<<<<<<HEAD
            List<BibEntry> entries;
            try {
                entries = new GrobidService(this.preferences.getGrobidPreferences())
                        .processReferences(path, preferences.getImportFormatPreferences());
            } catch (Exception e) {
                LOGGER.warn("Grobid failed, falling back to LLM", e);
                entries = List.of();
            }
            if (entries.isEmpty()) {
                entries = CitationsFromPdf.extractCitationsUsingLLM(
                        this.aiService, this.preferences.getImportFormatPreferences(), path).getDatabase().getEntries();
=======
            List<BibEntry> entries = new GrobidService(this.preferences.getGrobidPreferences()).processReferences(path, preferences.getImportFormatPreferences());
            if (entries.isEmpty()) {
                entries = CitationsFromPdf.extractCitationsUsingLLM(this.preferences, LOGGER::info, path).getDatabase().getEntries();
>>>>>>> f362373cef372f3280cb57aa433998bc17fc53bf
            }
            return new ParserResult(entries);
        };
    }
}


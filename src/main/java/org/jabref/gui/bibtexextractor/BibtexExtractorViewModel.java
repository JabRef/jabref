package org.jabref.gui.bibtexextractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.DialogService;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.JabRefPreferences;

public class BibtexExtractorViewModel {

    private final StringProperty inputTextProperty = new SimpleStringProperty("");
    private final BibDatabaseContext bibdatabaseContext;
    private List<BibEntry> extractedEntries;
    private final BibDatabaseContext newDatabaseContext;
    private boolean directAdd;
    private DialogService dialogService;
    private GrobidCitationFetcher currentCitationfetcher;

    public BibtexExtractorViewModel(BibDatabaseContext bibdatabaseContext) {
        this.bibdatabaseContext = bibdatabaseContext;
        newDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        dialogService = JabRefGUI.getMainFrame().getDialogService();
    }

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }

    public void startParsing(boolean directAdd) {
        this.directAdd = directAdd;
        this.extractedEntries = null;
        Task<Void> parseUsingGrobid = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    currentCitationfetcher = new GrobidCitationFetcher(
                        JabRefPreferences.getInstance().getImportFormatPreferences(),
                        Globals.getFileUpdateMonitor(),
                        Globals.prefs
                    );
                    extractedEntries = currentCitationfetcher.performSearch(inputTextProperty.getValue());
                } catch (FetcherException e) {
                    extractedEntries = Collections.emptyList();
                }
                Platform.runLater(() -> executeParse());
                return null;
            }
        };
        dialogService.showProgressDialogAndWait(Localization.lang("Your text is being parsed.."),Localization.lang( "Please wait while we are parsing your text"), parseUsingGrobid);
        Globals.TASK_EXECUTOR.execute(parseUsingGrobid);
    }

    public void executeParse() {
        if (!extractedEntries.isEmpty()) {
            if (directAdd) {
                BibtexKeyGenerator bibtexKeyGenerator = new BibtexKeyGenerator(newDatabaseContext, Globals.prefs.getBibtexKeyPatternPreferences());
                for (BibEntry bibEntries: extractedEntries) {
                    bibtexKeyGenerator.generateAndSetKey(bibEntries);
                    newDatabaseContext.getDatabase().insertEntry(bibEntries);
                }
                newDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
                JabRefGUI.getMainFrame().addTab(newDatabaseContext,true);
            } else {
                for (BibEntry bibEntry: extractedEntries) {
                    this.bibdatabaseContext.getDatabase().insertEntry(bibEntry);
                    JabRefGUI.getMainFrame().getCurrentBasePanel().showAndEdit(bibEntry);
                    trackNewEntry(StandardEntryType.Article);
                }
            }
            if (currentCitationfetcher.getFailedEntries().size() > 0) {
                dialogService.showWarningDialogAndWait(Localization.lang("Grobid failed to parse the following entries:"), String.join("\n;;\n", currentCitationfetcher.getFailedEntries()));
            }
        }
        dialogService.notify(Localization.lang("Successfully added a new entry."));
    }

    public void startExtraction() {
        BibtexExtractor extractor = new BibtexExtractor();
        BibEntry entity = extractor.extract(inputTextProperty.getValue());
        this.bibdatabaseContext.getDatabase().insertEntry(entity);
        trackNewEntry(StandardEntryType.Article);
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}

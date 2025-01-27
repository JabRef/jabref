package org.jabref.gui.consistency;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    private final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckDialog.class);

    public ConsistencyCheckAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        List<BibEntry> entries = database.getDatabase().getEntries();

        /* BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(entries);

        String homeDir = System.getProperty("user.home");
        Path tempDir = Paths.get(homeDir, "downloads");
        Path txtFile = tempDir.resolve("consistency-check.txt");

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile))) {
            BibliographyConsistencyCheckResultTxtWriter bibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer);
            bibliographyConsistencyCheckResultTxtWriter.writeFindings();
            dialogService.notify("File created!");
        } catch (IOException e) {
            dialogService.notify("Exception occured!");
        }*/

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(entries);

        Map<EntryType, BibliographyConsistencyCheck.EntryTypeResult> entryTypeToResultMap = result.entryTypeToResultMap();

        entryTypeToResultMap.forEach((entryType, entryTypeResult) -> {
            LOGGER.info("Entry Type: " + entryType);

            Collection<Field> uniqueFields = entryTypeResult.fields();
            LOGGER.info("Unique Fields: " + uniqueFields);

            Collection<BibEntry> sortedEntries = entryTypeResult.sortedEntries();
            LOGGER.info("Sorted Entries: " + sortedEntries);
        });

        dialogService.showCustomDialogAndWait(new ConsistencyCheckDialog());
    }
}

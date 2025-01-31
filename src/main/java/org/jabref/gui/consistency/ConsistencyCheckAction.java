package org.jabref.gui.consistency;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ConsistencyCheckAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    private final List<String> entryTypes = new ArrayList<>();
    private final List<String> citationKeys = new ArrayList<>();
    private final List<String> columns = new ArrayList<>();

    public ConsistencyCheckAction(DialogService dialogService, StateManager stateManager, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        List<BibEntry> entries = databaseContext.getDatabase().getEntries();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(entries);

        result.entryTypeToResultMap().forEach((entrySet, entryTypeResult) -> {
            entryTypes.add(entrySet.toString());

            for (BibEntry entry: entryTypeResult.sortedEntries()) {
                citationKeys.add(entry.getCitationKey().get());
            }

            for (Field field: entryTypeResult.fields()) {
                columns.add(field.toString());
            }
        });

        dialogService.showCustomDialogAndWait(new ConsistencyCheckDialog(result, entryTypes, columns, citationKeys, dialogService, preferences));
    }
}

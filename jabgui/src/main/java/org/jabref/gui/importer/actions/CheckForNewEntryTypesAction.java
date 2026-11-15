package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.ImportCustomEntryTypesDialog;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.CustomEntryTypeDecision;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

/// This action checks whether any new custom entry types were loaded from this
/// BIB file. If so, an offer to remember these entry types is given.
public class CheckForNewEntryTypesAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        return !getListOfUnknownAndUnequalCustomizations(parserResult, preferences).isEmpty();
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult, preferences);
        dialogService.showCustomDialogAndWait(new ImportCustomEntryTypesDialog(mode, getListOfUnknownAndUnequalCustomizations(parserResult, preferences)));
    }

    private List<BibEntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult, CliPreferences preferences) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult, preferences);
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        Set<String> declinedDecisions = preferences.getDeclinedCustomEntryTypes();

        return parserResult.getEntryTypes()
                           .stream()
                           .filter(type -> entryTypesManager.isDifferentCustomOrModifiedType(type, mode))
                           .filter(type -> !declinedDecisions.contains(CustomEntryTypeDecision.fingerprint(type, entryTypesManager.enrich(type.getType(), mode), mode)))
                           .toList();
    }

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult, CliPreferences preferences) {
        return parserResult.getMetaData().getMode().orElse(preferences.getLibraryPreferences().getDefaultBibDatabaseMode());
    }
}

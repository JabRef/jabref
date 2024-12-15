package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.ImportCustomEntryTypesDialog;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

/**
 * This action checks whether any new custom entry types were loaded from this
 * BIB file. If so, an offer to remember these entry types is given.
 */
public class CheckForNewEntryTypesAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        return !getListOfUnknownAndUnequalCustomizations(parserResult, preferences.getLibraryPreferences()).isEmpty();
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferencesService) {
        LibraryPreferences preferences = preferencesService.getLibraryPreferences();
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult, preferences);
        dialogService.showCustomDialogAndWait(new ImportCustomEntryTypesDialog(mode, getListOfUnknownAndUnequalCustomizations(parserResult, preferences)));
    }

    private List<BibEntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult, LibraryPreferences preferences) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult, preferences);
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);

        return parserResult.getEntryTypes()
                           .stream()
                           .filter(type -> entryTypesManager.isDifferentCustomOrModifiedType(type, mode))
                           .collect(Collectors.toList());
    }

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult, LibraryPreferences preferences) {
        return parserResult.getMetaData().getMode().orElse(preferences.getDefaultBibDatabaseMode());
    }
}

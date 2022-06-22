package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.importer.ImportCustomEntryTypesDialog;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;

import com.airhacks.afterburner.injection.Injector;

/**
 * This action checks whether any new custom entry types were loaded from this
 * BIB file. If so, an offer to remember these entry types is given.
 */
public class CheckForNewEntryTypesAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult) {
        return !getListOfUnknownAndUnequalCustomizations(parserResult).isEmpty();
    }

    @Override
    public void performAction(LibraryTab libraryTab, ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new ImportCustomEntryTypesDialog(mode, getListOfUnknownAndUnequalCustomizations(parserResult)));
    }

    private List<BibEntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        return parserResult.getEntryTypes()
                           .stream()
                           .filter(type -> Globals.entryTypesManager.isDifferentCustomOrModifiedType(type, mode))
                           .collect(Collectors.toList());
    }

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult) {
        return parserResult.getMetaData().getMode().orElse(Globals.prefs.getGeneralPreferences().getDefaultBibDatabaseMode());
    }
}

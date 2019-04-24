package org.jabref.gui.importer.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.importer.ImportCustomEntryTypesDialog;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.EntryType;

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
    public void performAction(BasePanel panel, ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        ImportCustomEntryTypesDialog importCustomEntryTypesDialog = new ImportCustomEntryTypesDialog(mode, getListOfUnknownAndUnequalCustomizations(parserResult));
        importCustomEntryTypesDialog.showAndWait();

    }

    private List<EntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        return parserResult.getEntryTypes().values().stream()
                           .filter(type -> (!EntryTypes.getType(type.getName(), mode).isPresent())
                                           || !EntryTypes.isEqualNameAndFieldBased(type, EntryTypes.getType(type.getName(), mode).get()))
                           .collect(Collectors.toList());
    }

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult) {
        return parserResult.getMetaData().getMode().orElse(Globals.prefs.getDefaultBibDatabaseMode());
    }
}

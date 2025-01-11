package org.jabref.gui.edit;

import java.util.List;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyTo extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTo.class);
    private final StandardActions action;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ClipBoardManager clipBoardManager;
    private final GuiPreferences preferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final List<String> checkedPaths;
    private final String path;

    public CopyTo(StandardActions action,
                  DialogService dialogService,
                  StateManager stateManager,
                  ClipBoardManager clipBoardManager,
                  GuiPreferences preferences,
                  JournalAbbreviationRepository abbreviationRepository,
                  List<String> checkedPaths,
                  String path) {
        this.action = action;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.clipBoardManager = clipBoardManager;
        this.preferences = preferences;
        this.abbreviationRepository = abbreviationRepository;
        this.checkedPaths = checkedPaths;
        this.path = path;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        Logger logger = LoggerFactory.getLogger(CopyTo.class);

        BibDatabaseContext databaseContext = new BibDatabaseContext();

        List<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        List<String> titles = selectedEntries.stream()
                .filter(entry -> entry.getTitle().isPresent())
                .map(entry -> entry.getTitle().get())
                .toList();

        for (String title: titles) {
            logger.info("Selected Entreis: " + title);
        }
        for (String checkedPath: checkedPaths) {
            logger.info("Selected Libraries to copy: " + checkedPath);
        }

        boolean includeCrossReferences = askForCrossReferencedEntries();
        preferences.getCopyToPreferences().setShouldIncludeCrossReferences(includeCrossReferences);

        if (preferences.getCopyToPreferences().getShouldIncludeCrossReferences()) {
            logger.info("Include Cross References");
        } else {
            logger.info("Exclude Cross References");
        }
    }

    private boolean askForCrossReferencedEntries() {
        if (preferences.getCopyToPreferences().getShouldAskForIncludingCrossReferences()) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Include or exclude cross-referenced entries"),
                    Localization.lang("Would you like to include cross-reference entries in the current operation?"),
                    Localization.lang("Include"),
                    Localization.lang("Exclude"),
                    Localization.lang("Do not ask again"),
                    optOut -> preferences.getCopyToPreferences().setShouldAskForIncludingCrossReferences(!optOut)
            );
        } else {
            return preferences.getCopyToPreferences().getShouldIncludeCrossReferences();
        }
    }
}

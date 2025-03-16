package org.jabref.gui.journals;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);

    private final StandardActions action;
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;

    private AbbreviationType abbreviationType;

    public AbbreviateAction(StandardActions action,
                            Supplier<LibraryTab> tabSupplier,
                            DialogService dialogService,
                            StateManager stateManager,
                            JournalAbbreviationPreferences abbreviationPreferences,
                            JournalAbbreviationRepository abbreviationRepository,
                            TaskExecutor taskExecutor,
                            UndoManager undoManager) {
        this.action = action;
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.journalAbbreviationPreferences = abbreviationPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;

        switch (action) {
            case ABBREVIATE_DEFAULT -> abbreviationType = AbbreviationType.DEFAULT;
            case ABBREVIATE_DOTLESS -> abbreviationType = AbbreviationType.DOTLESS;
            case ABBREVIATE_SHORTEST_UNIQUE -> abbreviationType = AbbreviationType.SHORTEST_UNIQUE;
            default -> LOGGER.debug("Unknown action: {}", action.name());
        }

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        if ((action == StandardActions.ABBREVIATE_DEFAULT)
                || (action == StandardActions.ABBREVIATE_DOTLESS)
                || (action == StandardActions.ABBREVIATE_SHORTEST_UNIQUE)) {
            dialogService.notify(Localization.lang("Abbreviating..."));
            stateManager.getActiveDatabase().ifPresent(_ ->
                    BackgroundTask.wrap(() -> abbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(taskExecutor));
        } else if (action == StandardActions.UNABBREVIATE) {
            dialogService.notify(Localization.lang("Unabbreviating..."));
            stateManager.getActiveDatabase().ifPresent(_ ->
                    BackgroundTask.wrap(() -> unabbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(taskExecutor));
        } else {
            LOGGER.debug("Unknown action: {}", action.name());
        }
    }

    private String abbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(
                abbreviationRepository,
                abbreviationType,
                journalAbbreviationPreferences.shouldUseFJournalField());

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));

        int count = entries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.abbreviate(databaseContext.getDatabase(), entry, journalField, ce)).count()).sum();

        if (count == 0) {
            return Localization.lang("No journal names could be abbreviated.");
        }

        ce.end();
        undoManager.addEdit(ce);
        tabSupplier.get().markBaseChanged();
        return Localization.lang("Abbreviated %0 journal names.", String.valueOf(count));
    }

    private String unabbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(abbreviationRepository);

        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = entries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.unabbreviate(databaseContext.getDatabase(), entry, journalField, ce)).count()).sum();
        if (count == 0) {
            return Localization.lang("No journal names could be unabbreviated.");
        }

        ce.end();
        undoManager.addEdit(ce);
        tabSupplier.get().markBaseChanged();
        return Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
    }
}

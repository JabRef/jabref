package org.jabref.gui.push;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * An Action class representing the process of invoking a PushToApplication operation.
 */
public class PushToApplicationAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;

    private PushToApplication application;

    public PushToApplicationAction(PushToApplication application, StateManager stateManager, DialogService dialogService) {
        this.application = application;
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(this.executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    public void updateApplication(PushToApplication application) {
        this.application = Objects.requireNonNull(application);
    }

    public Action getActionInformation() {
        return new Action() {
            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(application.getIcon());
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
            }

            @Override
            public String getText() {
                return Localization.lang("Push entries to external application (%0)", application.getDisplayName());
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }

    private static String getKeyString(List<BibEntry> entries) {
        StringBuilder result = new StringBuilder();
        Optional<String> citeKey;
        boolean first = true;
        for (BibEntry bes : entries) {
            citeKey = bes.getCitationKey();
            if (citeKey.isEmpty() || citeKey.get().isEmpty()) {
                // Should never occur, because we made sure that all entries have keys
                continue;
            }
            if (first) {
                result.append(citeKey.get());
                first = false;
            } else {
                result.append(',').append(citeKey.get());
            }
        }
        return result.toString();
    }

    @Override
    public void execute() {
        // If required, check that all entries have citation keys defined:
        if (application.requiresCitationKeys()) {
            for (BibEntry entry : stateManager.getSelectedEntries()) {
                if (StringUtil.isBlank(entry.getCitationKey())) {
                    dialogService.showErrorDialogAndWait(
                            application.getDisplayName(),
                            Localization.lang("This operation requires all selected entries to have citation keys defined."));

                    return;
                }
            }
        }

        // All set, call the operation in a new thread:
        BackgroundTask.wrap(this::pushEntries)
                      .onSuccess(s -> application.operationCompleted())
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private void pushEntries() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        application.pushEntries(database, stateManager.getSelectedEntries(), getKeyString(stateManager.getSelectedEntries()));
    }
}

package org.jabref.gui.push;

import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
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
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * An Action class representing the process of invoking a PushToApplication operation.
 */
public class PushToApplicationAction extends SimpleCommand {

    private final PushToApplication operation;
    private final JabRefFrame frame;
    private final StateManager stateManager;
    private BasePanel panel;

    public PushToApplicationAction(final JabRefFrame frame, final StateManager stateManager) {
        this.frame = frame;
        this.operation = getLastUsedApplication(frame.getPushApplications().getApplications());
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(this.executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    private PushToApplication getLastUsedApplication(List<PushToApplication> pushActions) {
        String appSelected = Globals.prefs.get(JabRefPreferences.PUSH_TO_APPLICATION);
        for (PushToApplication application : pushActions) {
            if (application.getApplicationName().equals(appSelected)) {
                return application;
            }
        }

        // Nothing found, pick first
        return pushActions.get(0);
    }

    public Action getActionInformation() {
        return new Action() {

            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(operation.getIcon());
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
            }

            @Override
            public String getText() {
                return Localization.lang("Push entries to external application (%0)", operation.getApplicationName());
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
            citeKey = bes.getCiteKeyOptional();
            if (!(citeKey.isPresent()) || citeKey.get().isEmpty()) {
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
        panel = frame.getCurrentBasePanel();

        // If required, check that all entries have BibTeX keys defined:
        if (operation.requiresBibtexKeys()) {
            for (BibEntry entry : stateManager.getSelectedEntries()) {
                if (!(entry.getCiteKeyOptional().isPresent()) || entry.getCiteKeyOptional().get().trim().isEmpty()) {
                    frame.getDialogService().showErrorDialogAndWait(operation.getApplicationName(),
                                                                    Localization.lang("This operation requires all selected entries to have BibTeX keys defined."));

                    return;
                }
            }
        }

        // All set, call the operation in a new thread:
        BackgroundTask.wrap(this::pushEntries)
                      .onSuccess(s -> operation.operationCompleted(panel))
                      .executeWith(Globals.TASK_EXECUTOR);

    }

    private void pushEntries() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        operation.pushEntries(database, stateManager.getSelectedEntries(), getKeyString(stateManager.getSelectedEntries()));
    }
}

package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * An Action class representing the process of invoking a PushToApplication operation.
 */
public class PushToApplicationCommand extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToApplicationCommand.class);

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final List<Object> reconfigurableControls = new ArrayList<>();
    private final TaskExecutor taskExecutor;

    private PushToApplication application;

    public PushToApplicationCommand(StateManager stateManager, DialogService dialogService, GuiPreferences preferences, TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        setApplication(preferences.getPushToApplicationPreferences()
                                  .getActiveApplicationName());

        EasyBind.subscribe(preferences.getPushToApplicationPreferences().activeApplicationNameProperty(),
                this::setApplication);

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(
                this.executable,
                "",
                Localization.lang("This operation requires one or more entries to be selected.")));
    }

    public void registerReconfigurable(Object node) {
        if (!(node instanceof MenuItem) && !(node instanceof ButtonBase)) {
            LOGGER.error("Node must be either a MenuItem or a ButtonBase");
            return;
        }

        this.reconfigurableControls.add(node);
    }

    private void setApplication(String applicationName) {
        final ActionFactory factory = new ActionFactory();
        PushToApplication application = PushToApplications.getApplicationByName(
                                                                  applicationName,
                                                                  dialogService,
                                                                  preferences)
                                                          .orElse(new PushToEmacs(dialogService, preferences));

        preferences.getPushToApplicationPreferences().setActiveApplicationName(application.getDisplayName());
        this.application = Objects.requireNonNull(application);

        reconfigurableControls.forEach(object -> {
            if (object instanceof MenuItem item) {
                factory.configureMenuItem(application.getAction(), this, item);
            } else if (object instanceof ButtonBase base) {
                factory.configureIconButton(application.getAction(), this, base);
            }
        });
    }

    public Action getAction() {
        return application.getAction();
    }

    private static String getKeyString(List<BibEntry> entries, String delimiter) {
        StringBuilder result = new StringBuilder();
        Optional<String> citeKey;
        boolean first = true;
        for (BibEntry bes : entries) {
            citeKey = bes.getCitationKey();
            if (citeKey.isEmpty() || citeKey.get().isEmpty()) {
                LOGGER.warn("Should never occur, because we made sure that all entries have keys");
                continue;
            }
            if (first) {
                result.append(citeKey.get());
                first = false;
            } else {
                result.append(delimiter).append(citeKey.get());
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
                      .onSuccess(s -> application.onOperationCompleted())
                      .onFailure(ex -> LOGGER.error("Error pushing citation", ex))
                      .executeWith(taskExecutor);
    }

    private void pushEntries() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        application.pushEntries(database, stateManager.getSelectedEntries(), getKeyString(stateManager.getSelectedEntries(), application.getDelimiter()));
    }
}

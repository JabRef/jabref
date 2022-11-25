package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.InternalPreferences;

public class SearchForUpdateAction extends SimpleCommand {

    private final BuildInfo buildInfo;
    private final InternalPreferences internalPreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public SearchForUpdateAction(BuildInfo buildInfo, InternalPreferences internalPreferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.buildInfo = buildInfo;
        this.internalPreferences = internalPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        new VersionWorker(buildInfo.version, dialogService, taskExecutor, internalPreferences)
                .checkForNewVersionAsync();
    }
}

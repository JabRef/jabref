package org.jabref.gui.actions;

import org.jabref.gui.DialogService;
import org.jabref.gui.help.VersionWorker;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.VersionPreferences;

public class SearchForUpdateAction extends SimpleCommand {

    private final BuildInfo buildInfo;
    private final VersionPreferences versionPreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public SearchForUpdateAction(BuildInfo buildInfo, VersionPreferences versionPreferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.buildInfo = buildInfo;
        this.versionPreferences = versionPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        new VersionWorker(buildInfo.getVersion(), versionPreferences.getIgnoredVersion(), dialogService, taskExecutor)
                .checkForNewVersionAsync(true);
    }
}

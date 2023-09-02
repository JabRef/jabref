package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.PreferencesService;

public class SearchForUpdateAction extends SimpleCommand {

    private final BuildInfo buildInfo;
    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public SearchForUpdateAction(BuildInfo buildInfo,
                                 PreferencesService preferencesService,
                                 DialogService dialogService,
                                 TaskExecutor taskExecutor) {
        this.buildInfo = buildInfo;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(preferencesService.getInternalPreferences().versionCheckEnabledProperty());
    }

    @Override
    public void execute() {
        new VersionWorker(buildInfo.version, dialogService, taskExecutor, preferencesService)
                .checkForNewVersionAsync();
    }
}

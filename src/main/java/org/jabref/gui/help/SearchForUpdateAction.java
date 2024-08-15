package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.util.BuildInfo;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;

public class SearchForUpdateAction extends SimpleCommand {

    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public SearchForUpdateAction(PreferencesService preferencesService,
                                 DialogService dialogService,
                                 TaskExecutor taskExecutor) {
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        BuildInfo buildInfo = Injector.instantiateModelOrService(BuildInfo.class);
        new VersionWorker(buildInfo.version, dialogService, taskExecutor, preferencesService)
                .checkForNewVersionAsync();
    }
}

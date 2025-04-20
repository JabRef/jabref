package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;

public class SearchForUpdateAction extends SimpleCommand {

    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public SearchForUpdateAction(GuiPreferences preferences,
                                 DialogService dialogService,
                                 TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        BuildInfo buildInfo = Injector.instantiateModelOrService(BuildInfo.class);
        new VersionWorker(buildInfo.version, dialogService, taskExecutor, preferences)
                .checkForNewVersionAsync();
    }
}

package org.jabref.gui.preferences;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final TaskExecutor taskExecutor;
    private Optional<PreferencesTab> preferencesTabToSelect = Optional.empty();

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
    }

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor, PreferencesTab preferencesTabToSelect) {
        this(jabRefFrame, taskExecutor);
        this.preferencesTabToSelect = Optional.of(preferencesTabToSelect);
   }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        if (preferencesTabToSelect.isEmpty()) {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, null));
        } else {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, preferencesTabToSelect.get()));
        }
    }
}

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
    private Optional<String> preferencesTabToSelectName = Optional.empty();

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
    }

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor, String preferencesTabToSelectName) {
        this(jabRefFrame, taskExecutor);
         this.preferencesTabToSelectName = Optional.of(preferencesTabToSelectName);
   }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        if (preferencesTabToSelectName.isEmpty()) {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, ""));
        } else {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, preferencesTabToSelectName.get()));
        }
    }
}

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
    private Optional<Class<? extends PreferencesTab>> preferencesTabToSelectClass = Optional.empty();

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor) {
        this.jabRefFrame = jabRefFrame;
        this.taskExecutor = taskExecutor;
    }

    public ShowPreferencesAction(JabRefFrame jabRefFrame, TaskExecutor taskExecutor, Class<? extends PreferencesTab> preferencesTabToSelectClass) {
        this(jabRefFrame, taskExecutor);
        this.preferencesTabToSelectClass = Optional.of(preferencesTabToSelectClass);
   }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        if (preferencesTabToSelectClass.isEmpty()) {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, null));
        } else {
            dialogService.showCustomDialog(new PreferencesDialogView(jabRefFrame, preferencesTabToSelectClass.get()));
        }
    }
}

package org.jabref.gui.help;

import com.airhacks.afterburner.injection.Injector;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.errorconsole.ErrorConsoleView;

/**
 * Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 *
 * It offers a separate tab for the log output.
 */
public class ErrorConsoleAction extends SimpleCommand {

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialog(new ErrorConsoleView());
    }
}

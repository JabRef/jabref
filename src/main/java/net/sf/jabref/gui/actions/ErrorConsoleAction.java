package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.application.Platform;

import net.sf.jabref.gui.errorconsole.ErrorConsoleView;
import net.sf.jabref.logic.error.StreamEavesdropper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.Cache;

/**
 * Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 * <p/>
 * It offers a separate tab for the log output.
 */
public class ErrorConsoleAction extends AbstractAction {

    public ErrorConsoleAction(StreamEavesdropper streamEavesdropper, Cache cache) {
        super(Localization.menuTitle("View event log"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Display all error messages"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new ErrorConsoleView().show());
    }

}

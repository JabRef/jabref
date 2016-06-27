package net.sf.jabref.gui.keyboard;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.application.Platform;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;


public class KeyBindingAction extends AbstractAction {


    public KeyBindingAction() {
        super(Localization.lang("Customize key bindings"));
        this.putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.KEY_BINDINGS.getSmallIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> {
            KeyBindingsDialogView view = new KeyBindingsDialogView();
            view.show(Globals.getKeyPrefs());
        });
    }

}

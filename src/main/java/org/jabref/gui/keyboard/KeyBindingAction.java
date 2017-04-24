package org.jabref.gui.keyboard;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.application.Platform;

import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;

public class KeyBindingAction extends AbstractAction {


    public KeyBindingAction() {
        super(Localization.lang("Customize key bindings"));
        this.putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.KEY_BINDINGS.getSmallIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> {
            KeyBindingsDialogView view = new KeyBindingsDialogView();
            view.show();
        });
    }

}

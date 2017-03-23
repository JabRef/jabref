package org.jabref.gui.keyboard;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;

import org.jabref.Globals;

public class KeyBinder {

    private KeyBinder() {
    }

    /**
     * Binds ESC-Key to cancel button
     *
     * @param rootPane     the pane to bind the action to. Typically, this variable is retrieved by this.getRootPane();
     * @param cancelAction the action to bind
     */
    public static void bindCloseDialogKeyToCancelAction(JRootPane rootPane, Action cancelAction) {
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);
    }

}

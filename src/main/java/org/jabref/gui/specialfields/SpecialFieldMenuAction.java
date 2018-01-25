package org.jabref.gui.specialfields;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;

public class SpecialFieldMenuAction extends AbstractAction {

    private final JabRefFrame frame;
    private final Actions actionName;


    public SpecialFieldMenuAction(SpecialFieldValueViewModel val, JabRefFrame frame) {
        super(val.getMenuString(), val.getSpecialFieldValueIcon());
        this.frame = frame;
        this.actionName = val.getCommand();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (frame.getCurrentBasePanel() != null) {
            frame.getCurrentBasePanel().runCommand(actionName);
        }
    }
}

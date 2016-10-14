package net.sf.jabref.gui.specialfields;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.specialfields.SpecialFieldLocalization;
import net.sf.jabref.model.entry.SpecialFieldValue;

public class SpecialFieldMenuAction extends AbstractAction {

    private final JabRefFrame frame;
    private final String actionName;


    public SpecialFieldMenuAction(SpecialFieldValue val, JabRefFrame frame) {
        super(SpecialFieldLocalization.getMenuString(val), SpecialFieldIcon.getSpecialFieldValueIcon(val));
        this.frame = frame;
        this.actionName = val.getActionName();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (frame.getCurrentBasePanel() != null) {
            frame.getCurrentBasePanel().runCommand(actionName);
        }
    }
}

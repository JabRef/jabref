package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.entry.EntryType;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ChangeTypeAction extends AbstractAction {
    final EntryType type;
    final BasePanel panel;

    public ChangeTypeAction(EntryType type, BasePanel bp) {
        super(type.getName());
        this.type = type;
        panel = bp;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        panel.changeType(type);
    }
}

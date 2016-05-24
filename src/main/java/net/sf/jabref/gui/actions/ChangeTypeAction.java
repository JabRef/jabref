package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.entry.EntryType;

public class ChangeTypeAction extends AbstractAction {

    private final String type;
    private final BasePanel panel;

    public ChangeTypeAction(EntryType type, BasePanel bp) {
        super(type.getName());
        this.type = type.getName();
        panel = bp;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        panel.changeTypeOfSelectedEntries(type);
    }
}

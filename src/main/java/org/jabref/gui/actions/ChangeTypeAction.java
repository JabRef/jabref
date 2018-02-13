package org.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.undo.UndoManager;

import javafx.scene.control.MenuItem;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

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

    public static MenuItem as(EntryType type, BibEntry entry, BasePanel panel) {
        return as(type, Collections.singletonList(entry), panel);
    }

    public static MenuItem as(EntryType type, List<BibEntry> entries, BasePanel panel) {
        MenuItem menuItem = new MenuItem(type.getName());
        menuItem.setOnAction(event -> {
            panel.changeType(entries, type.getName());
        });
        return menuItem;
    }
}

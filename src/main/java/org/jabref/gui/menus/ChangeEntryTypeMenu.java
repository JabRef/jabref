package org.jabref.gui.menus;

import java.util.Collection;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;

public class ChangeEntryTypeMenu {

    public ChangeEntryTypeMenu() {

    }

    public static MenuItem createMenuItem(EntryType type, BibEntry entry, UndoManager undoManager) {
        MenuItem menuItem = new MenuItem(type.getName());
        menuItem.setOnAction(event -> {
            NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
            entry.setType(type)
                 .ifPresent(change -> compound.addEdit(new UndoableChangeType(change)));
            undoManager.addEdit(compound);
        });
        return menuItem;
    }

    public ContextMenu getChangeEntryTypePopupMenu(BibEntry entry, BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager) {
        ContextMenu menu = new ContextMenu();
        populateComplete(menu.getItems(), entry, bibDatabaseContext, undoManager);
        return menu;
    }

    public Menu getChangeEntryTypeMenu(BibEntry entry, BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager) {
        Menu menu = new Menu();
        menu.setText(Localization.lang("Change entry type"));
        populateComplete(menu.getItems(), entry, bibDatabaseContext, undoManager);
        return menu;
    }

    private void populateComplete(ObservableList<MenuItem> items, BibEntry entry, BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager) {
        if (bibDatabaseContext.isBiblatexMode()) {
            // Default BibLaTeX
            populate(items, EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX), entry, undoManager);

            // Custom types
            populateSubMenu(items, Localization.lang("Custom"), EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX), entry, undoManager);
        } else {
            // Default BibTeX
            populateSubMenu(items, BibDatabaseMode.BIBTEX.getFormattedName(), BibtexEntryTypes.ALL, entry, undoManager);
            items.remove(0); // Remove separator

            // IEEETran
            populateSubMenu(items, "IEEETran", IEEETranEntryTypes.ALL, entry, undoManager);

            // Custom types
            populateSubMenu(items, Localization.lang("Custom"), EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX), entry, undoManager);
        }
    }

    private void populateSubMenu(ObservableList<MenuItem> items, String text, List<EntryType> entryTypes, BibEntry entry, CountingUndoManager undoManager) {
        if (!entryTypes.isEmpty()) {
            items.add(new SeparatorMenuItem());
            Menu custom = new Menu(text);
            populate(custom, entryTypes, entry, undoManager);
            items.add(custom);
        }
    }

    private void populate(ObservableList<MenuItem> items, Collection<EntryType> types, BibEntry entry, UndoManager undoManager) {
        for (EntryType type : types) {
            items.add(createMenuItem(type, entry, undoManager));
        }
    }

    private void populate(Menu menu, Collection<EntryType> types, BibEntry entry, UndoManager undoManager) {
        populate(menu.getItems(), types, entry, undoManager);
    }
}

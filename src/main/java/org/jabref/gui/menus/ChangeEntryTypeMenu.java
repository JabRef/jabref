package org.jabref.gui.menus;

import java.awt.Font;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.ChangeTypeAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;

public class ChangeEntryTypeMenu {
    public final Map<String, KeyStroke> entryShortCuts = new HashMap<>();

    public ChangeEntryTypeMenu(KeyBindingRepository keyBindings) {
        entryShortCuts.put(BibtexEntryTypes.ARTICLE.getName(), keyBindings.getKey(KeyBinding.NEW_ARTICLE));
        entryShortCuts.put(BibtexEntryTypes.BOOK.getName(), keyBindings.getKey(KeyBinding.NEW_BOOK));
        entryShortCuts.put(BibtexEntryTypes.PHDTHESIS.getName(), keyBindings.getKey(KeyBinding.NEW_PHDTHESIS));
        entryShortCuts.put(BibtexEntryTypes.INBOOK.getName(), keyBindings.getKey(KeyBinding.NEW_MASTERSTHESIS));
        entryShortCuts.put(BibtexEntryTypes.INBOOK.getName(), keyBindings.getKey(KeyBinding.NEW_INBOOK));
        entryShortCuts.put(BibtexEntryTypes.PROCEEDINGS.getName(), keyBindings.getKey(KeyBinding.NEW_PROCEEDINGS));
        entryShortCuts.put(BibtexEntryTypes.UNPUBLISHED.getName(), keyBindings.getKey(KeyBinding.NEW_UNPUBLISHED));
        entryShortCuts.put(BibtexEntryTypes.TECHREPORT.getName(), keyBindings.getKey(KeyBinding.NEW_TECHREPORT));
    }

    public JMenu getChangeEntryTypeMenu(BasePanel panel) {
        JMenu menu = new JMenu(Localization.lang("Change entry type"));
        populateChangeEntryTypeMenu(menu, panel);
        return menu;
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
            items.add(ChangeTypeAction.as(type, entry, undoManager));
        }
    }

    private void populate(Menu menu, Collection<EntryType> types, BibEntry entry, UndoManager undoManager) {
        populate(menu.getItems(), types, entry, undoManager);
    }

    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    private void populateChangeEntryTypeMenu(JMenu menu, BasePanel panel) {
        menu.removeAll();

        // biblatex?
        if (panel.getBibDatabaseContext().isBiblatexMode()) {
            for (EntryType type : EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX)) {
                menu.add(new ChangeTypeAction(type, panel));
            }

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (!customTypes.isEmpty()) {
                menu.addSeparator();
                // custom types
                createEntryTypeSection(panel, menu, "Custom Entries", customTypes);
            }
        } else {
            // Bibtex
            createEntryTypeSection(panel, menu, "BibTeX Entries", BibtexEntryTypes.ALL);
            menu.addSeparator();
            // ieeetran
            createEntryTypeSection(panel, menu, "IEEETran Entries", IEEETranEntryTypes.ALL);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
            if (!customTypes.isEmpty()) {
                menu.addSeparator();
                // custom types
                createEntryTypeSection(panel, menu, "Custom Entries", customTypes);
            }
        }
    }

    private void createEntryTypeSection(BasePanel panel, JMenu menu, String title, List<? extends EntryType> types) {
        // bibtex
        JMenuItem header = new JMenuItem(title);
        Font font = new Font(menu.getFont().getName(), Font.ITALIC, menu.getFont().getSize());
        header.setFont(font);
        header.setEnabled(false);
        if (!types.isEmpty()) {
            menu.add(header);
        }

        for (EntryType type : types) {
            menu.add(new ChangeTypeAction(type, panel));
        }
    }
}

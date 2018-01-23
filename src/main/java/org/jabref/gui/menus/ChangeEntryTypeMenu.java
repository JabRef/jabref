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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.ChangeTypeAction;
import org.jabref.gui.keyboard.KeyBinding;
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

    public ChangeEntryTypeMenu () {
        entryShortCuts.put(BibtexEntryTypes.ARTICLE.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_ARTICLE));
        entryShortCuts.put(BibtexEntryTypes.BOOK.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_BOOK));
        entryShortCuts.put(BibtexEntryTypes.PHDTHESIS.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_PHDTHESIS));
        entryShortCuts.put(BibtexEntryTypes.INBOOK.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_MASTERSTHESIS));
        entryShortCuts.put(BibtexEntryTypes.INBOOK.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_INBOOK));
        entryShortCuts.put(BibtexEntryTypes.PROCEEDINGS.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_PROCEEDINGS));
        entryShortCuts.put(BibtexEntryTypes.UNPUBLISHED.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_UNPUBLISHED));
        entryShortCuts.put(BibtexEntryTypes.TECHREPORT.getName(), Globals.getKeyPrefs().getKey(KeyBinding.NEW_TECHREPORT));
    }

    public JMenu getChangeEntryTypeMenu(BasePanel panel) {
        JMenu menu = new JMenu(Localization.lang("Change entry type"));
        populateChangeEntryTypeMenu(menu, panel);
        return menu;
    }

    public ContextMenu getChangeEntryTypePopupMenu(BibEntry entry, BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager) {
        ContextMenu menu = new ContextMenu();

        if (bibDatabaseContext.isBiblatexMode()) {
            // Default BibLaTeX
            populate(menu, EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX), entry, undoManager);

            // Custom types
            populateSubMenu(menu, Localization.lang("Custom"), EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX), entry, undoManager);
        } else {
            // Default BibTeX
            populateSubMenu(menu, Localization.BIBTEX, BibtexEntryTypes.ALL, entry, undoManager);
            menu.getItems().remove(0); // Remove separator

            // IEEETran
            populateSubMenu(menu, "IEEETran", IEEETranEntryTypes.ALL, entry, undoManager);

            // Custom types
            populateSubMenu(menu, Localization.lang("Custom"), EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX), entry, undoManager);
        }

        return menu;
    }

    private void populateSubMenu(ContextMenu menu, String text, List<EntryType> entryTypes, BibEntry entry, CountingUndoManager undoManager) {
        if (!entryTypes.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            Menu custom = new Menu(text);
            populate(custom, entryTypes, entry, undoManager);
            menu.getItems().add(custom);
        }
    }

    private void populate(ContextMenu menu, Collection<EntryType> types, BibEntry entry, UndoManager undoManager) {
        for (EntryType type : types) {
            menu.getItems().add(ChangeTypeAction.as(type, entry, undoManager));
        }
    }

    private void populate(Menu menu, Collection<EntryType> types, BibEntry entry, UndoManager undoManager) {
        for (EntryType type : types) {
            menu.getItems().add(ChangeTypeAction.as(type, entry, undoManager));
        }
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

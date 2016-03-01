package net.sf.jabref.gui.menus;

import net.sf.jabref.Globals;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.ChangeTypeAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IEEETranEntryTypes;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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
    }

    public JMenu getChangeEntryTypeMenu(BasePanel panel) {
        JMenu menu = new JMenu(Localization.lang("Change entry type"));
        populateChangeEntryTypeMenu(menu, panel);
        return menu;
    }

    public JPopupMenu getChangeentryTypePopupMenu(BasePanel panel) {
        JMenu menu = getChangeEntryTypeMenu(panel);
        return menu.getPopupMenu();
    }
    /**
     * Remove all types from the menu. Then cycle through all available
     * types, and add them.
     */
    private void populateChangeEntryTypeMenu(JMenu menu, BasePanel panel) {
        menu.removeAll();

        // biblatex?
        if(panel.getBibDatabaseContext().isBiblatexMode()) {
            for (EntryType type : EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX)) {
                menu.add(new ChangeTypeAction(type, panel));
            }
        } else {
            // Bibtex
            createEntryTypeSection(panel, menu, "BibTeX Entries", BibtexEntryTypes.ALL);
            menu.addSeparator();
            // ieeetran
            createEntryTypeSection(panel, menu, "IEEETran Entries", IEEETranEntryTypes.ALL);
            menu.addSeparator();
            // custom types
            createEntryTypeSection(panel, menu, "Custom Entries", CustomEntryTypesManager.ALL);
        }
    }

    private void createEntryTypeSection(BasePanel panel, JMenu menu, String title, java.util.List<EntryType> types) {
        // bibtex
        JMenuItem header = new JMenuItem(title);
        Font font = new Font(menu.getFont().getName(), Font.ITALIC, menu.getFont().getSize());
        header.setFont(font);
        header.setEnabled(false);
        if(!types.isEmpty()) {
            menu.add(header);
        }

        for (EntryType type : types) {
            menu.add(new ChangeTypeAction(type, panel));
        }
    }
}

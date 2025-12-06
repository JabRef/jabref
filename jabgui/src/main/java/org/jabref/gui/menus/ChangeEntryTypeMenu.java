package org.jabref.gui.menus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.BiblatexNonStandardEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;

public class ChangeEntryTypeMenu {

    private final List<BibEntry> entries;
    private final BibDatabaseContext bibDatabaseContext;
    private final UndoManager undoManager;
    private final ActionFactory factory;
    private final BibEntryTypesManager entryTypesManager;

    public ChangeEntryTypeMenu(List<BibEntry> entries,
                               BibDatabaseContext bibDatabaseContext,
                               UndoManager undoManager,
                               BibEntryTypesManager entryTypesManager) {
        this.entries = entries;
        this.bibDatabaseContext = bibDatabaseContext;
        this.undoManager = undoManager;
        this.entryTypesManager = entryTypesManager;
        this.factory = new ActionFactory();
    }

    public ContextMenu asContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getItems().setAll(getMenuItems(entries, bibDatabaseContext, undoManager));
        return menu;
    }

    public Menu asSubMenu() {
        Menu menu = new Menu(Localization.lang("Change entry type"));
        menu.getItems().setAll(getMenuItems(entries, bibDatabaseContext, undoManager));
        return menu;
    }

    private ObservableList<MenuItem> getMenuItems(List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, UndoManager undoManager) {
        ObservableList<MenuItem> items = FXCollections.observableArrayList();

        if (bibDatabaseContext.isBiblatexMode()) {
            // Default BibLaTeX - exclude non-standard types to avoid duplicates
            Set<EntryType> nonStandardEntryTypes = BiblatexNonStandardEntryTypeDefinitions.ALL.stream()
                                                                                              .<EntryType>map(BibEntryType::getType)
                                                                                              .collect(Collectors.toSet());
            Collection<BibEntryType> allTypes = entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX);
            Collection<BibEntryType> standardTypes = allTypes.stream()
                                                             .filter(type -> !nonStandardEntryTypes.contains(type.getType()))
                                                             .collect(Collectors.toList());
            items.addAll(fromEntryTypes(standardTypes, entries, undoManager));

            // Non-standard types
            createSubMenu(Localization.lang("Non-standard Types"), BiblatexNonStandardEntryTypeDefinitions.ALL, entries, undoManager)
                    .ifPresent(subMenu -> items.addAll(
                            new SeparatorMenuItem(),
                            subMenu
                    ));

            // Custom types
            createSubMenu(Localization.lang("Custom"), entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBLATEX), entries, undoManager)
                    .ifPresent(subMenu ->
                            items.addAll(new SeparatorMenuItem(),
                                    subMenu
                            ));
        } else {
            // Default BibTeX
            createSubMenu(BibDatabaseMode.BIBTEX.getFormattedName(), BibtexEntryTypeDefinitions.ALL, entries, undoManager)
                    .ifPresent(items::add);

            // IEEETran
            createSubMenu("IEEETran", IEEETranEntryTypeDefinitions.ALL, entries, undoManager)
                    .ifPresent(subMenu -> items.addAll(
                            new SeparatorMenuItem(),
                            subMenu
                    ));

            // Custom types
            createSubMenu(Localization.lang("Custom"), entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBTEX), entries, undoManager)
                    .ifPresent(subMenu -> items.addAll(
                            new SeparatorMenuItem(),
                            subMenu
                    ));
        }

        return items;
    }

    private Optional<Menu> createSubMenu(String text, List<BibEntryType> entryTypes, List<BibEntry> entries, UndoManager undoManager) {
        Menu subMenu = null;

        if (!entryTypes.isEmpty()) {
            subMenu = factory.createMenu(() -> text);
            subMenu.getItems().addAll(fromEntryTypes(entryTypes, entries, undoManager));
        }

        return Optional.ofNullable(subMenu);
    }

    private List<MenuItem> fromEntryTypes(Collection<BibEntryType> types, List<BibEntry> entries, UndoManager undoManager) {
        return types.stream()
                    .map(BibEntryType::getType)
                    .map(type -> factory.createMenuItem(type::getDisplayName, new ChangeEntryTypeAction(type, entries, undoManager)))
                    .toList();
    }
}

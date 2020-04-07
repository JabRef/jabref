package org.jabref.gui.menus;

import java.util.Collection;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

public class ChangeEntryTypeMenu {

    public ChangeEntryTypeMenu() {

    }

    public static MenuItem createMenuItem(EntryType type, BibEntry entry, UndoManager undoManager) {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(type.getDisplayName()));
        menuItem.setOnAction(event -> {
            NamedCompound compound = new NamedCompound(Localization.lang("Change entry type"));
            entry.setType(type)
                 .ifPresent(change -> compound.addEdit(new UndoableChangeType(change)));
            undoManager.addEdit(compound);
        });
        String description = getDescription(type);
        if (StringUtil.isNotBlank(description)) {
            Tooltip tooltip = new Tooltip(description);
            Tooltip.install(menuItem.getContent(), tooltip);
        }
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
            populate(items, Globals.entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX), entry, undoManager);

            // Custom types
            populateSubMenu(items, Localization.lang("Custom"), Globals.entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBLATEX), entry, undoManager);
        } else {
            // Default BibTeX
            populateSubMenu(items, BibDatabaseMode.BIBTEX.getFormattedName(), BibtexEntryTypeDefinitions.ALL, entry, undoManager);
            items.remove(0); // Remove separator

            // IEEETran
            populateSubMenu(items, "IEEETran", IEEETranEntryTypeDefinitions.ALL, entry, undoManager);

            // Custom types
            populateSubMenu(items, Localization.lang("Custom"), Globals.entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBTEX), entry, undoManager);
        }
    }

    private void populateSubMenu(ObservableList<MenuItem> items, String text, List<BibEntryType> entryTypes, BibEntry entry, CountingUndoManager undoManager) {
        if (!entryTypes.isEmpty()) {
            items.add(new SeparatorMenuItem());
            Menu custom = new Menu(text);
            populate(custom, entryTypes, entry, undoManager);
            items.add(custom);
        }
    }

    private void populate(ObservableList<MenuItem> items, Collection<BibEntryType> types, BibEntry entry, UndoManager undoManager) {
        for (BibEntryType type : types) {
            items.add(createMenuItem(type.getType(), entry, undoManager));
        }
    }

    private void populate(Menu menu, Collection<BibEntryType> types, BibEntry entry, UndoManager undoManager) {
        populate(menu.getItems(), types, entry, undoManager);
    }

    //The description is coming from biblatex manual chapter 2
    //Biblatex documentation is favored over the bibtex,
    //since bibtex is a subset of biblatex and biblatex is better documented.
    public static String getDescription(EntryType entryType) {
        if (entryType instanceof StandardEntryType entry) {
            switch (entry) {
                case Article -> {
                    return Localization.lang("An article in a journal, magazine, newspaper, or other periodical which forms a self-contained unit with its own title.");
                }
                case Book -> {
                    return Localization.lang("A single-volume book with one or more authors where the authors share credit for the work as a whole.");
                }
                case Booklet -> {
                    return Localization.lang("A book-like work without a formal publisher or sponsoring institution.");
                }
                case Collection -> {
                    return Localization.lang("A single-volume collection with multiple, self-contained contributions by distinct authors which have their own title. The work as a whole has no overall author but it will usually have an editor.");
                }
                case Conference -> {
                    return Localization.lang("A legacy alias for \"InProceedings\".");
                }
                case InBook -> {
                    return Localization.lang("A part of a book which forms a self-contained unit with its own title.");
                }
                case InCollection -> {
                    return Localization.lang("A contribution to a collection which forms a self-contained unit with a distinct author and title.");
                }
                case InProceedings -> {
                    return Localization.lang("An article in a conference proceedings.");
                }
                case Manual -> {
                    return Localization.lang("Technical or other documentation, not necessarily in printed form.");
                }
                case MastersThesis -> {
                    return Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term  Master's thesis.");
                }
                case Misc -> {
                    return Localization.lang("A fallback type for entries which do not fit into any other category.");
                }
                case PhdThesis -> {
                    return Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term PhD thesis.");
                }
                case Proceedings -> {
                    return Localization.lang("A single-volume conference proceedings. This type is very similar to \"Collection\".");
                }
                case TechReport -> {
                    return Localization.lang("Similar to \"Report\" except that the type field is optional and defaults to the localised term technical report.");
                }
                case Unpublished -> {
                    return Localization.lang("A work with an author and a title which has not been formally published, such as a manuscript or the script of a talk.");
                }
                case BookInBook -> {
                    return Localization.lang("This type is similar to \"InBook\" but intended for works originally published as a stand-alone book.");
                }
                case InReference -> {
                    return Localization.lang("An article in a work of reference. This is a more specific variant of the generic \"InCollection\" entry type.");
                }
                case MvBook -> {
                    return Localization.lang("A multi-volume \"Book\".");
                }
                case MvCollection -> {
                    return Localization.lang("A multi-volume \"Collection\".");
                }
                case MvProceedings -> {
                    return Localization.lang("A multi-volume \"Proceedings\" entry.");
                }
                case MvReference -> {
                    return Localization.lang("A multi-volume \"Reference\" entry. The standard styles will treat this entry type as an alias for \"MvCollection\".");
                }
                case Online -> {
                    return Localization.lang("This entry type is intended for sources such as web sites which are intrinsically online resources.");
                }
                case Reference -> {
                    return Localization.lang("A single-volume work of reference such as an encyclopedia or a dictionary.");
                }
                case Report -> {
                    return Localization.lang("A technical report, research report, or white paper published by a university or some other institution.");
                }
                case Set -> {
                    return Localization.lang("An entry set is a group of entries which are cited as a single reference and listed as a single item in the bibliography.");
                }
                case SuppBook -> {
                    return Localization.lang("Supplemental material in a \"Book\". This type is provided for elements such as prefaces, introductions, forewords, afterwords, etc. which often have a generic title only.");
                }
                case SuppCollection -> {
                    return Localization.lang("Supplemental material in a \"Collection\".");
                }
                case SuppPeriodical -> {
                    return Localization.lang("Supplemental material in a \"Periodical\". This type may be useful when referring to items such as regular columns, obituaries, letters to the editor, etc. which only have a generic title.");
                }
                case Thesis -> {
                    return Localization.lang("A thesis written for an educational institution to satisfy the requirements for a degree.");
                }
                case WWW -> {
                    return Localization.lang("An alias for \"Online\", provided for jurabib compatibility.");
                }
                case Software -> {
                    return Localization.lang("Computer software. The standard styles will treat this entry type as an alias for \"Misc\".");
                }
                case DATESET -> {
                    return Localization.lang("A data set or a similar collection of (mostly) raw data.");
                }
                default -> {
                    return "";
                }
            }
        } else {
            return "";
        }
    }

}

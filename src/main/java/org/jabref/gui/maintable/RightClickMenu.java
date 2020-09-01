package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.SendAsEMailAction;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.filelist.AttachFileAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PreviewPreferences;

public class RightClickMenu {

    public static ContextMenu create(BibEntryTableViewModel entry, KeyBindingRepository keyBindingRepository, BasePanel panel, DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, panel.frame(), stateManager)));
        contextMenu.getItems().add(createCopySubMenu(panel, factory, dialogService, stateManager, preferencesService));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, panel.frame(), stateManager)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, panel.frame(), stateManager)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, panel.frame(), stateManager)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsEMailAction(dialogService, stateManager)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            // ToDo: SpecialField needs the active BasePanel to mark it as changed.
            //  Refactor BasePanel, should mark the BibDatabaseContext or the UndoManager as dirty instead!
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, panel.frame(), dialogService, stateManager));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, panel.frame(), dialogService, stateManager));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, panel.frame(), dialogService, stateManager));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, panel.frame(), dialogService, stateManager));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, panel.frame(), dialogService, stateManager));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, panel.frame(), dialogService, stateManager));
        }

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferencesService)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferencesService)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_URL, new OpenUrlAction(dialogService, stateManager)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.SEARCH_SHORTSCIENCE, new SearchShortScienceAction(dialogService, stateManager)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(new ChangeEntryTypeMenu().getChangeEntryTypeMenu(entry.getEntry(), panel.getBibDatabaseContext(), panel.getUndoManager()));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(panel, dialogService, stateManager)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.ATTACH_FILE, new AttachFileAction(panel, dialogService, stateManager, preferencesService)));
        // ToDo: Refactor BasePanel, see ahead.
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(panel.frame(), dialogService, stateManager)));

        return contextMenu;
    }

    private static Menu createCopySubMenu(BasePanel panel, ActionFactory factory, DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        Menu copySpecialMenu = factory.createMenu(StandardActions.COPY_MORE);
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, Globals.clipboardManager, preferencesService)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, Globals.clipboardManager, preferencesService)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, Globals.clipboardManager, preferencesService)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, Globals.clipboardManager, preferencesService)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, Globals.clipboardManager, preferencesService)));

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        PreviewLayout style = previewPreferences.getCurrentPreviewStyle();
        if (style instanceof CitationStylePreviewLayout) {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_HTML, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
            Menu copyCitationMenu = factory.createMenu(StandardActions.COPY_CITATION_MORE);
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_TEXT, new CopyCitationAction(CitationStyleOutputFormat.TEXT, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_RTF, new CopyCitationAction(CitationStyleOutputFormat.RTF, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_ASCII_DOC, new CopyCitationAction(CitationStyleOutputFormat.ASCII_DOC, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_XSLFO, new CopyCitationAction(CitationStyleOutputFormat.XSL_FO, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
            copySpecialMenu.getItems().add(copyCitationMenu);
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, Globals.clipboardManager, previewPreferences)));
        }

        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(panel, dialogService, Globals.exportFactory, Globals.clipboardManager, Globals.TASK_EXECUTOR)));
        return copySpecialMenu;
    }
}

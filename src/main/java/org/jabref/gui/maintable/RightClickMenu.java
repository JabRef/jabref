package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.OldCommandWrapper;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.filelist.AttachFileAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.worker.SendAsEMailAction;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PreviewPreferences;

public class RightClickMenu {

    public static ContextMenu create(BibEntryTableViewModel entry, KeyBindingRepository keyBindingRepository, BasePanel panel, DialogService dialogService, StateManager stateManager, PreferencesService preferencesService) {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(keyBindingRepository);

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.COPY, new OldCommandWrapper(Actions.COPY, panel)));
        contextMenu.getItems().add(createCopySubMenu(panel, factory, dialogService));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.PASTE, new OldCommandWrapper(Actions.PASTE, panel)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.CUT, new OldCommandWrapper(Actions.CUT, panel)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.DELETE, new OldCommandWrapper(Actions.DELETE, panel)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsEMailAction(dialogService, stateManager)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        if (Globals.prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED)) {
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, panel));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, panel));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, panel));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, panel));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, panel));
            contextMenu.getItems().add(SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, panel));
        }

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferencesService)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferencesService)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.OPEN_URL, new OpenUrlAction(dialogService, stateManager)));

        contextMenu.getItems().add(new SeparatorMenuItem());

        contextMenu.getItems().add(new ChangeEntryTypeMenu().getChangeEntryTypeMenu(entry.getEntry(), panel.getBibDatabaseContext(), panel.getUndoManager()));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(panel, dialogService, stateManager)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.ATTACH_FILE, new AttachFileAction(panel, dialogService, stateManager, preferencesService)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(panel, dialogService, stateManager)));

        return contextMenu;
    }

    private static Menu createCopySubMenu(BasePanel panel, ActionFactory factory, DialogService dialogService) {
        Menu copySpecialMenu = factory.createMenu(StandardActions.COPY_MORE);
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_TITLE, new OldCommandWrapper(Actions.COPY_TITLE, panel)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY, new OldCommandWrapper(Actions.COPY_KEY, panel)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITE_KEY, new OldCommandWrapper(Actions.COPY_CITE_KEY, panel)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new OldCommandWrapper(Actions.COPY_KEY_AND_TITLE, panel)));
        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new OldCommandWrapper(Actions.COPY_KEY_AND_LINK, panel)));

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        PreviewLayout style = previewPreferences.getCurrentPreviewStyle();
        if (style instanceof CitationStylePreviewLayout) {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_HTML, new OldCommandWrapper(Actions.COPY_CITATION_HTML, panel)));
            Menu copyCitationMenu = factory.createMenu(StandardActions.COPY_CITATION_MORE);
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_TEXT, new OldCommandWrapper(Actions.COPY_CITATION_TEXT, panel)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_RTF, new OldCommandWrapper(Actions.COPY_CITATION_RTF, panel)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_ASCII_DOC, new OldCommandWrapper(Actions.COPY_CITATION_ASCII_DOC, panel)));
            copyCitationMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_XSLFO, new OldCommandWrapper(Actions.COPY_CITATION_XSLFO, panel)));
            copySpecialMenu.getItems().add(copyCitationMenu);
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new OldCommandWrapper(Actions.COPY_CITATION_HTML, panel)));
        }

        copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(panel, dialogService)));
        return copySpecialMenu;
    }
}

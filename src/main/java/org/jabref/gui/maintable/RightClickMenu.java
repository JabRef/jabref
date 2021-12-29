package org.jabref.gui.maintable;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.SendAsEMailAction;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.linkedfile.AttachFileAction;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PreviewPreferences;

public class RightClickMenu {

    public static ContextMenu create(BibEntryTableViewModel entry,
                                     KeyBindingRepository keyBindingRepository,
                                     LibraryTab libraryTab,
                                     DialogService dialogService,
                                     StateManager stateManager,
                                     PreferencesService preferencesService,
                                     UndoManager undoManager,
                                     ClipBoardManager clipBoardManager,
                                     TaskExecutor taskExecutor) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, libraryTab.frame(), stateManager)),
                createCopySubMenu(factory, dialogService, stateManager, preferencesService, clipBoardManager, taskExecutor),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, libraryTab.frame(), stateManager)),
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, libraryTab.frame(), stateManager)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(libraryTab.frame(), dialogService, stateManager)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, libraryTab.frame(), stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsEMailAction(dialogService, preferencesService, stateManager)),

                new SeparatorMenuItem(),

                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, libraryTab.frame(), dialogService, preferencesService, undoManager, stateManager),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ATTACH_FILE, new AttachFileAction(libraryTab, dialogService, stateManager, preferencesService.getFilePreferences())),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferencesService)),
                factory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferencesService)),
                factory.createMenuItem(StandardActions.OPEN_URL, new OpenUrlAction(dialogService, stateManager, preferencesService)),
                factory.createMenuItem(StandardActions.SEARCH_SHORTSCIENCE, new SearchShortScienceAction(dialogService, stateManager, preferencesService)),

                new SeparatorMenuItem(),

                new ChangeEntryTypeMenu().getChangeEntryTypeMenu(entry.getEntry(), libraryTab.getBibDatabaseContext(), libraryTab.getUndoManager()),
                factory.createMenuItem(StandardActions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(libraryTab, dialogService, stateManager))
        );

        return contextMenu;
    }

    private static Menu createCopySubMenu(ActionFactory factory,
                                          DialogService dialogService,
                                          StateManager stateManager,
                                          PreferencesService preferencesService,
                                          ClipBoardManager clipBoardManager,
                                          TaskExecutor taskExecutor) {
        Menu copySpecialMenu = factory.createMenu(StandardActions.COPY_MORE);

        copySpecialMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, clipBoardManager, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, clipBoardManager, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, clipBoardManager, preferencesService)),
                factory.createMenuItem(StandardActions.COPY_DOI, new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferencesService)),
                new SeparatorMenuItem()
        );

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = preferencesService.getPreviewPreferences();
        if (previewPreferences.getSelectedPreviewLayout() instanceof CitationStylePreviewLayout) {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_HTML, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)));
            Menu copyCitationMenu = factory.createMenu(StandardActions.COPY_CITATION_MORE);
            copyCitationMenu.getItems().addAll(
                    factory.createMenuItem(StandardActions.COPY_CITATION_TEXT, new CopyCitationAction(CitationStyleOutputFormat.TEXT, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)),
                    factory.createMenuItem(StandardActions.COPY_CITATION_RTF, new CopyCitationAction(CitationStyleOutputFormat.RTF, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)),
                    factory.createMenuItem(StandardActions.COPY_CITATION_ASCII_DOC, new CopyCitationAction(CitationStyleOutputFormat.ASCII_DOC, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)),
                    factory.createMenuItem(StandardActions.COPY_CITATION_XSLFO, new CopyCitationAction(CitationStyleOutputFormat.XSL_FO, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)));
            copySpecialMenu.getItems().add(copyCitationMenu);
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, previewPreferences)));
        }

        copySpecialMenu.getItems().addAll(
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(dialogService, Globals.exportFactory, stateManager, clipBoardManager, taskExecutor, preferencesService)));

        return copySpecialMenu;
    }
}

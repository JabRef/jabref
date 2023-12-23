package org.jabref.gui.maintable;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.SendAsKindleEmailAction;
import org.jabref.gui.SendAsStandardEmailAction;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.linkedfile.AttachFileAction;
import org.jabref.gui.linkedfile.AttachFileFromURLAction;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntryTypesManager;
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
                                     TaskExecutor taskExecutor,
                                     JournalAbbreviationRepository abbreviationRepository,
                                     BibEntryTypesManager entryTypesManager) {
        ActionFactory factory = new ActionFactory(keyBindingRepository);
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, () -> libraryTab, stateManager, undoManager)),
                createCopySubMenu(factory, dialogService, stateManager, preferencesService, clipBoardManager, abbreviationRepository, taskExecutor),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, () -> libraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, () -> libraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(dialogService, stateManager, preferencesService)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, () -> libraryTab, stateManager, undoManager)),

                new SeparatorMenuItem(),

                createSendSubMenu(factory, dialogService, stateManager, preferencesService, entryTypesManager, taskExecutor),

                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, () -> libraryTab, dialogService, preferencesService, undoManager, stateManager),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ATTACH_FILE, new AttachFileAction(libraryTab, dialogService, stateManager, preferencesService.getFilePreferences())),
                factory.createMenuItem(StandardActions.ATTACH_FILE_FROM_URL, new AttachFileFromURLAction(dialogService, stateManager, taskExecutor, preferencesService)),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferencesService, taskExecutor)),
                factory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferencesService, taskExecutor)),

                factory.createMenuItem(StandardActions.OPEN_URL, new OpenUrlAction(dialogService, stateManager, preferencesService)),
                factory.createMenuItem(StandardActions.SEARCH_SHORTSCIENCE, new SearchShortScienceAction(dialogService, stateManager, preferencesService)),

                new SeparatorMenuItem(),

                new ChangeEntryTypeMenu(libraryTab.getSelectedEntries(), libraryTab.getBibDatabaseContext(), undoManager, keyBindingRepository, entryTypesManager).asSubMenu(),
                factory.createMenuItem(StandardActions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(dialogService, stateManager, taskExecutor, preferencesService, undoManager))
        );

        return contextMenu;
    }

    private static Menu createCopySubMenu(ActionFactory factory,
                                          DialogService dialogService,
                                          StateManager stateManager,
                                          PreferencesService preferencesService,
                                          ClipBoardManager clipBoardManager,
                                          JournalAbbreviationRepository abbreviationRepository,
                                          TaskExecutor taskExecutor) {
        Menu copySpecialMenu = factory.createMenu(StandardActions.COPY_MORE);

        copySpecialMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_DOI, new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_DOI_URL, new CopyMoreAction(StandardActions.COPY_DOI_URL, dialogService, stateManager, clipBoardManager, preferencesService, abbreviationRepository)),
                new SeparatorMenuItem()
        );

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = preferencesService.getPreviewPreferences();
        if (previewPreferences.getSelectedPreviewLayout() instanceof CitationStylePreviewLayout) {
            copySpecialMenu.getItems().addAll(
                    factory.createMenuItem(StandardActions.COPY_CITATION_HTML, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, preferencesService, abbreviationRepository)),
                    factory.createMenuItem(StandardActions.COPY_CITATION_TEXT, new CopyCitationAction(CitationStyleOutputFormat.TEXT, dialogService, stateManager, clipBoardManager, taskExecutor, preferencesService, abbreviationRepository)));
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, preferencesService, abbreviationRepository)));
        }

        copySpecialMenu.getItems().addAll(
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferencesService)));

        return copySpecialMenu;
    }

    private static Menu createSendSubMenu(ActionFactory factory,
                                          DialogService dialogService,
                                          StateManager stateManager,
                                          PreferencesService preferencesService,
                                          BibEntryTypesManager entryTypesManager,
                                          TaskExecutor taskExecutor) {
        Menu sendMenu = factory.createMenu(StandardActions.SEND);
        sendMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsStandardEmailAction(dialogService, preferencesService, stateManager, entryTypesManager, taskExecutor)),
                factory.createMenuItem(StandardActions.SEND_TO_KINDLE, new SendAsKindleEmailAction(dialogService, preferencesService, stateManager, taskExecutor)),
                new SeparatorMenuItem()
        );

        return sendMenu;
    }
}

package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.CopyTo;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.frame.SendAsKindleEmailAction;
import org.jabref.gui.frame.SendAsStandardEmailAction;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.linkedfile.AttachFileAction;
import org.jabref.gui.linkedfile.AttachFileFromURLAction;
import org.jabref.gui.menus.ChangeEntryTypeMenu;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.mergeentries.MergeWithFetchedEntryAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.SpecialField;

import com.tobiasdiez.easybind.EasyBind;

public class RightClickMenu {

    public static ContextMenu create(BibEntryTableViewModel entry,
                                     KeyBindingRepository keyBindingRepository,
                                     LibraryTab libraryTab,
                                     DialogService dialogService,
                                     StateManager stateManager,
                                     GuiPreferences preferences,
                                     UndoManager undoManager,
                                     ClipBoardManager clipBoardManager,
                                     TaskExecutor taskExecutor,
                                     JournalAbbreviationRepository abbreviationRepository,
                                     BibEntryTypesManager entryTypesManager,
                                     ImportHandler importHandler) {
        ActionFactory factory = new ActionFactory();
        ContextMenu contextMenu = new ContextMenu();

        ExtractReferencesAction extractReferencesAction = new ExtractReferencesAction(dialogService, stateManager, preferences);
        // Two menu items required, because of menu item display. Action checks preference internal what to do
        MenuItem extractFileReferencesOnline = factory.createMenuItem(StandardActions.EXTRACT_FILE_REFERENCES_ONLINE, extractReferencesAction);
        MenuItem extractFileReferencesOffline = factory.createMenuItem(StandardActions.EXTRACT_FILE_REFERENCES_OFFLINE, extractReferencesAction);

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, () -> libraryTab, stateManager, undoManager)),
                createCopySubMenu(factory, dialogService, stateManager, preferences, clipBoardManager, abbreviationRepository, taskExecutor),
                createCopyToMenu(factory, dialogService, stateManager, preferences, libraryTab, importHandler),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, () -> libraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, () -> libraryTab, stateManager, undoManager)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(dialogService, stateManager, undoManager, preferences)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, () -> libraryTab, stateManager, undoManager)),

                new SeparatorMenuItem(),

                createSendSubMenu(factory, dialogService, stateManager, preferences, entryTypesManager, taskExecutor),

                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, () -> libraryTab, dialogService, preferences, undoManager, stateManager),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ATTACH_FILE, new AttachFileAction(libraryTab, dialogService, stateManager, preferences.getFilePreferences(), preferences.getExternalApplicationsPreferences())),
                factory.createMenuItem(StandardActions.ATTACH_FILE_FROM_URL, new AttachFileFromURLAction(dialogService, stateManager, taskExecutor, preferences)),
                factory.createMenuItem(StandardActions.OPEN_FOLDER, new OpenFolderAction(dialogService, stateManager, preferences, taskExecutor)),
                factory.createMenuItem(StandardActions.OPEN_EXTERNAL_FILE, new OpenExternalFileAction(dialogService, stateManager, preferences, taskExecutor)),
                extractFileReferencesOnline,
                extractFileReferencesOffline,

                factory.createMenuItem(StandardActions.OPEN_URL, new OpenUrlAction(dialogService, stateManager, preferences)),
                factory.createMenuItem(StandardActions.SEARCH_SHORTSCIENCE, new SearchShortScienceAction(dialogService, stateManager, preferences)),

                new SeparatorMenuItem(),

                new ChangeEntryTypeMenu(libraryTab.getSelectedEntries(), libraryTab.getBibDatabaseContext(), undoManager, entryTypesManager).asSubMenu(),
                factory.createMenuItem(StandardActions.MERGE_WITH_FETCHED_ENTRY, new MergeWithFetchedEntryAction(dialogService, stateManager, taskExecutor, preferences, undoManager))
        );

        EasyBind.subscribe(preferences.getGrobidPreferences().grobidEnabledProperty(), enabled -> {
            extractFileReferencesOnline.setVisible(enabled);
            extractFileReferencesOffline.setVisible(!enabled);
        });

        return contextMenu;
    }

    private static Menu createCopyToMenu(ActionFactory factory,
                                         DialogService dialogService,
                                         StateManager stateManager,
                                         GuiPreferences preferences,
                                         LibraryTab libraryTab,
                                         ImportHandler importHandler
                                         ) {
        Menu copyToMenu = factory.createMenu(StandardActions.COPY_TO);

        ObservableList<BibDatabaseContext> openDatabases = stateManager.getOpenDatabases();

        BibDatabaseContext sourceDatabaseContext = libraryTab.getBibDatabaseContext();

        Optional<Path> sourcePath = libraryTab.getBibDatabaseContext().getDatabasePath();
        String sourceDatabaseName = FileUtil.getUniquePathFragment(stateManager.collectAllDatabasePaths(), sourcePath.get()).get();

        if (!openDatabases.isEmpty()) {
            openDatabases.forEach(bibDatabaseContext -> {
                Optional<String> destinationPath = Optional.empty();
                String destinationDatabaseName = "";

                if (bibDatabaseContext.getDatabasePath().isPresent()) {
                    destinationDatabaseName = FileUtil.getUniquePathFragment(stateManager.collectAllDatabasePaths(), bibDatabaseContext.getDatabasePath().get()).get();
                    if (destinationDatabaseName.equals(sourceDatabaseName)) {
                        return;
                    }
                } else if (bibDatabaseContext.getLocation() == DatabaseLocation.SHARED) {
                    destinationDatabaseName = bibDatabaseContext.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared") + "]";
                } else {
                    destinationDatabaseName = destinationPath.orElse(Localization.lang("untitled"));
                }

                copyToMenu.getItems().addAll(
                        factory.createCustomMenuItem(
                                StandardActions.COPY_TO,
                                new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, bibDatabaseContext),
                                destinationDatabaseName
                        )
                );
            });
        }

        return copyToMenu;
    }

    private static Menu createCopySubMenu(ActionFactory factory,
                                          DialogService dialogService,
                                          StateManager stateManager,
                                          GuiPreferences preferences,
                                          ClipBoardManager clipBoardManager,
                                          JournalAbbreviationRepository abbreviationRepository,
                                          TaskExecutor taskExecutor) {
        Menu copySpecialMenu = factory.createMenu(StandardActions.COPY_MORE);

        copySpecialMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_DOI, new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                factory.createMenuItem(StandardActions.COPY_DOI_URL, new CopyMoreAction(StandardActions.COPY_DOI_URL, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                new SeparatorMenuItem()
        );

        // the submenu will behave dependent on what style is currently selected (citation/preview)
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        if (previewPreferences.getSelectedPreviewLayout() instanceof CitationStylePreviewLayout) {
            copySpecialMenu.getItems().addAll(
                    factory.createMenuItem(StandardActions.COPY_CITATION_HTML, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, preferences, abbreviationRepository)),
                    factory.createMenuItem(StandardActions.COPY_CITATION_TEXT, new CopyCitationAction(CitationStyleOutputFormat.TEXT, dialogService, stateManager, clipBoardManager, taskExecutor, preferences, abbreviationRepository)));
        } else {
            copySpecialMenu.getItems().add(factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, preferences, abbreviationRepository)));
        }

        copySpecialMenu.getItems().addAll(
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.EXPORT_TO_CLIPBOARD, new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferences)));

        return copySpecialMenu;
    }

    private static Menu createSendSubMenu(ActionFactory factory,
                                          DialogService dialogService,
                                          StateManager stateManager,
                                          GuiPreferences preferences,
                                          BibEntryTypesManager entryTypesManager,
                                          TaskExecutor taskExecutor) {
        Menu sendMenu = factory.createMenu(StandardActions.SEND);
        sendMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsStandardEmailAction(dialogService, preferences, stateManager, entryTypesManager, taskExecutor)),
                factory.createMenuItem(StandardActions.SEND_TO_KINDLE, new SendAsKindleEmailAction(dialogService, preferences, stateManager, taskExecutor)),
                new SeparatorMenuItem()
        );

        return sendMenu;
    }
}

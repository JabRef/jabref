package org.jabref.gui.frame;

import java.util.function.Supplier;

import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.ai.ClearEmbeddingsAction;
import org.jabref.gui.auximport.NewSubLibraryAction;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.gui.cleanup.CleanupAction;
import org.jabref.gui.copyfiles.CopyFilesAction;
import org.jabref.gui.documentviewer.ShowDocumentViewerAction;
import org.jabref.gui.duplicationFinder.DuplicateSearch;
import org.jabref.gui.edit.CopyMoreAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.edit.ManageKeywordsAction;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.edit.ReplaceStringAction;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorAction;
import org.jabref.gui.entryeditor.OpenEntryEditorAction;
import org.jabref.gui.entryeditor.PreviewSwitchAction;
import org.jabref.gui.exporter.ExportCommand;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.exporter.SaveAction;
import org.jabref.gui.exporter.SaveAllAction;
import org.jabref.gui.exporter.WriteMetadataToLinkedPdfsAction;
import org.jabref.gui.externalfiles.AutoLinkFilesAction;
import org.jabref.gui.externalfiles.DownloadFullTextAction;
import org.jabref.gui.externalfiles.FindUnlinkedFilesAction;
import org.jabref.gui.help.AboutAction;
import org.jabref.gui.help.ErrorConsoleAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.help.SearchForUpdateAction;
import org.jabref.gui.importer.ImportCommand;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.importer.fetcher.LookupIdentifierAction;
import org.jabref.gui.integrity.IntegrityCheckAction;
import org.jabref.gui.journals.AbbreviateAction;
import org.jabref.gui.libraryproperties.LibraryPropertiesAction;
import org.jabref.gui.linkedfile.RedownloadMissingFilesAction;
import org.jabref.gui.maintable.NewLibraryFromPdfActionOffline;
import org.jabref.gui.maintable.NewLibraryFromPdfActionOnline;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.plaincitationparser.PlainCitationParserAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.ShowPreferencesAction;
import org.jabref.gui.preview.CopyCitationAction;
import org.jabref.gui.push.PushToApplicationCommand;
import org.jabref.gui.search.RebuildFulltextSearchIndexAction;
import org.jabref.gui.shared.ConnectToSharedDatabaseCommand;
import org.jabref.gui.shared.PullChangesFromSharedAction;
import org.jabref.gui.sidepane.SidePane;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.slr.EditExistingStudyAction;
import org.jabref.gui.slr.ExistingStudySearchAction;
import org.jabref.gui.slr.StartNewStudyAction;
import org.jabref.gui.specialfields.SpecialFieldMenuItemFactory;
import org.jabref.gui.texparser.ParseLatexAction;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.util.FileUpdateMonitor;

public class MainMenu extends MenuBar {
    private final JabRefFrame frame;
    private final FileHistoryMenu fileHistoryMenu;
    private final SidePane sidePane;
    private final PushToApplicationCommand pushToApplicationCommand;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final BibEntryTypesManager entryTypesManager;
    private final CountingUndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final Supplier<OpenDatabaseAction> openDatabaseActionSupplier;
    private final AiService aiService;

    public MainMenu(JabRefFrame frame,
                    FileHistoryMenu fileHistoryMenu,
                    SidePane sidePane,
                    PushToApplicationCommand pushToApplicationCommand,
                    GuiPreferences preferences,
                    StateManager stateManager,
                    FileUpdateMonitor fileUpdateMonitor,
                    TaskExecutor taskExecutor,
                    DialogService dialogService,
                    JournalAbbreviationRepository abbreviationRepository,
                    BibEntryTypesManager entryTypesManager,
                    CountingUndoManager undoManager,
                    ClipBoardManager clipBoardManager,
                    Supplier<OpenDatabaseAction> openDatabaseActionSupplier,
                    AiService aiService) {
        this.frame = frame;
        this.fileHistoryMenu = fileHistoryMenu;
        this.sidePane = sidePane;
        this.pushToApplicationCommand = pushToApplicationCommand;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.abbreviationRepository = abbreviationRepository;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.openDatabaseActionSupplier = openDatabaseActionSupplier;
        this.aiService = aiService;

        createMenu();
    }

    private void createMenu() {
        ActionFactory factory = new ActionFactory();
        Menu file = new Menu(Localization.lang("File"));
        Menu edit = new Menu(Localization.lang("Edit"));
        Menu library = new Menu(Localization.lang("Library"));
        Menu quality = new Menu(Localization.lang("Quality"));
        Menu lookup = new Menu(Localization.lang("Lookup"));
        Menu view = new Menu(Localization.lang("View"));
        Menu tools = new Menu(Localization.lang("Tools"));
        Menu help = new Menu(Localization.lang("Help"));

        file.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_LIBRARY, new NewDatabaseAction(frame, preferences)),
                factory.createMenuItem(StandardActions.OPEN_LIBRARY, openDatabaseActionSupplier.get()),
                fileHistoryMenu,
                factory.createMenuItem(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, frame::getCurrentLibraryTab, dialogService, preferences, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_LIBRARY_AS, new SaveAction(SaveAction.SaveMethod.SAVE_AS, frame::getCurrentLibraryTab, dialogService, preferences, stateManager)),
                factory.createMenuItem(StandardActions.SAVE_ALL, new SaveAllAction(frame::getLibraryTabs, preferences, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.CLOSE_LIBRARY, new JabRefFrame.CloseDatabaseAction(frame, stateManager)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.IMPORT,
                        factory.createMenuItem(StandardActions.IMPORT_INTO_CURRENT_LIBRARY, new ImportCommand(frame, ImportCommand.ImportMethod.TO_EXISTING, preferences, stateManager, fileUpdateMonitor, taskExecutor, dialogService)),
                        factory.createMenuItem(StandardActions.IMPORT_INTO_NEW_LIBRARY, new ImportCommand(frame, ImportCommand.ImportMethod.AS_NEW, preferences, stateManager, fileUpdateMonitor, taskExecutor, dialogService))),

                factory.createSubMenu(StandardActions.EXPORT,
                        factory.createMenuItem(StandardActions.EXPORT_ALL, new ExportCommand(ExportCommand.ExportMethod.EXPORT_ALL, frame::getCurrentLibraryTab, stateManager, dialogService, preferences, entryTypesManager, abbreviationRepository, taskExecutor)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED, new ExportCommand(ExportCommand.ExportMethod.EXPORT_SELECTED, frame::getCurrentLibraryTab, stateManager, dialogService, preferences, entryTypesManager, abbreviationRepository, taskExecutor)),
                        factory.createMenuItem(StandardActions.SAVE_SELECTED_AS_PLAIN_BIBTEX, new SaveAction(SaveAction.SaveMethod.SAVE_SELECTED, frame::getCurrentLibraryTab, dialogService, preferences, stateManager))),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.REMOTE_DB,
                        factory.createMenuItem(StandardActions.CONNECT_TO_SHARED_DB, new ConnectToSharedDatabaseCommand(frame, dialogService)),
                        factory.createMenuItem(StandardActions.PULL_CHANGES_FROM_SHARED_DB, new PullChangesFromSharedAction(stateManager))),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SHOW_PREFS, new ShowPreferencesAction(frame, dialogService)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.QUIT, new JabRefFrame.CloseAction(frame))
        );

        edit.getItems().addAll(
                factory.createMenuItem(StandardActions.UNDO, new UndoAction(frame::getCurrentLibraryTab, undoManager, dialogService, stateManager)),
                factory.createMenuItem(StandardActions.REDO, new RedoAction(frame::getCurrentLibraryTab, undoManager, dialogService, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT, frame::getCurrentLibraryTab, stateManager, undoManager)),

                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY, frame::getCurrentLibraryTab, stateManager, undoManager)),
                factory.createSubMenu(StandardActions.COPY_MORE,
                        factory.createMenuItem(StandardActions.COPY_TITLE, new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.COPY_KEY, new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.COPY_CITE_KEY, new CopyMoreAction(StandardActions.COPY_CITE_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_TITLE, new CopyMoreAction(StandardActions.COPY_KEY_AND_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.COPY_KEY_AND_LINK, new CopyMoreAction(StandardActions.COPY_KEY_AND_LINK, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.COPY_CITATION_PREVIEW, new CopyCitationAction(CitationStyleOutputFormat.HTML, dialogService, stateManager, clipBoardManager, taskExecutor, preferences, abbreviationRepository)),
                        factory.createMenuItem(StandardActions.EXPORT_SELECTED_TO_CLIPBOARD, new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferences))),

                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE, frame::getCurrentLibraryTab, stateManager, undoManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REPLACE_ALL, new ReplaceStringAction(frame::getCurrentLibraryTab, stateManager, dialogService)),
                factory.createMenuItem(StandardActions.GENERATE_CITE_KEYS, new GenerateCitationKeyAction(frame::getCurrentLibraryTab, dialogService, stateManager, taskExecutor, preferences, undoManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.MANAGE_KEYWORDS, new ManageKeywordsAction(stateManager)),
                factory.createMenuItem(StandardActions.AUTOMATIC_FIELD_EDITOR, new AutomaticFieldEditorAction(stateManager, dialogService, undoManager)));
        SeparatorMenuItem specialFieldsSeparator = new SeparatorMenuItem();
        specialFieldsSeparator.visibleProperty().bind(preferences.getSpecialFieldsPreferences().specialFieldsEnabledProperty());

        edit.getItems().addAll(
                specialFieldsSeparator,
                // ToDo: SpecialField needs the active BasePanel to mark it as changed.
                //  Refactor BasePanel, should mark the BibDatabaseContext or the UndoManager as dirty instead!
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.RANKING, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.RELEVANCE, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.QUALITY, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.getSpecialFieldSingleItem(SpecialField.PRINTED, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.PRIORITY, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager),
                SpecialFieldMenuItemFactory.createSpecialFieldMenu(SpecialField.READ_STATUS, factory, frame::getCurrentLibraryTab, dialogService, preferences, undoManager, stateManager));
        edit.addEventHandler(ActionEvent.ACTION, event -> {
            // Work around for mac only issue, where cmd+v on a dialogue triggers the paste action of menu item, resulting in addition of the pasted content in the MainTable.
            // If the mainscreen is not focused, the actions captured by menu are consumed.
            if (OS.OS_X && !frame.getMainStage().focusedProperty().get()) {
                event.consume();
            }
        });

        library.getItems().addAll(
                factory.createMenuItem(StandardActions.NEW_ENTRY, new NewEntryAction(frame::getCurrentLibraryTab, dialogService, preferences, stateManager)),
                factory.createMenuItem(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new PlainCitationParserAction(dialogService, stateManager)),
                factory.createMenuItem(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, frame::getCurrentLibraryTab, stateManager, undoManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.OPEN_DATABASE_FOLDER, new JabRefFrame.OpenDatabaseFolder(dialogService, stateManager, preferences, () -> stateManager.getActiveDatabase().orElse(null))),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(stateManager, preferences, dialogService)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.LIBRARY_PROPERTIES, new LibraryPropertiesAction(stateManager))
        );

        quality.getItems().addAll(
                factory.createMenuItem(StandardActions.FIND_DUPLICATES, new DuplicateSearch(frame::getCurrentLibraryTab, dialogService, stateManager, preferences, entryTypesManager, taskExecutor)),
                factory.createMenuItem(StandardActions.MERGE_ENTRIES, new MergeEntriesAction(dialogService, stateManager, undoManager, preferences)),
                factory.createMenuItem(StandardActions.CHECK_INTEGRITY, new IntegrityCheckAction(frame::getCurrentLibraryTab, preferences, dialogService, stateManager, (UiTaskExecutor) taskExecutor, abbreviationRepository)),
                factory.createMenuItem(StandardActions.CLEANUP_ENTRIES, new CleanupAction(frame::getCurrentLibraryTab, preferences, dialogService, stateManager, taskExecutor, undoManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SET_FILE_LINKS, new AutoLinkFilesAction(dialogService, preferences, stateManager, undoManager, (UiTaskExecutor) taskExecutor)),

                new SeparatorMenuItem(),

                factory.createSubMenu(StandardActions.ABBREVIATE,
                        factory.createMenuItem(StandardActions.ABBREVIATE_DEFAULT, new AbbreviateAction(StandardActions.ABBREVIATE_DEFAULT, frame::getCurrentLibraryTab, dialogService, stateManager, preferences.getJournalAbbreviationPreferences(), abbreviationRepository, taskExecutor, undoManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_DOTLESS, new AbbreviateAction(StandardActions.ABBREVIATE_DOTLESS, frame::getCurrentLibraryTab, dialogService, stateManager, preferences.getJournalAbbreviationPreferences(), abbreviationRepository, taskExecutor, undoManager)),
                        factory.createMenuItem(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, new AbbreviateAction(StandardActions.ABBREVIATE_SHORTEST_UNIQUE, frame::getCurrentLibraryTab, dialogService, stateManager, preferences.getJournalAbbreviationPreferences(), abbreviationRepository, taskExecutor, undoManager))),

                factory.createMenuItem(StandardActions.UNABBREVIATE, new AbbreviateAction(StandardActions.UNABBREVIATE, frame::getCurrentLibraryTab, dialogService, stateManager, preferences.getJournalAbbreviationPreferences(), abbreviationRepository, taskExecutor, undoManager))
        );

        Menu lookupIdentifiers = factory.createSubMenu(StandardActions.LOOKUP_DOC_IDENTIFIER);
        for (IdFetcher<?> fetcher : WebFetchers.getIdFetchers(preferences.getImportFormatPreferences())) {
            LookupIdentifierAction<?> identifierAction = new LookupIdentifierAction<>(fetcher, stateManager, undoManager, dialogService, taskExecutor);
            lookupIdentifiers.getItems().add(factory.createMenuItem(identifierAction.getAction(), identifierAction));
        }

        lookup.getItems().addAll(
                lookupIdentifiers,
                factory.createMenuItem(StandardActions.DOWNLOAD_FULL_TEXT, new DownloadFullTextAction(dialogService, stateManager, preferences, (UiTaskExecutor) taskExecutor)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.FIND_UNLINKED_FILES, new FindUnlinkedFilesAction(dialogService, stateManager))
        );

        final MenuItem pushToApplicationMenuItem = factory.createMenuItem(pushToApplicationCommand.getAction(), pushToApplicationCommand);
        pushToApplicationCommand.registerReconfigurable(pushToApplicationMenuItem);

        tools.getItems().addAll(
                factory.createMenuItem(StandardActions.PARSE_LATEX, new ParseLatexAction(stateManager)),
                factory.createMenuItem(StandardActions.NEW_SUB_LIBRARY_FROM_AUX, new NewSubLibraryAction(frame, stateManager, dialogService)),
                factory.createMenuItem(StandardActions.NEW_LIBRARY_FROM_PDF_ONLINE, new NewLibraryFromPdfActionOnline(frame, stateManager, dialogService, preferences, taskExecutor)),
                factory.createMenuItem(StandardActions.NEW_LIBRARY_FROM_PDF_OFFLINE, new NewLibraryFromPdfActionOffline(frame, stateManager, dialogService, preferences, taskExecutor)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.WRITE_METADATA_TO_PDF,
                        new WriteMetadataToLinkedPdfsAction(dialogService, preferences.getFieldPreferences(), preferences.getFilePreferences(), preferences.getXmpPreferences(), entryTypesManager, abbreviationRepository, taskExecutor, stateManager)),
                factory.createMenuItem(StandardActions.COPY_LINKED_FILES, new CopyFilesAction(dialogService, preferences, stateManager, (UiTaskExecutor) taskExecutor)), // we know at this point that this is a UITaskExecutor

                new SeparatorMenuItem(),

                createSendSubMenu(factory, dialogService, stateManager, preferences),
                pushToApplicationMenuItem,

                new SeparatorMenuItem(),

                // Systematic Literature Review (SLR)
                factory.createMenuItem(StandardActions.START_NEW_STUDY, new StartNewStudyAction(frame, openDatabaseActionSupplier, fileUpdateMonitor, taskExecutor, preferences, stateManager, dialogService)),
                factory.createMenuItem(StandardActions.EDIT_EXISTING_STUDY, new EditExistingStudyAction(dialogService, stateManager)),
                factory.createMenuItem(StandardActions.UPDATE_SEARCH_RESULTS_OF_STUDY, new ExistingStudySearchAction(frame, openDatabaseActionSupplier, dialogService, fileUpdateMonitor, taskExecutor, preferences, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REBUILD_FULLTEXT_SEARCH_INDEX, new RebuildFulltextSearchIndexAction(stateManager, frame::getCurrentLibraryTab, dialogService, preferences)),
                factory.createMenuItem(StandardActions.CLEAR_EMBEDDINGS_CACHE, new ClearEmbeddingsAction(stateManager, dialogService, aiService, taskExecutor)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.REDOWNLOAD_MISSING_FILES, new RedownloadMissingFilesAction(stateManager, dialogService, preferences.getExternalApplicationsPreferences(), preferences.getFilePreferences(), taskExecutor))
        );

        SidePaneType webSearchPane = SidePaneType.WEB_SEARCH;
        SidePaneType groupsPane = SidePaneType.GROUPS;
        SidePaneType openOfficePane = SidePaneType.OPEN_OFFICE;
        view.getItems().addAll(
                factory.createCheckMenuItem(webSearchPane.getToggleAction(), sidePane.getToggleCommandFor(webSearchPane), sidePane.paneVisibleBinding(webSearchPane)),
                factory.createCheckMenuItem(groupsPane.getToggleAction(), sidePane.getToggleCommandFor(groupsPane), sidePane.paneVisibleBinding(groupsPane)),
                factory.createCheckMenuItem(openOfficePane.getToggleAction(), sidePane.getToggleCommandFor(openOfficePane), sidePane.paneVisibleBinding(openOfficePane)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.NEXT_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.NEXT, frame::getCurrentLibraryTab, stateManager)),
                factory.createMenuItem(StandardActions.PREVIOUS_PREVIEW_STYLE, new PreviewSwitchAction(PreviewSwitchAction.Direction.PREVIOUS, frame::getCurrentLibraryTab, stateManager)),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.SHOW_PDF_VIEWER, new ShowDocumentViewerAction(stateManager, preferences)),
                factory.createMenuItem(StandardActions.EDIT_ENTRY, new OpenEntryEditorAction(frame::getCurrentLibraryTab, stateManager)),
                factory.createMenuItem(StandardActions.OPEN_CONSOLE, new OpenConsoleAction(stateManager, preferences, dialogService))
        );

        help.getItems().addAll(
                factory.createMenuItem(StandardActions.HELP, new HelpAction(HelpFile.CONTENTS, dialogService, preferences.getExternalApplicationsPreferences())),
                factory.createMenuItem(StandardActions.OPEN_FORUM, new OpenBrowserAction("http://discourse.jabref.org/", dialogService, preferences.getExternalApplicationsPreferences())),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.ERROR_CONSOLE, new ErrorConsoleAction()),

                new SeparatorMenuItem(),

                factory.createMenuItem(StandardActions.DONATE, new OpenBrowserAction("https://donations.jabref.org", dialogService, preferences.getExternalApplicationsPreferences())),
                factory.createMenuItem(StandardActions.SEARCH_FOR_UPDATES, new SearchForUpdateAction(preferences, dialogService, taskExecutor)),
                factory.createSubMenu(StandardActions.WEB_MENU,
                        factory.createMenuItem(StandardActions.OPEN_WEBPAGE, new OpenBrowserAction("https://jabref.org/", dialogService, preferences.getExternalApplicationsPreferences())),
                        factory.createMenuItem(StandardActions.OPEN_BLOG, new OpenBrowserAction("https://blog.jabref.org/", dialogService, preferences.getExternalApplicationsPreferences())),
                        factory.createMenuItem(StandardActions.OPEN_FACEBOOK, new OpenBrowserAction("https://www.facebook.com/JabRef/", dialogService, preferences.getExternalApplicationsPreferences())),
                        factory.createMenuItem(StandardActions.OPEN_TWITTER, new OpenBrowserAction("https://twitter.com/jabref_org", dialogService, preferences.getExternalApplicationsPreferences())),
                        factory.createMenuItem(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref", dialogService, preferences.getExternalApplicationsPreferences())),

                        new SeparatorMenuItem(),

                        factory.createMenuItem(StandardActions.OPEN_DEV_VERSION_LINK, new OpenBrowserAction("https://builds.jabref.org/master/", dialogService, preferences.getExternalApplicationsPreferences())),
                        factory.createMenuItem(StandardActions.OPEN_CHANGELOG, new OpenBrowserAction("https://github.com/JabRef/jabref/blob/main/CHANGELOG.md", dialogService, preferences.getExternalApplicationsPreferences()))
                ),
                factory.createMenuItem(StandardActions.ABOUT, new AboutAction())
        );

        // @formatter:on
        getStyleClass().add("mainMenu");
        getMenus().addAll(
                file,
                edit,
                library,
                quality,
                lookup,
                tools,
                view,
                help);
        setUseSystemMenuBar(true);
    }

    private Menu createSendSubMenu(ActionFactory factory,
                                   DialogService dialogService,
                                   StateManager stateManager,
                                   GuiPreferences preferences) {
        Menu sendMenu = factory.createMenu(StandardActions.SEND);
        sendMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.SEND_AS_EMAIL, new SendAsStandardEmailAction(dialogService, preferences, stateManager, entryTypesManager, taskExecutor)),
                factory.createMenuItem(StandardActions.SEND_TO_KINDLE, new SendAsKindleEmailAction(dialogService, preferences, stateManager, taskExecutor))
        );

        return sendMenu;
    }
}

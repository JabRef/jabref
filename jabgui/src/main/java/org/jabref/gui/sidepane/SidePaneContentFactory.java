package org.jabref.gui.sidepane;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.importer.fetcher.WebSearchPaneView;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;

public class SidePaneContentFactory {
    private final LibraryTabContainer tabContainer;
    private final GuiPreferences preferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final AiService aiService;
    private final StateManager stateManager;
    private final AdaptVisibleTabs adaptVisibleTabs;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final ClipBoardManager clipBoardManager;
    private final UndoManager undoManager;

    public SidePaneContentFactory(LibraryTabContainer tabContainer,
                                  GuiPreferences preferences,
                                  JournalAbbreviationRepository abbreviationRepository,
                                  TaskExecutor taskExecutor,
                                  DialogService dialogService,
                                  AiService aiService,
                                  StateManager stateManager,
                                  AdaptVisibleTabs adaptVisibleTabs,
                                  FileUpdateMonitor fileUpdateMonitor,
                                  BibEntryTypesManager entryTypesManager,
                                  ClipBoardManager clipBoardManager,
                                  UndoManager undoManager) {
        this.tabContainer = tabContainer;
        this.preferences = preferences;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.adaptVisibleTabs = adaptVisibleTabs;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.undoManager = undoManager;
    }

    public Node create(SidePaneType sidePaneType) {
        return switch (sidePaneType) {
            case GROUPS ->
                    new GroupTreeView(
                            taskExecutor,
                            stateManager,
                            adaptVisibleTabs,
                            preferences,
                            dialogService,
                            aiService,
                            undoManager,
                            fileUpdateMonitor);
            case OPEN_OFFICE ->
                    new OpenOfficePanel(
                            tabContainer,
                            preferences,
                            preferences.getOpenOfficePreferences(Injector.instantiateModelOrService(JournalAbbreviationRepository.class)),
                            preferences.getExternalApplicationsPreferences(),
                            preferences.getLayoutFormatterPreferences(),
                            preferences.getCitationKeyPatternPreferences(),
                            abbreviationRepository,
                            (UiTaskExecutor) taskExecutor,
                            dialogService,
                            aiService,
                            stateManager,
                            fileUpdateMonitor,
                            entryTypesManager,
                            clipBoardManager,
                            undoManager).getContent();
            case WEB_SEARCH ->
                    new WebSearchPaneView(
                            preferences,
                            dialogService,
                            stateManager);
        };
    }
}

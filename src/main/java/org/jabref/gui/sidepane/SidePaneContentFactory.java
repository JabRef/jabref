package org.jabref.gui.sidepane;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.importer.fetcher.WebSearchPaneView;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class SidePaneContentFactory {
    private final LibraryTabContainer tabContainer;
    private final PreferencesService preferences;
    private final AiService aiService;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final ClipBoardManager clipBoardManager;
    private final UndoManager undoManager;

    public SidePaneContentFactory(LibraryTabContainer tabContainer,
                                  PreferencesService preferences,
                                  AiService aiService,
                                  JournalAbbreviationRepository abbreviationRepository,
                                  TaskExecutor taskExecutor,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  FileUpdateMonitor fileUpdateMonitor,
                                  BibEntryTypesManager entryTypesManager,
                                  ClipBoardManager clipBoardManager,
                                  UndoManager undoManager) {
        this.tabContainer = tabContainer;
        this.preferences = preferences;
        this.aiService = aiService;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.undoManager = undoManager;
    }

    public Node create(SidePaneType sidePaneType) {
        return switch (sidePaneType) {
            case GROUPS -> new GroupTreeView(
                    taskExecutor,
                    stateManager,
                    preferences,
                    dialogService,
                    aiService);
            case OPEN_OFFICE -> new OpenOfficePanel(
                    tabContainer,
                    preferences,
                    aiService,
                    preferences.getKeyBindingRepository(),
                    abbreviationRepository,
                    taskExecutor,
                    dialogService,
                    stateManager,
                    fileUpdateMonitor,
                    entryTypesManager,
                    clipBoardManager,
                    undoManager).getContent();
            case WEB_SEARCH -> new WebSearchPaneView(
                    preferences,
                    dialogService,
                    stateManager);
        };
    }
}

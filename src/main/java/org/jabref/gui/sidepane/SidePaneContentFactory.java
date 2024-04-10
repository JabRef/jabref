package org.jabref.gui.sidepane;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupTreeView;
import org.jabref.gui.importer.fetcher.WebSearchPaneView;
import org.jabref.gui.openoffice.OpenOfficePanel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class SidePaneContentFactory {
    private final LibraryTabContainer tabContainer;
    private final PreferencesService preferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final UndoManager undoManager;

    public SidePaneContentFactory(LibraryTabContainer tabContainer,
                                  PreferencesService preferences,
                                  JournalAbbreviationRepository abbreviationRepository,
                                  TaskExecutor taskExecutor,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  FileUpdateMonitor fileUpdateMonitor,
                                  BibEntryTypesManager entryTypesManager,
                                  UndoManager undoManager) {
        this.tabContainer = tabContainer;
        this.preferences = preferences;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
    }

    public Node create(SidePaneType sidePaneType) {
        return switch (sidePaneType) {
            case GROUPS -> new GroupTreeView(
                    taskExecutor,
                    stateManager,
                    preferences,
                    dialogService);
            case OPEN_OFFICE -> new OpenOfficePanel(
                    tabContainer,
                    preferences,
                    preferences.getKeyBindingRepository(),
                    abbreviationRepository,
                    taskExecutor,
                    dialogService,
                    stateManager,
                    fileUpdateMonitor,
                    entryTypesManager,
                    undoManager).getContent();
            case WEB_SEARCH -> new WebSearchPaneView(
                    preferences,
                    dialogService,
                    stateManager);
        };
    }
}

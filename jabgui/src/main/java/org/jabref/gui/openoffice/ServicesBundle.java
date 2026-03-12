package org.jabref.gui.openoffice;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.undo.UndoManager;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;

/**
 * Bundle of service-related dependencies used in OpenOfficePanel and related classes
 */
public class ServicesBundle {
    public final DialogService dialogService;
    public final AiService aiService;
    public final StateManager stateManager;
    public final UiTaskExecutor taskExecutor;
    public final ClipBoardManager clipBoardManager;
    public final UndoManager undoManager;

    public ServicesBundle(DialogService dialogService,
                          AiService aiService,
                          StateManager stateManager,
                          UiTaskExecutor taskExecutor,
                          ClipBoardManager clipBoardManager,
                          UndoManager undoManager) {
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.clipBoardManager = clipBoardManager;
        this.undoManager = undoManager;
    }
}

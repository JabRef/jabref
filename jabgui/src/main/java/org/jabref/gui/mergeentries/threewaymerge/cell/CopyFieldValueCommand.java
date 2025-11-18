package org.jabref.gui.mergeentries.threewaymerge.cell;

import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.actions.SimpleCommand;

import org.jspecify.annotations.NonNull;

public class CopyFieldValueCommand extends SimpleCommand {
    private final String fieldValue;
    private final ClipBoardManager clipBoardManager;

    public CopyFieldValueCommand(@NonNull final String fieldValue, StateManager stateManager) {
        this.fieldValue = fieldValue;
        this.clipBoardManager = new ClipBoardManager(stateManager);
    }

    @Override
    public void execute() {
        clipBoardManager.setContent(fieldValue);
    }
}

package org.jabref.gui.mergeentries.threewaymerge.cell;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.actions.SimpleCommand;

import org.jspecify.annotations.NonNull;

public class CopyFieldValueCommand extends SimpleCommand {
    private final String fieldValue;
    private final ClipBoardManager clipBoardManager;

    public CopyFieldValueCommand(@NonNull final String fieldValue) {
        this.fieldValue = fieldValue;
        this.clipBoardManager = new ClipBoardManager();
    }

    @Override
    public void execute() {
        clipBoardManager.setContent(fieldValue);
    }
}

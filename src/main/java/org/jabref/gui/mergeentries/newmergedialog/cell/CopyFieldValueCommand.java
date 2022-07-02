package org.jabref.gui.mergeentries.newmergedialog.cell;

import java.util.Objects;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.preferences.PreferencesService;

public class CopyFieldValueCommand extends SimpleCommand {
    private final String fieldValue;
    private final ClipBoardManager clipBoardManager;

    public CopyFieldValueCommand(PreferencesService preferencesService, final String fieldValue) {
        Objects.requireNonNull(fieldValue, "Field value cannot be null");
        this.fieldValue = fieldValue;
        this.clipBoardManager = new ClipBoardManager(preferencesService);
    }

    @Override
    public void execute() {
        clipBoardManager.setContent(fieldValue);
    }
}

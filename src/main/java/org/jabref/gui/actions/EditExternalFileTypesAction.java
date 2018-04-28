package org.jabref.gui.actions;

import org.jabref.gui.externalfiletype.ExternalFileTypeEditor;

public class EditExternalFileTypesAction extends SimpleCommand {

    private ExternalFileTypeEditor editor;

    @Override
    public void execute() {
        if (editor == null) {
            editor = new ExternalFileTypeEditor();
        }
        editor.show();
    }
}

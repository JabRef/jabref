package org.jabref.gui.actions;

import org.jabref.gui.externalfiletype.ExternalFileTypeEditor;

public class EditExternalFileTypesAction extends SimpleCommand {

    @Override
    public void execute() {
        ExternalFileTypeEditor editor = new ExternalFileTypeEditor();
        editor.show();
    }
}

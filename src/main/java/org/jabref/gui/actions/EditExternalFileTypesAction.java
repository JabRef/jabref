package org.jabref.gui.actions;

import org.jabref.gui.externalfiletype.ExternalFileTypeEditor;

//TODO: DOES NOT SHOW UP
public class EditExternalFileTypesAction extends SimpleCommand {

    private ExternalFileTypeEditor editor;


    @Override
    public void execute() {
        if (editor == null) {
            editor = new ExternalFileTypeEditor(null);

        }

    }

}

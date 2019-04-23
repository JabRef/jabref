package org.jabref.gui.externalfiletype;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiletype.CustomizeExternalFileTypesDialog;

public class EditExternalFileTypesAction extends SimpleCommand {

    @Override
    public void execute() {
        CustomizeExternalFileTypesDialog editor = new CustomizeExternalFileTypesDialog();
        editor.showAndWait();
    }
}

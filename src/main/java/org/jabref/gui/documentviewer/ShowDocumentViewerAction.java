package org.jabref.gui.documentviewer;

import org.jabref.gui.actions.SimpleCommand;

public class ShowDocumentViewerAction extends SimpleCommand {

    @Override
    public void execute() {
        new DocumentViewerView().show();
    }

}

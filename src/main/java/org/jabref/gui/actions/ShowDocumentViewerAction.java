package org.jabref.gui.actions;

import org.jabref.gui.documentviewer.DocumentViewerView;

public class ShowDocumentViewerAction extends SimpleCommand {

    @Override
    public void execute() {
        new DocumentViewerView().show();
    }

}

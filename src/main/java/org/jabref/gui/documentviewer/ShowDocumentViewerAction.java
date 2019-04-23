package org.jabref.gui.documentviewer;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.documentviewer.DocumentViewerView;

public class ShowDocumentViewerAction extends SimpleCommand {

    @Override
    public void execute() {
        new DocumentViewerView().show();
    }

}

package org.jabref.gui.actions;

import org.jabref.gui.BasePanel;
import org.jabref.gui.externalfiles.WriteXMPActionWorker;

public class WriteXMPAction extends SimpleCommand {

    private final BasePanel basePanel;

    public WriteXMPAction(BasePanel basePanel) {
        this.basePanel = basePanel;

    }

    @Override
    public void execute() {
        new WriteXMPActionWorker(basePanel).run();
    }

}

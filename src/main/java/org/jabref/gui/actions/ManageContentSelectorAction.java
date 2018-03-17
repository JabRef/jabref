package org.jabref.gui.actions;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.contentselector.ContentSelectorDialog;

//TODO: Throws an NPE If BasePanel is null
public class ManageContentSelectorAction extends SimpleCommand {

    private final BasePanel basePanel;
    private final JabRefFrame frame;

    public ManageContentSelectorAction(BasePanel basePanel, JabRefFrame frame)
    {
        this.basePanel = basePanel;
        this.frame = frame;
    }

    @Override
    public void execute() {
        ContentSelectorDialog csd = new ContentSelectorDialog(null, frame, basePanel, false, null);
        csd.setVisible(true);

    }

}

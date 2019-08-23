package org.jabref.gui.bibtexextractor;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

public class ExtractBibtexAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ExtractBibtexAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        ExtractBibtexDialog dlg = new ExtractBibtexDialog(jabRefFrame);
        dlg.showAndWait();
    }
}

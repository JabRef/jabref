package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternDialog;

public class BibtexKeyPatternAction extends SimpleCommand {

    private JabRefFrame frame;

    public BibtexKeyPatternAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        BibtexKeyPatternDialog bibtexKeyPatternDialog = new BibtexKeyPatternDialog(frame.getCurrentBasePanel());
        bibtexKeyPatternDialog.setLocationRelativeTo(null);
        bibtexKeyPatternDialog.setVisible(true);
    }
}

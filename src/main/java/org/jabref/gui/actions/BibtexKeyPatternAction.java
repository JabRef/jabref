package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternDialog;

public class BibtexKeyPatternAction extends SimpleCommand {

    private final JabRefFrame frame;

    public BibtexKeyPatternAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
       new BibtexKeyPatternDialog(frame.getCurrentBasePanel()).showAndWait();
    }
}

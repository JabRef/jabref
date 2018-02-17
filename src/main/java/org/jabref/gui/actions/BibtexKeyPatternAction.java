package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibtexkeypattern.BibtexKeyPatternDialog;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternAction extends SimpleCommand {

    private JabRefFrame frame;
    private BibtexKeyPatternDialog bibtexKeyPatternDialog;

    public BibtexKeyPatternAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        JabRefPreferences.getInstance();
        if (bibtexKeyPatternDialog == null) {
            // if no instance of BibtexKeyPatternDialog exists, create new one
            bibtexKeyPatternDialog = new BibtexKeyPatternDialog(frame, frame.getCurrentBasePanel());
        } else {
            // BibtexKeyPatternDialog allows for updating content based on currently selected panel
            bibtexKeyPatternDialog.setPanel(frame.getCurrentBasePanel());
        }
        bibtexKeyPatternDialog.setLocationRelativeTo(null);
        bibtexKeyPatternDialog.setVisible(true);
    }
}

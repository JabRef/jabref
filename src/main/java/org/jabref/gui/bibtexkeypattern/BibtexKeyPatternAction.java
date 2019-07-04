package org.jabref.gui.bibtexkeypattern;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class BibtexKeyPatternAction extends SimpleCommand {

    private final JabRefFrame frame;

    public BibtexKeyPatternAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
       new BibtexKeyPatternDialog(frame.getCurrentBasePanel()).showAndWait();
    }
}

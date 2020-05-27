package org.jabref.gui.citationkeypattern;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class CitationKeyPatternAction extends SimpleCommand {

    private final JabRefFrame frame;

    public CitationKeyPatternAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        new CitationKeyPatternDialog(frame.getCurrentBasePanel()).showAndWait();
    }
}

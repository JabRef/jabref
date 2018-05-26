package org.jabref.gui.actions;

import org.jabref.gui.GenFieldsCustomizer;
import org.jabref.gui.JabRefFrame;

public class SetupGeneralFieldsAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public SetupGeneralFieldsAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        GenFieldsCustomizer gf = new GenFieldsCustomizer(jabRefFrame);
        gf.setVisible(true);

    }

}

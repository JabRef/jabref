package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.protectedterms.ProtectedTermsDialog;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

public class ManageProtectedTermsAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final ProtectedTermsLoader termsLoader;

    public ManageProtectedTermsAction(JabRefFrame jabRefFrame, ProtectedTermsLoader termsLoader) {
        this.jabRefFrame = jabRefFrame;
        this.termsLoader = termsLoader;
    }
    @Override
    public void execute() {
        ProtectedTermsDialog protectTermsDialog = new ProtectedTermsDialog(jabRefFrame);
        protectTermsDialog.setVisible(true);

    }

}

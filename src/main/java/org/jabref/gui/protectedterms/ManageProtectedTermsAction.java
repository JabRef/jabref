package org.jabref.gui.protectedterms;

import org.jabref.gui.actions.SimpleCommand;

public class ManageProtectedTermsAction extends SimpleCommand {

    @Override
    public void execute() {
        ManageProtectedTermsDialog protectTermsDialog = new ManageProtectedTermsDialog();
        protectTermsDialog.showAndWait();
    }
}

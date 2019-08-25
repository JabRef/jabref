package org.jabref.gui.actions;

import org.jabref.gui.sharelatex.ShareLatexLoginDialog;

public class SynchronizeWithShareLatexAction extends SimpleCommand {

    public SynchronizeWithShareLatexAction() {
        super();
    }

    @Override
    public void execute() {
        new ShareLatexLoginDialog().show();

    }
}

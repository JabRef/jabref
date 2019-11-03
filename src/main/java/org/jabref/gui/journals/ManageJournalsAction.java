package org.jabref.gui.journals;

import org.jabref.gui.actions.SimpleCommand;

public class ManageJournalsAction extends SimpleCommand {

    @Override
    public void execute() {
        new ManageJournalAbbreviationsView().show();
    }
}

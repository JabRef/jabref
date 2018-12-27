package org.jabref.gui.actions;

import org.jabref.gui.journals.ManageJournalAbbreviationsView;

public class ManageJournalsAction extends SimpleCommand {

    @Override
    public void execute() {
        new ManageJournalAbbreviationsView().show();
    }

}

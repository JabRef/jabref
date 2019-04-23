package org.jabref.gui.journals;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.journals.ManageJournalAbbreviationsView;

public class ManageJournalsAction extends SimpleCommand {

    @Override
    public void execute() {
        new ManageJournalAbbreviationsView().show();
    }

}

package org.jabref.gui.journals;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import javafx.application.Platform;

import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.logic.l10n.Localization;

public class ManageJournalsAction extends MnemonicAwareAction {

    public ManageJournalsAction() {
        super();
        putValue(Action.NAME, Localization.menuTitle("Manage journal abbreviations"));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Platform.runLater(() -> new ManageJournalAbbreviationsView().show());
    }
}

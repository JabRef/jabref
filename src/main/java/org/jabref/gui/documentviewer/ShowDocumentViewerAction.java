package org.jabref.gui.documentviewer;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import javafx.application.Platform;

import org.jabref.gui.IconTheme;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.logic.l10n.Localization;

public class ShowDocumentViewerAction extends MnemonicAwareAction {

    public ShowDocumentViewerAction(String title, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    public ShowDocumentViewerAction() {
        this(Localization.menuTitle("Show document viewer"), Localization.lang("Show document viewer"),
                IconTheme.JabRefIcon.PDF_FILE.getIcon());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new DocumentViewerView().show());
    }

}

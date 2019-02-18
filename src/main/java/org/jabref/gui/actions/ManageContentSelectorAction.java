package org.jabref.gui.actions;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.contentselector.ContentSelectorDialogView;
import org.jabref.logic.l10n.Localization;

public class ManageContentSelectorAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageContentSelectorAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        BasePanel basePanel = jabRefFrame.getCurrentBasePanel();
        if (noActiveConnectionExists(basePanel)) {
            jabRefFrame.getDialogService().showErrorDialogAndWait(Localization.lang("Active database connection do not exists!"));
            return;
        }
        new ContentSelectorDialogView(basePanel).showAndWait();
    }

    private boolean noActiveConnectionExists(BasePanel basePanel) {
        return basePanel == null || basePanel.getBibDatabaseContext() == null;
    }
}

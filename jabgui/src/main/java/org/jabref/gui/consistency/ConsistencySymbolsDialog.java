package org.jabref.gui.consistency;

import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ConsistencySymbolsDialog extends BaseDialog<Void> {

    public ConsistencySymbolsDialog() {
        this.setTitle(Localization.lang("Symbols information"));
        this.initModality(Modality.NONE);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }
}

package org.jabref.gui.fallbackerror;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;


public class FallbackErrorView extends BaseDialog<Void> {

    public FallbackErrorView() {
        this.setTitle(Localization.lang("Unexpected error"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }
}

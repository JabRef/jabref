package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public abstract class AbstractDialogView extends FXMLView {

    public AbstractDialogView() {
        super();

        // Set resource bundle to internal localizations
        bundle = Localization.getMessages();
    }

    public abstract void show();
}

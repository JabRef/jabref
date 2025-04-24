package org.jabref.gui.l10n;

import java.util.ResourceBundle;

import com.airhacks.afterburner.views.ResourceLocator;

public class LocalizationLocator implements ResourceLocator {
    @Override
    public ResourceBundle getResourceBundle(String s) {
        return org.jabref.logic.l10n.Localization.getMessages();
    }
}

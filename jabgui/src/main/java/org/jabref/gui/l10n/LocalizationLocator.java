package org.jabref.gui.l10n;

import java.util.ResourceBundle;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ResourceLocator;

public class LocalizationLocator implements ResourceLocator {
    @Override
    public ResourceBundle getResourceBundle(String s) {
        return Localization.getMessages();
    }
}

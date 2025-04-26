package org.jabref.gui.l10n;

import java.util.ResourceBundle;

import com.airhacks.afterburner.views.ResourceLocator;

import org.jabref.logic.l10n.Localization;

public class LocalizationLocator implements ResourceLocator {
    @Override
    public ResourceBundle getResourceBundle(String s) {
        return Localization.getMessages();
    }
}

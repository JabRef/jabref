package org.jabref.logic.l10n;

import com.airhacks.afterburner.views.ResourceLocator;
import java.util.ResourceBundle;

public class LocalizationLocator implements ResourceLocator {

    @Override
    public ResourceBundle getResourceBundle(String s) {
        return Localization.getMessages();
    }
}

package org.jabref.gui.util;

import java.util.ResourceBundle;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefResourceLocator implements ResourceLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefResourceLocator.class);

    @Override
    public ResourceBundle getResourceBundle(String s) {
        LOGGER.debug("Requested bundle for '{}'.", s);

        return Localization.getMessages();
    }
}

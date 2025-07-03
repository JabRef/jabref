package org.jabref.http.cli;

import java.util.ResourceBundle;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Code duplication of org.jabref.gui.util.JabRefResourceLocator - should be streamlined
// Reason for duplication: The CLI does not depend on the GUI module, so it cannot use the GUI's JabRefResourceLocator directly.
public class JabRefResourceLocator implements ResourceLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefResourceLocator.class);

    @Override
    public ResourceBundle getResourceBundle(String s) {
        LOGGER.debug("Requested bundle for '{}'.", s);

        return Localization.getMessages();
    }
}

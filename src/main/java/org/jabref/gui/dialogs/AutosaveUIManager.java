package org.jabref.gui.dialogs;

import org.jabref.gui.BasePanel;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.model.database.event.AutosaveEvent;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has an abstract UI role as it listens for an {@link AutosaveEvent}
 * and saves the bib file associated with the given {@link BasePanel}.
 */
public class AutosaveUIManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutosaveUIManager.class);
    private final BasePanel panel;


    public AutosaveUIManager(BasePanel panel) {
        this.panel = panel;
    }

    @Subscribe
    public void listen(@SuppressWarnings("unused") AutosaveEvent event) {
        try {
            new SaveDatabaseAction(panel).runCommand();
        } catch (Throwable e) {
            LOGGER.error("Problem occured while saving.", e);
        }
    }
}

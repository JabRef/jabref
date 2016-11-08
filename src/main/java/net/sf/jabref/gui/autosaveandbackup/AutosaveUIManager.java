package net.sf.jabref.gui.autosaveandbackup;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.exporter.SaveDatabaseAction;
import net.sf.jabref.model.database.event.AutosaveEvent;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class has an abstract UI role as it listens for an {@link AutosaveEvent}
 * and saves the bib file associated with the given {@link BasePanel}.
 */
public class AutosaveUIManager {

    private static final Log LOGGER = LogFactory.getLog(AutosaveUIManager.class);
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

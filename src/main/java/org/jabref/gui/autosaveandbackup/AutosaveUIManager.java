package org.jabref.gui.autosaveandbackup;

import org.jabref.Logger;
import org.jabref.gui.BasePanel;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.model.database.event.AutosaveEvent;

import com.google.common.eventbus.Subscribe;

/**
 * This class has an abstract UI role as it listens for an {@link AutosaveEvent}
 * and saves the bib file associated with the given {@link BasePanel}.
 */
public class AutosaveUIManager {

    private final BasePanel panel;


    public AutosaveUIManager(BasePanel panel) {
        this.panel = panel;
    }

    @Subscribe
    public void listen(@SuppressWarnings("unused") AutosaveEvent event) {
        try {
            new SaveDatabaseAction(panel).runCommand();
        } catch (Throwable e) {
            Logger.error(this, "Problem occured while saving.", e);
        }
    }
}

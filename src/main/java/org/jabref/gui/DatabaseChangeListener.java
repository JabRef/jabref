package org.jabref.gui;


import com.google.common.eventbus.Subscribe;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;

public class DatabaseChangeListener {

    private BasePanel basePanel;

    public DatabaseChangeListener(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Subscribe
    public void listen(BibDatabaseContextChangedEvent event) {
        basePanel.markBaseChanged();
    }

}

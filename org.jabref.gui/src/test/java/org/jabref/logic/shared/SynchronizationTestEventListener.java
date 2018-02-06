package org.jabref.logic.shared;

import org.jabref.logic.shared.event.SharedEntryNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.testutils.category.DatabaseTest;

import com.google.common.eventbus.Subscribe;

@DatabaseTest
public class SynchronizationTestEventListener {

    private SharedEntryNotPresentEvent sharedEntryNotPresentEvent;
    private UpdateRefusedEvent updateRefusedEvent;


    @Subscribe
    public void listen(SharedEntryNotPresentEvent event) {
        this.sharedEntryNotPresentEvent = event;
    }

    @Subscribe
    public void listen(UpdateRefusedEvent event) {
        this.updateRefusedEvent = event;
    }

    public SharedEntryNotPresentEvent getSharedEntryNotPresentEvent() {
        return sharedEntryNotPresentEvent;
    }

    public UpdateRefusedEvent getUpdateRefusedEvent() {
        return updateRefusedEvent;
    }
}

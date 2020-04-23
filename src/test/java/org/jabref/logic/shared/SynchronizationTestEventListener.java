package org.jabref.logic.shared;

import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.testutils.category.DatabaseTest;

import com.google.common.eventbus.Subscribe;

@DatabaseTest
public class SynchronizationTestEventListener {

    private SharedEntriesNotPresentEvent sharedEntriesNotPresentEvent;
    private UpdateRefusedEvent updateRefusedEvent;

    @Subscribe
    public void listen(SharedEntriesNotPresentEvent event) {
        this.sharedEntriesNotPresentEvent = event;
    }

    @Subscribe
    public void listen(UpdateRefusedEvent event) {
        this.updateRefusedEvent = event;
    }

    public SharedEntriesNotPresentEvent getSharedEntriesNotPresentEvent() {
        return sharedEntriesNotPresentEvent;
    }

    public UpdateRefusedEvent getUpdateRefusedEvent() {
        return updateRefusedEvent;
    }
}

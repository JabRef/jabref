package org.jabref.shared;

import org.jabref.shared.event.SharedEntryNotPresentEvent;
import org.jabref.shared.event.UpdateRefusedEvent;
import org.jabref.testutils.category.DatabaseTests;

import com.google.common.eventbus.Subscribe;
import org.junit.experimental.categories.Category;

@Category(DatabaseTests.class)
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

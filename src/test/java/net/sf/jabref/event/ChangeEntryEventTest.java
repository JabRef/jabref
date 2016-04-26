package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

import com.google.common.eventbus.EventBus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ChangeEntryEventTest {

    private EventBus eventBus;
    private TestEventListener testEventListener;

    @Before
    public void setUp() {
        eventBus = new EventBus();
        testEventListener = new TestEventListener();
        eventBus.register(testEventListener);
    }

    @Test
    public void testEventReceivement() {
        BibEntry shouldBeBibEntry = new BibEntry();
        shouldBeBibEntry.setId(IdGenerator.next());
        ChangeEntryEvent aocee = new ChangeEntryEvent(shouldBeBibEntry);
        eventBus.post(aocee);

        Assert.assertEquals(shouldBeBibEntry, testEventListener.getBibEntry());
        Assert.assertEquals(shouldBeBibEntry, aocee.getBibEntry());
    }
}

package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

import com.google.common.eventbus.EventBus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HierarchicalEventTest {

    private EventBus eventBus;
    private TestHierarchicalEventListener testHierarchicalEventListener;


    @Before
    public void setUp() {
        eventBus = new EventBus();
        testHierarchicalEventListener = new TestHierarchicalEventListener();
        eventBus.register(testHierarchicalEventListener);
    }

    @Test
    public void testParentEntryOnChildReceivementOfAddEntryEvent() {
        BibEntry shouldBeBibEntry = new BibEntry();
        shouldBeBibEntry.setId("testkey3");
        AddEntryEvent aee = new AddEntryEvent(shouldBeBibEntry);
        eventBus.post(aee);

        Assert.assertEquals(shouldBeBibEntry.getId(), testHierarchicalEventListener.getParentBibEntry().getId());
        Assert.assertEquals(shouldBeBibEntry.getId(), testHierarchicalEventListener.getBibEntry().getId());
        Assert.assertEquals(shouldBeBibEntry.getId(), aee.getEntry().getId());
    }

    @Test
    public void testParentEntryOnChildReceivementOfChangeEntryEvent() {
        BibEntry shouldBeBibEntry = new BibEntry();
        shouldBeBibEntry.setId("testkey4");
        ChangeEntryEvent cee = new ChangeEntryEvent(shouldBeBibEntry);
        eventBus.post(cee);

        Assert.assertEquals(shouldBeBibEntry.getId(), testHierarchicalEventListener.getParentBibEntry().getId());
        Assert.assertEquals(shouldBeBibEntry.getId(), testHierarchicalEventListener.getBibEntry().getId());
        Assert.assertEquals(shouldBeBibEntry.getId(), cee.getEntry().getId());
    }

    @Test
    public void testChildEntryNullOnParentReceivementOfAddOrChangeEntryEvent() {
        BibEntry shouldBeBibEntry = new BibEntry();
        shouldBeBibEntry.setId("testkey5");
        AddOrChangeEntryEvent aocee = new AddOrChangeEntryEvent(shouldBeBibEntry);
        eventBus.post(aocee);

        Assert.assertEquals(shouldBeBibEntry.getId(), testHierarchicalEventListener.getParentBibEntry().getId());
        Assert.assertNull(testHierarchicalEventListener.getBibEntry());
        Assert.assertEquals(shouldBeBibEntry.getId(), aocee.getEntry().getId());
    }


}

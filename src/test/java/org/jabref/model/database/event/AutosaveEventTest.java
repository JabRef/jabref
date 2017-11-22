package org.jabref.model.database.event;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AutosaveEventTest {

    @Test
    public void givenNothingWhenCreatingThenNotNull() {
        AutosaveEvent e = new AutosaveEvent();
        assertNotNull(e);
    }
}

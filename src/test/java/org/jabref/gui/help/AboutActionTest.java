package org.jabref.gui.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AboutActionTest {

    @Test
    public void shouldOpenDialogOnSameMonitor() {
        // given
        AboutAction aboutAction = new AboutAction();

        // when
        aboutAction.execute();

        // then
        assertNotNull(aboutAction.getAboutDialogView().getOwner());
    }
}

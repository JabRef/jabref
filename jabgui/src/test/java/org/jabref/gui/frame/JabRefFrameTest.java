package org.jabref.gui.frame;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class JabRefFrameTest {

    @Test
    void closesSelectedWelcomeTabAndRunsItsCleanup() {
        Tab welcomeTab = new Tab("Welcome");
        AtomicBoolean cleanedUp = new AtomicBoolean();
        welcomeTab.setOnClosed(_ -> cleanedUp.set(true));
        TabPane tabbedPane = new TabPane(welcomeTab);
        tabbedPane.getSelectionModel().select(welcomeTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(), tabbedPane.getTabs());
        assertTrue(cleanedUp.get());
    }

    @Test
    void keepsTabVetoingItsClose() {
        Tab vetoingTab = new Tab("Vetoing");
        vetoingTab.setOnCloseRequest(Event::consume);
        TabPane tabbedPane = new TabPane(vetoingTab);
        tabbedPane.getSelectionModel().select(vetoingTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(vetoingTab), tabbedPane.getTabs());
    }

    @Test
    void keepsNonClosableTab() {
        Tab fixedTab = new Tab("Fixed");
        fixedTab.setClosable(false);
        TabPane tabbedPane = new TabPane(fixedTab);
        tabbedPane.getSelectionModel().select(fixedTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(fixedTab), tabbedPane.getTabs());
    }
}

package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.shared.DBMSConnectionProperties;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class SharedDatabaseErrorTabTest extends JavaFxTest {

    private final AtomicInteger retries = new AtomicInteger();

    private SharedDatabaseErrorTab tab;

    @BeforeEach
    void setUp() {
        DBMSConnectionProperties connectionProperties = mock(DBMSConnectionProperties.class);
        when(connectionProperties.getDatabase()).thenReturn("Literature");

        interact(() -> {
            tab = new SharedDatabaseErrorTab("shared-1", connectionProperties);
            tab.setRetryAction(retries::incrementAndGet);
            tab.showError(new SQLException("Connection refused"));
        });
    }

    private Button retryButton() {
        return (Button) tab.getContent().lookup(".button");
    }

    private Label message() {
        return (Label) tab.getContent().lookup("#" + SharedDatabaseErrorTab.MESSAGE_ID);
    }

    @Test
    void tabIsNamedAfterTheDatabaseAndShowsTheError() {
        assertEquals("Literature", tab.getText());
        assertEquals("Connection refused", message().getText());
    }

    @Test
    void retryStartsOneAttemptAndBlocksFurtherClicksWhileItRuns() {
        interact(() -> {
            retryButton().fire();
            retryButton().fire();
        });

        assertEquals(1, retries.get());
        assertTrue(retryButton().isDisabled());
    }

    @Test
    void retryRemovesTheTabFromItsPane() {
        TabPane tabPane = new TabPane();
        interact(() -> {
            tabPane.getTabs().add(tab);
            retryButton().fire();
        });

        assertTrue(tabPane.getTabs().isEmpty());
    }

    @Test
    void anotherFailureAllowsRetryingAgain() {
        interact(() -> {
            retryButton().fire();
            tab.showError(new SQLException("Still refused"));
        });

        assertEquals("Still refused", message().getText());

        interact(() -> retryButton().fire());

        assertEquals(2, retries.get());
    }
}

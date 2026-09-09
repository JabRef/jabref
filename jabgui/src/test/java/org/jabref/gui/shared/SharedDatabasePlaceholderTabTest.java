package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.jabref.logic.shared.DBMSConnectionProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SharedDatabasePlaceholderTabTest extends ApplicationTest {

    private final AtomicInteger retries = new AtomicInteger();

    private SharedDatabasePlaceholderTab tab;

    @BeforeEach
    void setUp() {
        DBMSConnectionProperties connectionProperties = mock(DBMSConnectionProperties.class);
        when(connectionProperties.getDatabase()).thenReturn("Literature");

        interact(() -> {
            tab = new SharedDatabasePlaceholderTab("shared-1", connectionProperties);
            tab.setRetryAction(retries::incrementAndGet);
        });
    }

    private Button retryButton() {
        return (Button) tab.getContent().lookup(".button");
    }

    private Label message() {
        return (Label) tab.getContent().lookup("#" + SharedDatabasePlaceholderTab.MESSAGE_ID);
    }

    @Test
    void startsConnectingWithoutRetry() {
        assertEquals("Literature", tab.getText());
        assertEquals("Connecting...", message().getText());
        assertEquals(true, retryButton().isDisabled());
    }

    @Test
    void failureShowsTheError() {
        interact(() -> tab.showError(new SQLException("Connection refused")));

        assertEquals("Connection refused", message().getText());
        assertEquals(false, retryButton().isDisabled());
    }

    @Test
    void retryStartsOneAttemptAndBlocksFurtherClicksWhileItRuns() {
        interact(() -> {
            tab.showError(new SQLException("Connection refused"));
            retryButton().fire();
            retryButton().fire();
        });

        assertEquals(1, retries.get());
        assertEquals(true, retryButton().isDisabled());
    }

    @Test
    void anotherFailureAllowsRetryingAgain() {
        interact(() -> {
            tab.showError(new SQLException("Connection refused"));
            retryButton().fire();
            tab.showError(new SQLException("Still refused"));
        });

        assertEquals("Still refused", message().getText());

        interact(() -> retryButton().fire());

        assertEquals(2, retries.get());
    }
}

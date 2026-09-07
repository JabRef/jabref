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

class SharedDatabaseErrorTabTest extends ApplicationTest {

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
        return (Label) tab.getContent().lookupAll(".label").stream().skip(1).findFirst().orElseThrow();
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
        assertEquals(true, retryButton().isDisabled());
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

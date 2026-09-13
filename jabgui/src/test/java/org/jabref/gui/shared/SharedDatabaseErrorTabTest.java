package org.jabref.gui.shared;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
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
    void retryRunsTheActionAndKeepsTheTab() {
        TabPane tabPane = new TabPane();
        interact(() -> {
            tabPane.getTabs().add(tab);
            retryButton().fire();
        });

        assertEquals(1, retries.get());
        assertEquals(List.of(tab), tabPane.getTabs());
    }

    @Test
    void expertModeIsNamedAfterTheUrlWithoutCredentials() {
        DBMSConnectionProperties expertProperties = mock(DBMSConnectionProperties.class);
        when(expertProperties.isUseExpertMode()).thenReturn(true);
        when(expertProperties.getDatabase()).thenReturn("stale");
        when(expertProperties.getJdbcUrl()).thenReturn("jdbc:postgresql://db.example.org/literature?user=alice&password=secret");

        interact(() -> tab = new SharedDatabaseErrorTab(null, expertProperties));

        assertEquals("literature", tab.getText());
        assertEquals("Could not connect to literature", ((Label) tab.getContent().lookup(".welcome-header-label")).getText());
    }

    @Test
    void missingDatabaseNameFallsBackToAGenericName() {
        DBMSConnectionProperties incompleteProperties = mock(DBMSConnectionProperties.class);
        when(incompleteProperties.getDatabase()).thenReturn(null);

        interact(() -> tab = new SharedDatabaseErrorTab("shared-1", incompleteProperties));

        assertEquals("Shared database connection", tab.getText());
    }

    @Test
    void closeRemovesTheTabFromItsPane() {
        TabPane tabPane = new TabPane();
        interact(() -> {
            tabPane.getTabs().add(tab);
            tab.close();
        });

        assertEquals(List.of(), tabPane.getTabs());
    }

    @Test
    void rememberAsKeepsTheIdOfAReplacedTab() {
        SharedDatabaseErrorTab dialogTab = new SharedDatabaseErrorTab(null, tab.getConnectionProperties());
        dialogTab.rememberAs("shared-1");

        assertEquals(Optional.of("shared-1"), dialogTab.getSharedDatabaseId());
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

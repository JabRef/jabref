package org.jabref.gui.shared;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnectionProperties;

import com.google.common.base.Throwables;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// [impl->req~shared-database.connect-in-background~1]
/// [impl->req~shared-database.reconnect-retry~1]
///
/// Placeholder tab for a shared database that is not (yet) open: it shows "Connecting..." while the connection is
/// being established and, if that fails, the error and a retry button. The library tab replaces it on success.
///
/// A failed reconnection of a remembered database keeps its id here, so that the database stays remembered when
/// JabRef is closed while this tab is open. A database entered in the connection dialog has no id yet.
@NullMarked
public class SharedDatabasePlaceholderTab extends Tab {

    /// Identifies the label carrying the connection error, for lookups in tests.
    static final String MESSAGE_ID = "shared-database-error-message";

    private final @Nullable String sharedDatabaseId;
    private final DBMSConnectionProperties connectionProperties;
    private final Label header = new Label();
    private final Label message = new Label();
    private final Button retryButton = new Button(Localization.lang("Retry"));

    private Runnable retryAction = () -> {
    };

    public SharedDatabasePlaceholderTab(@Nullable String sharedDatabaseId, DBMSConnectionProperties connectionProperties) {
        this.sharedDatabaseId = sharedDatabaseId;
        this.connectionProperties = connectionProperties;

        setText(connectionProperties.getDatabase());

        header.setWrapText(true);
        header.getStyleClass().addAll(StyleClasses.WELCOME_HEADER);
        message.setId(MESSAGE_ID);
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);
        retryButton.setDefaultButton(true);
        retryButton.setOnAction(_ -> retry());

        VBox content = new VBox(10, header, message, retryButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        setContent(content);
        showConnecting();
    }

    public void setRetryAction(Runnable retryAction) {
        this.retryAction = retryAction;
    }

    public void retry() {
        showConnecting();
        retryAction.run();
    }

    public Optional<String> getSharedDatabaseId() {
        return Optional.ofNullable(sharedDatabaseId);
    }

    public DBMSConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    /// Whether the outcome of the connection attempt still matters: the user may have closed this tab or
    /// quit JabRef while the attempt was pending.
    public boolean isAbandoned() {
        return Optional.ofNullable(getTabPane())
                       .map(TabPane::getScene)
                       .map(Scene::getWindow)
                       .filter(Window::isShowing)
                       .isEmpty();
    }

    private void showConnecting() {
        setGraphic(IconTheme.JabRefIcons.CONNECT_DB.getGraphicNode());
        header.setText(Localization.lang("Connecting to shared database %0", connectionProperties.getDatabase()));
        message.setText(Localization.lang("Connecting..."));
        retryButton.setDisable(true);
    }

    public void showError(Exception exception) {
        setGraphic(IconTheme.JabRefIcons.ERROR.getGraphicNode());
        header.setText(Localization.lang("Could not connect to shared database %0", connectionProperties.getDatabase()));
        // The driver's own message is generic ("The connection attempt failed."); the reason is at the end of the cause chain
        Throwable rootCause = Throwables.getRootCause(exception);
        message.setText(Optional.ofNullable(rootCause.getLocalizedMessage()).orElseGet(rootCause::toString));
        retryButton.setDisable(false);
    }
}

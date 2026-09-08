package org.jabref.gui.shared;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnectionProperties;

import org.jspecify.annotations.NullMarked;

/// [impl->req~shared-database.reconnect-retry~1]
///
/// Placeholder tab for a shared database that could not be reconnected on startup.
///
/// The tab keeps the failed connection visible and retryable instead of dropping it: without it the shared database
/// would be missing from the open tabs at quit and therefore be forgotten for the next session.
@NullMarked
public class SharedDatabaseErrorTab extends Tab {

    /// Identifies the label carrying the connection error, for lookups in tests.
    static final String MESSAGE_ID = "shared-database-error-message";

    private final String sharedDatabaseId;
    private final DBMSConnectionProperties connectionProperties;
    private final Label message = new Label();
    private final Button retryButton = new Button(Localization.lang("Retry"));

    private Runnable retryAction = () -> {
    };

    public SharedDatabaseErrorTab(String sharedDatabaseId, DBMSConnectionProperties connectionProperties) {
        this.sharedDatabaseId = sharedDatabaseId;
        this.connectionProperties = connectionProperties;
        String databaseName = connectionProperties.getDatabase();

        setText(databaseName);
        setGraphic(IconTheme.JabRefIcons.ERROR.getGraphicNode());

        message.setId(MESSAGE_ID);
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        retryButton.setDefaultButton(true);
        retryButton.setOnAction(_ -> {
            retryButton.setDisable(true);
            message.setText(Localization.lang("Connecting..."));
            retryAction.run();
        });

        Label header = new Label(Localization.lang("Could not reconnect to shared database %0.", databaseName));
        header.setWrapText(true);
        header.getStyleClass().addAll(StyleClasses.WELCOME_HEADER);

        VBox content = new VBox(10, header, message, retryButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        setContent(content);
    }

    public void setRetryAction(Runnable retryAction) {
        this.retryAction = retryAction;
    }

    public String getSharedDatabaseId() {
        return sharedDatabaseId;
    }

    public DBMSConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    public void showError(Exception exception) {
        message.setText(Optional.ofNullable(exception.getMessage()).orElseGet(exception::toString));
        retryButton.setDisable(false);
    }
}

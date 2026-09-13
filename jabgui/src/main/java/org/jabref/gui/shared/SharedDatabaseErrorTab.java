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
import org.jspecify.annotations.Nullable;

/// [impl->req~shared-database.reconnect-retry~1]
///
/// Placeholder tab for a shared database that could not be connected, on startup or from the connection dialog.
///
/// The tab keeps the failed connection visible and retryable instead of dropping it: without it a remembered shared
/// database would be missing from the open tabs at quit and therefore be forgotten for the next session. A database
/// entered in the connection dialog has no id yet and is remembered only once it connects.
@NullMarked
public class SharedDatabaseErrorTab extends Tab {

    /// Identifies the label carrying the connection error, for lookups in tests.
    static final String MESSAGE_ID = "shared-database-error-message";

    private final @Nullable String sharedDatabaseId;
    private final DBMSConnectionProperties connectionProperties;
    private final Label message = new Label();
    private final Button retryButton = new Button(Localization.lang("Retry"));

    private Runnable retryAction = () -> {
    };

    public SharedDatabaseErrorTab(@Nullable String sharedDatabaseId, DBMSConnectionProperties connectionProperties) {
        this.sharedDatabaseId = sharedDatabaseId;
        this.connectionProperties = connectionProperties;
        // In expert mode the database name may be empty: the JDBC URL is all the user entered
        String databaseName = connectionProperties.getDatabase().isBlank()
                ? connectionProperties.getJdbcUrl()
                : connectionProperties.getDatabase();

        setText(databaseName);
        setGraphic(IconTheme.JabRefIcons.ERROR.getGraphicNode());

        message.setId(MESSAGE_ID);
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        retryButton.setDefaultButton(true);
        // A retry swaps this tab for a fresh loading tab: closing that tab cancels the attempt, so a pending attempt
        // can never resurrect a placeholder the user already dismissed.
        retryButton.setOnAction(_ -> {
            retryButton.setDisable(true);
            Optional.ofNullable(getTabPane()).ifPresent(tabPane -> tabPane.getTabs().remove(this));
            retryAction.run();
        });

        Label header = new Label(sharedDatabaseId == null
                                 ? Localization.lang("Could not connect to %0", databaseName)
                                 : Localization.lang("Could not reconnect to shared database %0.", databaseName));
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

    public Optional<String> getSharedDatabaseId() {
        return Optional.ofNullable(sharedDatabaseId);
    }

    public DBMSConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    public void showError(Throwable exception) {
        message.setText(Optional.ofNullable(exception.getMessage()).orElseGet(exception::toString));
        retryButton.setDisable(false);
    }
}

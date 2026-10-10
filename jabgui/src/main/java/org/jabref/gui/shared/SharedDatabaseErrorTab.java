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
import org.jabref.logic.shared.DBMSConnectionUrl;

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
    private @Nullable Throwable error;

    public SharedDatabaseErrorTab(@Nullable String sharedDatabaseId, DBMSConnectionProperties connectionProperties) {
        this.sharedDatabaseId = sharedDatabaseId;
        this.connectionProperties = connectionProperties;
        String databaseName = displayName(connectionProperties);

        setText(databaseName);
        setGraphic(IconTheme.JabRefIcons.ERROR.getGraphicNode());

        message.setId(MESSAGE_ID);
        message.setWrapText(true);
        message.setMaxWidth(600);
        message.setTextAlignment(TextAlignment.CENTER);
        retryButton.setDefaultButton(true);
        retryButton.setOnAction(_ -> retryAction.run());

        Label header = new Label(sharedDatabaseId == null
                                 ? Localization.lang("Could not connect to %0.", databaseName)
                                 : Localization.lang("Could not reconnect to shared database %0.", databaseName));
        header.setWrapText(true);
        header.getStyleClass().addAll(StyleClasses.WELCOME_HEADER);

        VBox content = new VBox(10, header, message, retryButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        setContent(content);
    }

    /// In expert mode the connection uses the JDBC URL, not the database field (which may hold a stale name or, for
    /// incomplete remembered preferences, none at all). The URL itself is never shown, it may carry the password;
    /// only its database or host is.
    private static String displayName(DBMSConnectionProperties connectionProperties) {
        Optional<String> fromUrl = connectionProperties.isUseExpertMode()
                                   ? DBMSConnectionUrl.parse(connectionProperties.getJdbcUrl())
                                                      .map(url -> url.database().isBlank() ? url.host() : url.database())
                                   : Optional.empty();
        return fromUrl.or(() -> Optional.ofNullable(connectionProperties.getDatabase()))
                      .filter(name -> !name.isBlank())
                      .orElseGet(() -> Localization.lang("Shared database connection"));
    }

    public void setRetryAction(Runnable retryAction) {
        this.retryAction = retryAction;
    }

    public Optional<String> getSharedDatabaseId() {
        return Optional.ofNullable(sharedDatabaseId);
    }

    /// Called once a new attempt for this database has started: the loading tab takes this tab's place. Removing the
    /// tab only then keeps it when the attempt is cancelled before it starts (e.g. a declined overwrite confirmation).
    public void close() {
        Optional.ofNullable(getTabPane()).ifPresent(tabPane -> tabPane.getTabs().remove(this));
    }

    public DBMSConnectionProperties getConnectionProperties() {
        return connectionProperties;
    }

    public void showError(Throwable exception) {
        this.error = exception;
        message.setText(Optional.ofNullable(exception.getMessage()).orElseGet(exception::toString));
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }
}

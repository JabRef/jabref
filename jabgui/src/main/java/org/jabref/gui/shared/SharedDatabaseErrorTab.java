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

    private @Nullable String sharedDatabaseId;
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
        retryButton.setOnAction(_ -> retryAction.run());

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

    /// A dialog attempt for a remembered database that fails again inherits the id, so the database stays remembered
    public void rememberAs(String sharedDatabaseId) {
        this.sharedDatabaseId = sharedDatabaseId;
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
        message.setText(Optional.ofNullable(exception.getMessage()).orElseGet(exception::toString));
    }
}

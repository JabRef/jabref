package org.jabref.gui.preferences.forms;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

/// Custom editor: a bound text field followed by a narrow "browse" icon button.
///
/// This is the single most duplicated block across the preference tabs (theme path, backup
/// directory, main file directory, ...). Encapsulating it here removes the repeated FXML +
/// controller wiring. The browse button's disabled state follows the text field, so binding
/// `field().disableProperty()` (via [PreferencesFormBuilder.disableWhen]) disables the whole row.
public final class BrowseFileEditor {

    /// Tight gap between a field and the icon button that belongs to it.
    private static final double GAP = 4.0;

    /// @param row   the assembled `[TextField][browse]` row to place in the form
    /// @param field the text field, exposed so callers can attach validation/disable bindings
    public record Result(HBox row, TextField field) {
    }

    private BrowseFileEditor() {
    }

    public static Result create(StringProperty value, Runnable onBrowse) {
        TextField field = new TextField();
        field.textProperty().bindBidirectional(value);
        HBox.setHgrow(field, Priority.ALWAYS);

        Button browse = new Button();
        browse.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        browse.getStyleClass().addAll("icon-button", "narrow");
        browse.setPrefSize(20.0, 20.0);
        browse.setTooltip(new Tooltip(Localization.lang("Browse")));
        browse.disableProperty().bind(field.disableProperty());
        browse.setOnAction(_ -> onBrowse.run());

        HBox row = new HBox(GAP, field, browse);
        row.setAlignment(Pos.CENTER_LEFT);
        return new Result(row, field);
    }
}

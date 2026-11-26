package org.jabref.gui.preferences.forms;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.control.ToggleSwitch;
import org.jspecify.annotations.NullMarked;

/// One setting as a row of its own: the caption (with optional description lines beneath) on the
/// left, then the controls. Below that row, a tile can hold details: settings that only apply while
/// its switch is on, shown only then.
///
/// Controls start at a common line, the end of a caption column of fixed width, so that the
/// controls of all tiles line up whatever their captions; a control that shows more than a single
/// value grows from there to the end of the row. A switch, or the chevron of a tile that expands,
/// is no control of that kind: it sits at the end of the row, and a tile with nothing else lets its
/// caption take the whole row.
///
/// The caption follows the control's disabled state, so that a setting disabled by a condition
/// reads as disabled as a whole and not only at its control. A switch tile toggles when clicked
/// anywhere outside its controls, as the whole row is the setting.
@NullMarked
final class SettingTile extends VBox {

    static final String STYLE_CLASS = "setting-tile";

    private final Label title;
    private final VBox captions;
    private final HBox controls = new HBox(FormMetrics.GAP);
    private final HBox header;
    private final VBox details = new VBox(FormMetrics.GAP);
    private final Node control;

    SettingTile(String titleText, Node control) {
        this.control = control;
        getStyleClass().add(STYLE_CLASS);

        title = new Label(titleText);
        title.getStyleClass().add("setting-tile-title");
        followControl(title);

        captions = new VBox(2, title);
        captions.setAlignment(Pos.CENTER_LEFT);

        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(controls, Priority.ALWAYS);

        header = new HBox(FormMetrics.TILE_HEADER_GAP, captions, controls);
        header.getStyleClass().add("setting-tile-header");
        header.setAlignment(Pos.CENTER_LEFT);

        details.getStyleClass().add("setting-tile-details");
        details.managedProperty().bind(details.visibleProperty());
        details.setVisible(false);

        getChildren().addAll(header, details);

        controls.getChildren().addListener((ListChangeListener<Node>) _ -> fitCaption());
        control.getProperties().put(SettingTile.class, this);
        if (control instanceof ToggleSwitch toggle) {
            header.getChildren().add(toggle);
            header.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (!toggle.isDisabled() && !hitsControls(event)) {
                    toggle.setSelected(!toggle.isSelected());
                }
            });
        } else {
            controls.getChildren().add(control);
        }
        fitCaption();
    }

    Label title() {
        return title;
    }

    Node control() {
        return control;
    }

    /// What sits beside the caption: the control (unless it is a switch) and the attachments.
    List<Node> controls() {
        return controls.getChildren();
    }

    /// Where the settings go that depend on this tile's switch.
    VBox details() {
        return details;
    }

    /// Shows the details only while the switch is on.
    void revealDetailsWhenOn(ToggleSwitch toggle) {
        details.visibleProperty().bind(toggle.selectedProperty().and(Bindings.isNotEmpty(details.getChildren())));
    }

    /// Shows the details whether the switch is on or off, but enables them only while it is on, so
    /// that one sees what turning it on offers.
    void enableDetailsWhenOn(ToggleSwitch toggle) {
        details.visibleProperty().bind(Bindings.isNotEmpty(details.getChildren()));
        details.disableProperty().bind(toggle.selectedProperty().not());
    }

    /// Makes the row open and close the details on a click, with a chevron at the end of the row
    /// showing which way: for a setting whose options need more room than fits in the row, such as
    /// a gallery to pick from.
    void expandOnClick() {
        BooleanProperty expanded = new SimpleBooleanProperty(false);
        Node chevron = IconTheme.JabRefIcons.EXPAND.getGraphicNode();
        chevron.rotateProperty().bind(Bindings.when(expanded).then(180).otherwise(0));
        header.getChildren().add(chevron);
        header.getStyleClass().add("setting-tile-expander");
        header.addEventHandler(MouseEvent.MOUSE_CLICKED, _ -> expanded.set(!expanded.get()));
        details.visibleProperty().bind(expanded);
    }

    void addDescription(Label description) {
        description.getStyleClass().add("setting-tile-description");
        followControl(description);
        captions.getChildren().add(description);
    }

    /// Places `attachment` after what the row's controls hold so far, so attachments follow in the
    /// order they were attached. An attachment to a switch goes there too, so that it starts at the
    /// common line and the switch stays at the end of the row.
    void attach(Node attachment) {
        attachment.getProperties().put(SettingTile.class, this);
        controls.getChildren().add(attachment);
    }

    /// The tile `node` sits in as its control or as one of the attachments beside it. Each tile
    /// registers these with itself, so the answer does not depend on how the tile nests its rows.
    static Optional<SettingTile> holding(Node node) {
        return Optional.ofNullable(node.getProperties().get(SettingTile.class))
                       .map(SettingTile.class::cast);
    }

    /// With controls in the row, the caption is a column of fixed width, so that the controls start
    /// at the same line in every tile; with only a switch, the caption takes the whole row.
    private void fitCaption() {
        boolean hasControls = !controls.getChildren().isEmpty();
        double width = hasControls ? FormMetrics.TILE_CAPTION_COLUMN_WIDTH : Region.USE_COMPUTED_SIZE;
        captions.setMinWidth(hasControls ? width : 0);
        captions.setPrefWidth(width);
        captions.setMaxWidth(hasControls ? width : Double.MAX_VALUE);
        HBox.setHgrow(captions, hasControls ? Priority.NEVER : Priority.ALWAYS);
        controls.setVisible(hasControls);
        controls.setManaged(hasControls);
    }

    /// Whether the click went to a control itself. The controls' row stretches up to the switch, so
    /// a click on its empty part is a click on the tile and toggles it.
    private boolean hitsControls(MouseEvent event) {
        return Stream.iterate(event.getPickResult().getIntersectedNode(), Objects::nonNull, Node::getParent)
                     .anyMatch(node -> node == control || node.getParent() == controls);
    }

    private void followControl(Label caption) {
        caption.setWrapText(true);
        caption.setMaxWidth(Double.MAX_VALUE);
        caption.disableProperty().bind(control.disabledProperty());
    }
}

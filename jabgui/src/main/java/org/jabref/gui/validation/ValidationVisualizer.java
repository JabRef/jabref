package org.jabref.gui.validation;

import java.util.List;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Popup;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;

import org.jfxcore.validation.ValidationState;
import org.jfxcore.validation.property.ReadOnlyConstrainedProperty;

/// Shows the highest-severity {@link ValidationMessage} of a {@link ReadOnlyConstrainedProperty} as a small
/// icon anchored next to a control, replacing ControlsFX's `Decorator`-based validation decoration
/// (`ControlsFxVisualizer` + JabRef's old `IconValidationDecorator`).
///
/// ControlsFX's decoration mechanism inserts an overlay node as a sibling of the decorated control in its
/// parent, and tracks the overlay's position via bounds listeners for *every* decorated control regardless of
/// whether it is currently valid. That is both a performance concern and unsafe for controls that are direct
/// children of a `GridPane` (inserting a wrapper node would break `GridPane.rowIndex`/`columnIndex`
/// attachments). This visualizer instead uses an owned [Popup], which never touches the control's parent, and
/// only tracks position while a message is actually showing (i.e., while the control is invalid).
public class ValidationVisualizer {

    private final Pos position;

    public ValidationVisualizer() {
        this(Pos.CENTER_LEFT);
    }

    public ValidationVisualizer(Pos position) {
        this.position = position;
    }

    public void initVisualization(ReadOnlyConstrainedProperty<?, ValidationMessage> validation, Control control) {
        Popup popup = new Popup();
        popup.setHideOnEscape(false);

        Label icon = new Label();
        icon.setMaxHeight(Control.USE_PREF_SIZE);
        popup.getContent().setAll(icon);

        ValidationState.setSource(control, validation);

        InvalidationListener reposition = observable -> reposition(control, popup, icon);

        Runnable show = () -> {
            if (popup.isShowing() || control.getScene() == null || control.getScene().getWindow() == null) {
                return;
            }
            popup.show(control, 0, 0);
            control.boundsInLocalProperty().addListener(reposition);
            control.localToSceneTransformProperty().addListener(reposition);
            Window window = control.getScene().getWindow();
            window.xProperty().addListener(reposition);
            window.yProperty().addListener(reposition);
            reposition(control, popup, icon);
        };

        Runnable hide = () -> {
            if (!popup.isShowing()) {
                return;
            }
            control.boundsInLocalProperty().removeListener(reposition);
            control.localToSceneTransformProperty().removeListener(reposition);
            Scene scene = control.getScene();
            if (scene != null && scene.getWindow() != null) {
                scene.getWindow().xProperty().removeListener(reposition);
                scene.getWindow().yProperty().removeListener(reposition);
            }
            popup.hide();
        };

        Runnable update = () -> highestMessage(validation).ifPresentOrElse(message -> {
            applyIcon(icon, message);
            show.run();
        }, hide);

        validation.getDiagnostics().addListener((ListChangeListener<ValidationMessage>) change -> update.run());

        // The control may not yet be attached to a showing window when this is called (e.g. a dialog that
        // hasn't been shown yet) — retry once it is.
        control.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> update.run());
            }
            update.run();
        });

        update.run();
    }

    private void reposition(Control control, Popup popup, Node icon) {
        if (control.getScene() == null || control.getScene().getWindow() == null) {
            return;
        }
        Bounds bounds = control.localToScreen(control.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        double width = icon.prefWidth(-1);
        double height = icon.prefHeight(-1);
        double x = switch (position.getHpos()) {
            case LEFT -> bounds.getMinX();
            case CENTER -> bounds.getMinX() + (bounds.getWidth() - width) / 2;
            case RIGHT -> bounds.getMaxX() - width;
        };
        double y = switch (position.getVpos()) {
            case TOP -> bounds.getMinY();
            case CENTER, BASELINE -> bounds.getMinY() + (bounds.getHeight() - height) / 2;
            case BOTTOM -> bounds.getMaxY() - height;
        };
        popup.setX(x);
        popup.setY(y);
    }

    void applyIcon(Label icon, ValidationMessage message) {
        boolean error = message.severity() == Severity.ERROR;
        Node graphic = error ? IconTheme.JabRefIcons.ERROR.getGraphicNode() : IconTheme.JabRefIcons.WARNING.getGraphicNode();
        graphic.getStyleClass().add(error ? "error-icon" : "warning-icon");

        Tooltip tooltip = new Tooltip(message.message());
        tooltip.getStyleClass().add(error ? "tooltip-error" : "tooltip-warning");

        icon.setGraphic(graphic);
        icon.setTooltip(tooltip);
    }

    public static Optional<ValidationMessage> highestMessage(ReadOnlyConstrainedProperty<?, ValidationMessage> validation) {
        List<ValidationMessage> invalid = validation.getDiagnostics().invalidSubList();
        return invalid.stream().filter(message -> message.severity() == Severity.ERROR).findFirst()
                      .or(() -> invalid.stream().findFirst());
    }
}

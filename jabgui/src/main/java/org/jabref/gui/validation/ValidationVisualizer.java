package org.jabref.gui.validation;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
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
import org.jspecify.annotations.NullMarked;

/// Shows the highest-severity [ValidationMessage] of a [ReadOnlyConstrainedProperty] as a small
/// icon anchored next to a control, replacing ControlsFX's `Decorator`-based validation decoration
/// (`ControlsFxVisualizer` + JabRef's old `IconValidationDecorator`).
///
/// ControlsFX's decoration mechanism inserts an overlay node as a sibling of the decorated control in its
/// parent, and tracks the overlay's position via bounds listeners for *every* decorated control regardless of
/// whether it is currently valid. That is both a performance concern and unsafe for controls that are direct
/// children of a `GridPane` (inserting a wrapper node would break `GridPane.rowIndex`/`columnIndex`
/// attachments). This visualizer instead uses an owned [Popup], which never touches the control's parent, and
/// only tracks position while a message is actually showing (i.e., while the control is invalid).
///
/// See `docs/decisions/0065-use-jfxcore-validation.md` for the decision to replace ControlsFX/mvvmfx-validation
/// with `org.jfxcore:validation`.
@NullMarked
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

        // Tracks the window whose x/y listeners are currently registered, so hide() can clean them up even
        // after the control has already been detached from its scene (control.getScene() would be null by then).
        Window[] attachedWindow = new Window[1];

        InvalidationListener reposition = observable -> reposition(control, popup, icon);

        Runnable show = () -> {
            if (popup.isShowing() || control.getScene() == null || control.getScene().getWindow() == null) {
                return;
            }
            popup.show(control, 0, 0);
            control.boundsInLocalProperty().addListener(reposition);
            control.localToSceneTransformProperty().addListener(reposition);
            attachedWindow[0] = control.getScene().getWindow();
            attachedWindow[0].xProperty().addListener(reposition);
            attachedWindow[0].yProperty().addListener(reposition);
            reposition(control, popup, icon);
        };

        Runnable hide = () -> {
            if (!popup.isShowing()) {
                return;
            }
            control.boundsInLocalProperty().removeListener(reposition);
            control.localToSceneTransformProperty().removeListener(reposition);
            if (attachedWindow[0] != null) {
                attachedWindow[0].xProperty().removeListener(reposition);
                attachedWindow[0].yProperty().removeListener(reposition);
                attachedWindow[0] = null;
            }
            popup.hide();
        };

        Runnable update = () -> {
            if (control.getScene() == null || control.getScene().getWindow() == null) {
                // The control is detached (e.g. its dialog was closed) — always hide, regardless of validity,
                // instead of leaving a popup owned by a no-longer-attached control on screen.
                hide.run();
                return;
            }
            highestMessage(validation).ifPresentOrElse(message -> {
                applyIcon(icon, message);
                show.run();
            }, hide);
        };

        // The constrained property backing validation may be mutated off the FX thread (e.g. a background save
        // action touching the same view-model property), so the diagnostics listener below can fire off-thread
        // too — marshal it back before touching the popup/icon.
        ListChangeListener<ValidationMessage> diagnosticsListener = change -> runOnFxThread(update);
        validation.getDiagnostics().addListener(diagnosticsListener);

        // validation is typically a property owned by a ViewModel that outlives this View, so the listener
        // above would otherwise keep control (and this whole View) reachable forever. Drop it — together with
        // the popup and its position listeners — once the control's window closes, so the View can be
        // collected like any other closed dialog/tab.
        ChangeListener<Boolean> windowShowingListener = (observable, wasShowing, isShowing) -> {
            if (!isShowing) {
                hide.run();
                validation.getDiagnostics().removeListener(diagnosticsListener);
            }
        };

        ChangeListener<Window> windowChangeListener = (observable, oldWindow, newWindow) -> {
            if (oldWindow != null) {
                oldWindow.showingProperty().removeListener(windowShowingListener);
            }
            if (newWindow != null) {
                newWindow.showingProperty().addListener(windowShowingListener);
            }
            update.run();
        };

        // The control may not yet be attached to a (showing) window when this is called (e.g. a dialog that
        // hasn't been shown yet, or whose Scene hasn't been assigned to a Window yet) — track both the scene
        // and the window so the popup and the disposal hook above are (de)registered whenever either changes.
        control.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.windowProperty().removeListener(windowChangeListener);
                if (oldScene.getWindow() != null) {
                    oldScene.getWindow().showingProperty().removeListener(windowShowingListener);
                }
            }
            if (newScene != null) {
                newScene.windowProperty().addListener(windowChangeListener);
                if (newScene.getWindow() != null) {
                    newScene.getWindow().showingProperty().addListener(windowShowingListener);
                }
            }
            update.run();
        });

        // Bootstrap for the (common) case where control is already attached to a scene/window before this
        // method runs.
        Scene currentScene = control.getScene();
        if (currentScene != null) {
            currentScene.windowProperty().addListener(windowChangeListener);
            if (currentScene.getWindow() != null) {
                currentScene.getWindow().showingProperty().addListener(windowShowingListener);
            }
        }

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
            case LEFT ->
                    bounds.getMinX();
            case CENTER ->
                    bounds.getMinX() + (bounds.getWidth() - width) / 2;
            case RIGHT ->
                    bounds.getMaxX() - width;
        };
        double y = switch (position.getVpos()) {
            case TOP ->
                    bounds.getMinY();
            case CENTER,
                 BASELINE ->
                    bounds.getMinY() + (bounds.getHeight() - height) / 2;
            case BOTTOM ->
                    bounds.getMaxY() - height;
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

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}

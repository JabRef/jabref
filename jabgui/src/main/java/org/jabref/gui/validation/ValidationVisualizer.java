package org.jabref.gui.validation;

import java.util.List;
import java.util.Optional;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
/// See `docs/decisions/0066-use-jfxcore-validation.md` for the decision to replace ControlsFX/mvvmfx-validation
/// with `org.jfxcore:validation`.
@NullMarked
public class ValidationVisualizer {

    private static final double INSET = 2;

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

        // Created once and reused across updates instead of allocating a new graphic/Tooltip on every
        // validation change (e.g. on every keystroke) — only their text/style class need to change.
        Node errorGraphic = IconTheme.JabRefIcons.ERROR.getGraphicNode();
        errorGraphic.getStyleClass().add("error-icon");
        Node warningGraphic = IconTheme.JabRefIcons.WARNING.getGraphicNode();
        warningGraphic.getStyleClass().add("warning-icon");
        Tooltip tooltip = new Tooltip();
        icon.setTooltip(tooltip);

        ValidationState.setSource(control, validation);

        // Tracks whether the decoration is meant to be visible (i.e. show() has run and hide() hasn't), which
        // may be true even before the popup is actually showing: popup.show() is deferred, inside the timer
        // below, until the control's on-screen bounds can actually be resolved.
        boolean[] active = {false};

        // Repositions every pulse while active, instead of listening to control.boundsInLocal/
        // localToSceneTransform: those don't reliably invalidate when an ancestor ScrollPane scrolls (it
        // translates its content internally), which left the icon stuck in place while the field scrolled
        // underneath it. Also responsible for showing the popup in the first place, once bounds are
        // resolvable, and for hiding it again if bounds become unresolvable while it is showing (rather than
        // leaving it at stale coordinates).
        AnimationTimer repositionTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                computeLocation(control, icon).ifPresentOrElse(
                        location -> {
                            if (popup.isShowing()) {
                                popup.setX(location.getX());
                                popup.setY(location.getY());
                            } else {
                                popup.show(control, location.getX(), location.getY());
                            }
                        },
                        () -> {
                            if (popup.isShowing()) {
                                popup.hide();
                            }
                        });
            }
        };

        Runnable show = () -> {
            if (active[0]) {
                return;
            }
            Window window = control.getScene() == null ? null : control.getScene().getWindow();
            if (window == null || !window.isShowing()) {
                return;
            }
            active[0] = true;
            repositionTimer.start();
        };

        Runnable hide = () -> {
            if (!active[0]) {
                return;
            }
            active[0] = false;
            repositionTimer.stop();
            if (popup.isShowing()) {
                popup.hide();
            }
        };

        Runnable update = () -> {
            if (control.getScene() == null || control.getScene().getWindow() == null) {
                // The control is detached (e.g. its dialog was closed) — always hide, regardless of validity,
                // instead of leaving a popup owned by a no-longer-attached control on screen.
                hide.run();
                return;
            }
            highestMessage(validation).ifPresentOrElse(message -> {
                applyIcon(icon, errorGraphic, warningGraphic, tooltip, message);
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
            if (isShowing) {
                // The control may have been attached to a Window before that window was actually shown (e.g.
                // a dialog's initialize() runs before the dialog itself is displayed) — re-run update() now
                // that show() can actually succeed.
                update.run();
            } else {
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

    /// Computes the screen location the decoration should be shown at, or [Optional#empty] if the control's
    /// bounds cannot currently be resolved to screen coordinates (e.g. mid-layout, or not yet attached to a
    /// showing window).
    private Optional<Point2D> computeLocation(Control control, Node icon) {
        if (control.getScene() == null || control.getScene().getWindow() == null) {
            return Optional.empty();
        }
        Bounds bounds = control.localToScreen(control.getBoundsInLocal());
        if (bounds == null || !isWithinScrollableViewport(control, bounds)) {
            return Optional.empty();
        }
        double width = icon.prefWidth(-1);
        double height = icon.prefHeight(-1);
        // LEFT/RIGHT place the icon just outside the control (beside it, not over its content);
        // CENTER/TOP/BOTTOM still anchor inside, inset off the border so they don't straddle it.
        double x = switch (position.getHpos()) {
            case LEFT ->
                    bounds.getMinX() - width - INSET;
            case CENTER ->
                    bounds.getMinX() + (bounds.getWidth() - width) / 2;
            case RIGHT ->
                    bounds.getMaxX() + INSET;
        };
        double y = switch (position.getVpos()) {
            case TOP ->
                    bounds.getMinY() + INSET;
            case CENTER,
                 BASELINE ->
                    bounds.getMinY() + (bounds.getHeight() - height) / 2;
            case BOTTOM ->
                    bounds.getMaxY() - height - INSET;
        };
        return Optional.of(new Point2D(x, y));
    }

    /// Whether `control`'s on-screen bounds are (at least partially) inside the visible viewport of its
    /// nearest ancestor [ScrollPane], if any. A scrolled-out control is still attached and its bounds still
    /// resolve via [Control#localToScreen], so without this check the decoration would keep following it off
    /// into unrelated UI (e.g. the entry editor's tab bar) instead of hiding while it is scrolled out of view.
    private boolean isWithinScrollableViewport(Control control, Bounds controlScreenBounds) {
        for (Node parent = control.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane scrollPane) {
                Bounds viewportScreenBounds = scrollPane.localToScreen(scrollPane.getBoundsInLocal());
                return viewportScreenBounds != null && viewportScreenBounds.intersects(controlScreenBounds);
            }
        }
        return true;
    }

    void applyIcon(Label icon, Node errorGraphic, Node warningGraphic, Tooltip tooltip, ValidationMessage message) {
        boolean error = message.severity() == Severity.ERROR;
        icon.setGraphic(error ? errorGraphic : warningGraphic);

        tooltip.setText(message.message());
        tooltip.getStyleClass().removeAll("tooltip-error", "tooltip-warning");
        tooltip.getStyleClass().add(error ? "tooltip-error" : "tooltip-warning");
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

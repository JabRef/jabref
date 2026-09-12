package org.jabref.gui.walkthrough;

import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import org.jspecify.annotations.NullMarked;

/// The layer a walkthrough draws into: an initially empty pane covering a whole window, on top of that
/// window's regular content. Panels, tooltips and highlight effects are added here and removed again.
///
/// A window adds its pane where it is built and keeps it for its lifetime. The pane is a child of a
/// parent the window already has, never a replacement for the scene root -- three parties already
/// claim that root (JavaFX's [javafx.scene.control.Dialog] reassigns it on every show, ControlsFX injects
/// its decoration pane on the first validation decoration, and the walkthrough used to wrap it), and any
/// two of them colliding drops the third's contribution. Replacing the root of a visible window also
/// invalidates the CSS of the entire scene graph and makes Scenic View re-attach from scratch, losing the
/// developer's selection.
///
/// @implNote The pane is unmanaged and sizes itself to the scene rather than to its parent, so that one
/// rule covers every host: a [Pane] that is the scene root, a pane that fills it, and the content of a
/// popup alike. This assumes the host's origin coincides with the scene's, which holds for all three.
/// [javafx.scene.Parent#layout()] descends into unmanaged children, so the pane still lays out its own.
@NullMarked
public final class WalkthroughPane extends StackPane {

    /// Keyed into the scene's property map, so [#of(Window)] is a lookup instead of a scene-graph search
    /// and nothing outlives the scene that holds the pane. A window has at most one pane.
    private static final Object SCENE_PROPERTY_KEY = new Object();

    /// A lower view order renders a child in front of its siblings, whatever its position in the list.
    /// A [javafx.scene.control.DialogPane] appends its header, content and button bar as they are first
    /// needed, so position in the list is not something the pane can hold on to.
    private static final double IN_FRONT_OF_SIBLINGS = -1;

    public WalkthroughPane() {
        getStyleClass().add("walkthrough-pane");
        setMinSize(0, 0);
        setManaged(false);
        setViewOrder(IN_FRONT_OF_SIBLINGS);
        // While the pane holds nothing, it covers the window without any reason to receive input.
        mouseTransparentProperty().bind(Bindings.isEmpty(getChildren()));

        InvalidationListener fitToScene = _ -> fitToScene();
        sceneProperty().addListener((_, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.getProperties().remove(SCENE_PROPERTY_KEY);
                oldScene.widthProperty().removeListener(fitToScene);
                oldScene.heightProperty().removeListener(fitToScene);
            }
            if (newScene != null) {
                newScene.getProperties().put(SCENE_PROPERTY_KEY, this);
                newScene.widthProperty().addListener(fitToScene);
                newScene.heightProperty().addListener(fitToScene);
            }
            fitToScene();
        });
    }

    /// Returns the pane of the given window.
    ///
    /// The main window and every JabRef dialog are given theirs where they are built. A popup -- a context
    /// menu a walkthrough steps into -- is not JabRef's to build, so it is given one here, the first time
    /// a walkthrough draws on it. Empty for any other window built outside JabRef.
    public static Optional<WalkthroughPane> of(Window window) {
        Optional<WalkthroughPane> existing = Optional.ofNullable(window.getScene())
                                                     .map(scene -> scene.getProperties().get(SCENE_PROPERTY_KEY))
                                                     .map(WalkthroughPane.class::cast);
        if (existing.isPresent() || !(window instanceof PopupWindow)) {
            return existing;
        }

        return Optional.ofNullable(window.getScene())
                       .map(Scene::getRoot)
                       .flatMap(WalkthroughPane::childrenOf)
                       .map(children -> {
                           WalkthroughPane pane = new WalkthroughPane();
                           children.add(pane);
                           return pane;
                       });
    }

    /// The two parents that take arbitrary children -- the same pair [PopupWindow] itself accepts as a
    /// popup's root. Its own content list is not public beyond [javafx.stage.Popup], and a context menu is
    /// a [javafx.scene.control.PopupControl].
    private static Optional<ObservableList<Node>> childrenOf(Parent parent) {
        return switch (parent) {
            case Group group ->
                    Optional.of(group.getChildren());
            case Pane pane ->
                    Optional.of(pane.getChildren());
            default ->
                    Optional.empty();
        };
    }

    private void fitToScene() {
        Scene scene = getScene();
        if (scene != null) {
            resizeRelocate(0, 0, scene.getWidth(), scene.getHeight());
        }
    }
}

package org.jabref.gui.walkthrough;

import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.util.BackdropHighlight;
import org.jabref.gui.walkthrough.declarative.step.FullScreenStep;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display a walkthrough overlay on top of the main application window.
 */
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);
    private final Stage parentStage;
    private final Walkthrough manager;
    private final GridPane overlayPane;
    private final BackdropHighlight backdropHighlight;
    private final Pane originalRoot;
    private final StackPane stackPane;
    private final WalkthroughRenderer uiFactory;

    public WalkthroughOverlay(Stage stage, Walkthrough manager) {
        this.parentStage = stage;
        this.manager = manager;
        this.uiFactory = new WalkthroughRenderer();

        overlayPane = new GridPane();
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setVisible(false);
        overlayPane.setMaxWidth(Double.MAX_VALUE);
        overlayPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = stage.getScene();
        assert scene != null;

        originalRoot = (Pane) scene.getRoot();
        stackPane = new StackPane();

        stackPane.getChildren().add(originalRoot);
        backdropHighlight = new BackdropHighlight(stackPane);
        stackPane.getChildren().add(overlayPane);

        scene.setRoot(stackPane);
    }

    public void displayStep(WalkthroughNode step) {
        if (step == null) {
            hide();
            return;
        }

        show();

        backdropHighlight.detach();
        overlayPane.getChildren().clear();

        Node stepContent;
        if (step instanceof FullScreenStep fullScreenStep) {
            stepContent = uiFactory.render(fullScreenStep, manager);
            displayFullScreenContent(stepContent);
        } else if (step instanceof PanelStep panelStep) {
            stepContent = uiFactory.render(panelStep, manager);
            displayPanelContent(stepContent, panelStep.position());

            if (step.resolver().isPresent()) {
                Optional<Node> targetNodeOpt = step.resolver().get().apply(parentStage.getScene());
                if (targetNodeOpt.isPresent()) {
                    Node targetNode = targetNodeOpt.get();
                    backdropHighlight.attach(targetNode);
                    step.clickOnNodeAction().ifPresent(action ->
                            targetNode.setOnMouseClicked(_ -> action.accept(manager)));
                } else {
                    LOGGER.warn(Localization.lang("Could not resolve target node for step", step.title()));
                }
            }
        }
    }

    private void displayFullScreenContent(Node content) {
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(content);

        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setVgrow(Priority.ALWAYS);
        overlayPane.getRowConstraints().add(rowConstraints);
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setHgrow(Priority.ALWAYS);
        overlayPane.getColumnConstraints().add(columnConstraints);

        GridPane.setHgrow(content, Priority.ALWAYS);
        GridPane.setVgrow(content, Priority.ALWAYS);
        GridPane.setFillWidth(content, true);
        GridPane.setFillHeight(content, true);

        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setVisible(true);
    }

    private void displayPanelContent(Node panelContent, Pos position) {
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(panelContent);
        panelContent.setMouseTransparent(false);

        overlayPane.getRowConstraints().clear();
        overlayPane.getColumnConstraints().clear();

        GridPane.setHgrow(panelContent, Priority.NEVER);
        GridPane.setVgrow(panelContent, Priority.NEVER);
        GridPane.setFillWidth(panelContent, false);
        GridPane.setFillHeight(panelContent, false);

        RowConstraints rowConstraints = new RowConstraints();
        ColumnConstraints columnConstraints = new ColumnConstraints();

        overlayPane.setAlignment(position);

        switch (position) {
            case CENTER_LEFT:
            case CENTER_RIGHT:
                rowConstraints.setVgrow(Priority.ALWAYS);
                columnConstraints.setHgrow(Priority.NEVER);
                GridPane.setFillHeight(panelContent, true);
                break;
            case TOP_CENTER:
            case BOTTOM_CENTER:
                columnConstraints.setHgrow(Priority.ALWAYS);
                rowConstraints.setVgrow(Priority.NEVER);
                GridPane.setFillWidth(panelContent, true);
                break;
            default:
                LOGGER.warn("Unsupported position for panel step: {}", position);
                break;
        }

        overlayPane.getRowConstraints().add(rowConstraints);
        overlayPane.getColumnConstraints().add(columnConstraints);
        overlayPane.setVisible(true);
    }

    /**
     * Detaches the overlay and cleans up resources.
     */
    public void detach() {
        backdropHighlight.detach();

        overlayPane.setVisible(false);
        overlayPane.getChildren().clear();

        Scene scene = parentStage.getScene();
        if (scene != null && originalRoot != null) {
            stackPane.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
            LOGGER.debug("Restored original scene root: {}", originalRoot.getClass().getName());
        }
    }

    private void show() {
        overlayPane.setVisible(true);
        overlayPane.toFront();
    }

    private void hide() {
        overlayPane.setVisible(false);
    }
}

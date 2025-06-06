package org.jabref.gui.walkthrough;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.richtext.ArbitraryJFXBlock;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.FullScreenStep;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
import org.jabref.logic.l10n.Localization;

/**
 * Renders the walkthrough steps and content blocks into JavaFX Nodes.
 */
public class WalkthroughRenderer {
    public Node render(FullScreenStep step, Walkthrough manager) {
        VBox container = makePanel();
        container.setAlignment(Pos.CENTER);
        VBox content = new VBox();
        content.getStyleClass().add("walkthrough-fullscreen-content");
        Label titleLabel = new Label(Localization.lang(step.title()));
        titleLabel.getStyleClass().add("walkthrough-title");
        VBox contentContainer = makeContent(step, manager);
        content.getChildren().addAll(titleLabel, contentContainer, makeActions(step, manager));
        container.getChildren().add(content);
        return container;
    }

    public Node render(PanelStep step, Walkthrough manager) {
        VBox panel = makePanel();

        if (step.position() == Pos.CENTER_LEFT || step.position() == Pos.CENTER_RIGHT) {
            panel.getStyleClass().add("walkthrough-side-panel-vertical");
            VBox.setVgrow(panel, Priority.ALWAYS);
            panel.setMaxHeight(Double.MAX_VALUE);
        } else if (step.position() == Pos.TOP_CENTER || step.position() == Pos.BOTTOM_CENTER) {
            panel.getStyleClass().add("walkthrough-side-panel-horizontal");
            HBox.setHgrow(panel, Priority.ALWAYS);
            panel.setMaxWidth(Double.MAX_VALUE);
        }

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(Localization.lang(step.title()));
        titleLabel.getStyleClass().add("walkthrough-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stepCounter = new Label(Localization.lang("Step of",
                String.valueOf(manager.currentStepProperty().get() + 1),
                String.valueOf(manager.totalStepsProperty().get())));
        stepCounter.getStyleClass().add("walkthrough-step-counter");

        header.getChildren().addAll(titleLabel, spacer, stepCounter);

        VBox contentContainer = makeContent(step, manager);
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        HBox actions = makeActions(step, manager);
        panel.getChildren().addAll(header, contentContainer, bottomSpacer, actions);

        return panel;
    }

    public Node render(ArbitraryJFXBlock block, Walkthrough manager) {
        return block.componentFactory().apply(manager);
    }

    public Node render(TextBlock textBlock) {
        Label textLabel = new Label(Localization.lang(textBlock.text()));
        textLabel.getStyleClass().add("walkthrough-text-content");
        return textLabel;
    }

    public Node render(InfoBlock infoBlock) {
        HBox infoContainer = new HBox();
        infoContainer.getStyleClass().add("walkthrough-info-container");
        JabRefIconView icon = new JabRefIconView(IconTheme.JabRefIcons.INTEGRITY_INFO);
        Label infoLabel = new Label(Localization.lang(infoBlock.text()));
        HBox.setHgrow(infoLabel, Priority.ALWAYS);
        infoContainer.getChildren().addAll(icon, infoLabel);
        VBox infoWrapper = new VBox(infoContainer);
        infoWrapper.setAlignment(Pos.CENTER_LEFT);
        return infoWrapper;
    }

    private VBox makePanel() {
        VBox container = new VBox();
        container.getStyleClass().add("walkthrough-panel");
        return container;
    }

    private HBox makeActions(WalkthroughNode step, Walkthrough manager) {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setSpacing(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (step.actions().flatMap(WalkthroughActionsConfig::backButtonText).isPresent()) {
            actions.getChildren().add(makeBackButton(step, manager));
        }
        HBox rightActions = new HBox();
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.setSpacing(4);
        if (step.actions().flatMap(WalkthroughActionsConfig::skipButtonText).isPresent()) {
            rightActions.getChildren().add(makeSkipButton(step, manager));
        }
        if (step.actions().flatMap(WalkthroughActionsConfig::continueButtonText).isPresent()) {
            rightActions.getChildren().add(makeContinueButton(step, manager));
        }

        actions.getChildren().addAll(spacer, rightActions);

        return actions;
    }

    private VBox makeContent(WalkthroughNode step, Walkthrough manager) {
        return new VBox(12, step.content().stream().map(block ->
                switch (block) {
                    case TextBlock textBlock -> render(textBlock);
                    case InfoBlock infoBlock -> render(infoBlock);
                    case ArbitraryJFXBlock arbitraryBlock ->
                            render(arbitraryBlock, manager);
                }
        ).toArray(Node[]::new));
    }

    private Button makeContinueButton(WalkthroughNode step, Walkthrough manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::continueButtonText)
                                .orElse("Walkthrough continue button");

        Button continueButton = new Button(Localization.lang(buttonText));
        continueButton.getStyleClass().add("walkthrough-continue-button");
        continueButton.setOnAction(_ -> {
            step.nextStepAction().ifPresent(action -> action.accept(manager));
            manager.nextStep();
        });
        return continueButton;
    }

    private Button makeSkipButton(WalkthroughNode step, Walkthrough manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::skipButtonText)
                                .orElse("Walkthrough skip to finish");

        Button skipButton = new Button(Localization.lang(buttonText));
        skipButton.getStyleClass().add("walkthrough-skip-button");
        skipButton.setOnAction(_ -> {
            step.skipAction().ifPresent(action -> action.accept(manager));
            manager.skip();
        });
        return skipButton;
    }

    private Button makeBackButton(WalkthroughNode step, Walkthrough manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::backButtonText)
                                .orElse("Walkthrough back button");

        Button backButton = new Button(Localization.lang(buttonText));
        backButton.getStyleClass().add("walkthrough-back-button");
        backButton.setOnAction(_ -> {
            step.previousStepAction().ifPresent(action -> action.accept(manager));
            manager.previousStep();
        });
        return backButton;
    }
}

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
import org.jabref.gui.walkthrough.declarative.InfoBlockContentBlock;
import org.jabref.gui.walkthrough.declarative.StepType;
import org.jabref.gui.walkthrough.declarative.TextContentBlock;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.WalkthroughContentBlock;
import org.jabref.gui.walkthrough.declarative.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

/**
 * Factory for creating walkthrough UI components.
 */
public class WalkthroughUIFactory {
    /**
     * Creates a full-screen page using dynamic content from a walkthrough
     * step.
     */
    public static VBox createFullscreen(WalkthroughStep step, WalkthroughManager manager) {
        VBox container = makePanel();
        container.setAlignment(Pos.CENTER);
        VBox content = new VBox();
        content.getStyleClass().add("walkthrough-fullscreen-content");
        Label titleLabel = new Label(Localization.lang(step.title()));
        titleLabel.getStyleClass().add("walkthrough-title");
        VBox contentContainer = makeContent(step);
        content.getChildren().addAll(titleLabel, contentContainer, makeActions(step, manager));
        container.getChildren().add(content);
        return container;
    }

    /**
     * Creates a side panel for walkthrough steps.
     */
    public static VBox createSidePanel(WalkthroughStep step, WalkthroughManager manager) {
        VBox panel = makePanel();

        if (step.stepType() == StepType.LEFT_PANEL || step.stepType() == StepType.RIGHT_PANEL) {
            panel.getStyleClass().add("walkthrough-side-panel-vertical");
            VBox.setVgrow(panel, Priority.ALWAYS);
            panel.setMaxHeight(Double.MAX_VALUE);
        } else if (step.stepType() == StepType.TOP_PANEL || step.stepType() == StepType.BOTTOM_PANEL) {
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

        VBox contentContainer = makeContent(step);
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        HBox actions = makeActions(step, manager);
        panel.getChildren().addAll(header, contentContainer, bottomSpacer, actions);

        return panel;
    }

    private static Node createContentBlock(WalkthroughContentBlock block) {
        switch (block.getType()) {
            case TEXT:
                TextContentBlock textBlock = (TextContentBlock) block;
                Label textLabel = new Label(Localization.lang(textBlock.getText()));
                textLabel.getStyleClass().add("walkthrough-text-content");
                return textLabel;
            case INFO_BLOCK:
                InfoBlockContentBlock infoBlock = (InfoBlockContentBlock) block;
                HBox infoContainer = new HBox();
                infoContainer.getStyleClass().add("walkthrough-info-container");
                infoContainer.setAlignment(Pos.CENTER_LEFT);
                infoContainer.setSpacing(4);

                JabRefIconView icon = new JabRefIconView(IconTheme.JabRefIcons.INTEGRITY_INFO);
                icon.getStyleClass().add("walkthrough-info-icon");

                Label infoLabel = new Label(Localization.lang(infoBlock.getText()));
                infoLabel.getStyleClass().add("walkthrough-info-label");
                HBox.setHgrow(infoLabel, Priority.ALWAYS);

                infoContainer.getChildren().addAll(icon, infoLabel);

                VBox infoWrapper = new VBox(infoContainer);
                infoWrapper.setAlignment(Pos.CENTER_LEFT);
                return infoWrapper;
        }
        return new Label("Impossible content block type: " + block.getType());
    }

    private static VBox makePanel() {
        VBox container = new VBox();
        container.getStyleClass().add("walkthrough-panel");
        return container;
    }

    private static HBox makeActions(WalkthroughStep step, WalkthroughManager manager) {
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
        rightActions.setSpacing(2.5);
        if (step.actions().flatMap(WalkthroughActionsConfig::skipButtonText).isPresent()) {
            rightActions.getChildren().add(makeSkipButton(step, manager));
        }
        if (step.actions().flatMap(WalkthroughActionsConfig::continueButtonText).isPresent()) {
            rightActions.getChildren().add(makeContinueButton(step, manager));
        }

        actions.getChildren().addAll(spacer, rightActions);

        return actions;
    }

    private static VBox makeContent(WalkthroughStep step) {
        VBox contentContainer = new VBox();
        contentContainer.setSpacing(12);
        for (WalkthroughContentBlock contentBlock : step.content()) {
            Node contentNode = createContentBlock(contentBlock);
            contentContainer.getChildren().add(contentNode);
        }
        return contentContainer;
    }

    private static Button makeContinueButton(WalkthroughStep step, WalkthroughManager manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::continueButtonText)
                                .orElse("Walkthrough continue button");

        Button continueButton = new Button(Localization.lang(buttonText));
        continueButton.getStyleClass().add("walkthrough-continue-button");
        continueButton.setOnAction(_ -> manager.nextStep());
        return continueButton;
    }

    private static Button makeSkipButton(WalkthroughStep step, WalkthroughManager manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::skipButtonText)
                                .orElse("Walkthrough skip to finish");

        Button skipButton = new Button(Localization.lang(buttonText));
        skipButton.getStyleClass().add("walkthrough-skip-button");
        skipButton.setOnAction(_ -> manager.skip());
        return skipButton;
    }

    private static Button makeBackButton(WalkthroughStep step, WalkthroughManager manager) {
        String buttonText = step.actions()
                                .flatMap(WalkthroughActionsConfig::backButtonText)
                                .orElse("Walkthrough back button");

        Button backButton = new Button(Localization.lang(buttonText));
        backButton.getStyleClass().add("walkthrough-back-button");
        backButton.setOnAction(_ -> manager.previousStep());
        return backButton;
    }
}

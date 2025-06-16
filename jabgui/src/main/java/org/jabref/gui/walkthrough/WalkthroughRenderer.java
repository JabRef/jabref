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
import org.jabref.gui.walkthrough.declarative.richtext.ArbitraryJFXBlock;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

/**
 * Renders the walkthrough steps and content blocks into JavaFX Nodes.
 */
public class WalkthroughRenderer {
    /**
     * Renders a tooltip step into a JavaFX Node.
     *
     * @param step        The tooltip step to render
     * @param walkthrough The walkthrough context for navigation
     * @return The rendered tooltip content Node
     */
    public Node render(@NonNull TooltipStep step, @NonNull Walkthrough walkthrough) {
        return createTooltipContent(step, walkthrough);
    }

    /**
     * Renders a panel step into a JavaFX Node.
     *
     * @param step        The panel step to render
     * @param walkthrough The walkthrough context for navigation
     * @return The rendered panel Node
     */
    public Node render(@NonNull PanelStep step, @NonNull Walkthrough walkthrough) {
        VBox panel = makePanel();

        if (step.position() == Pos.CENTER_LEFT || step.position() == Pos.CENTER_RIGHT) {
            panel.getStyleClass().add("walkthrough-side-panel-vertical");
            VBox.setVgrow(panel, Priority.ALWAYS);
            panel.setMaxHeight(Double.MAX_VALUE);

            step.preferredWidth().ifPresent(width -> {
                panel.setPrefWidth(width);
                panel.setMaxWidth(width);
                panel.setMinWidth(width);
            });
        } else if (step.position() == Pos.TOP_CENTER || step.position() == Pos.BOTTOM_CENTER) {
            panel.getStyleClass().add("walkthrough-side-panel-horizontal");
            HBox.setHgrow(panel, Priority.ALWAYS);
            panel.setMaxWidth(Double.MAX_VALUE);

            step.preferredHeight().ifPresent(height -> {
                panel.setPrefHeight(height);
                panel.setMaxHeight(height);
                panel.setMinHeight(height);
            });
        }

        Label titleLabel = new Label(Localization.lang(step.title()));
        titleLabel.getStyleClass().add("walkthrough-title");

        VBox contentContainer = makeContent(step, walkthrough);
        HBox actionsContainer = makeActions(step, walkthrough);

        panel.getChildren().addAll(titleLabel, contentContainer, actionsContainer);

        return panel;
    }

    private Node createTooltipContent(@NonNull TooltipStep step, @NonNull Walkthrough walkthrough) {
        VBox tooltip = new VBox();
        tooltip.getStyleClass().add("walkthrough-tooltip-content-container");

        Label titleLabel = new Label(Localization.lang(step.title()));
        titleLabel.getStyleClass().add("walkthrough-tooltip-title");

        VBox contentContainer = makeContent(step, walkthrough);
        contentContainer.getStyleClass().add("walkthrough-tooltip-content");
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        HBox actionsContainer = makeActions(step, walkthrough);
        actionsContainer.getStyleClass().add("walkthrough-tooltip-actions");

        tooltip.getChildren().addAll(titleLabel, contentContainer, actionsContainer);

        return tooltip;
    }

    private Node render(@NonNull ArbitraryJFXBlock block, @NonNull Walkthrough walkthrough) {
        return block.componentFactory().apply(walkthrough);
    }

    private Node render(@NonNull TextBlock textBlock) {
        Label textLabel = new Label(Localization.lang(textBlock.text()));
        textLabel.getStyleClass().add("walkthrough-text-content");
        return textLabel;
    }

    private Node render(@NonNull InfoBlock infoBlock) {
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

    private HBox makeActions(@NonNull WalkthroughNode step, @NonNull Walkthrough walkthrough) {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("walkthrough-actions");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (step.backButtonText().isPresent()) {
            actions.getChildren().add(makeBackButton(step, walkthrough));
        }
        HBox rightActions = new HBox();
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.getStyleClass().add("walkthrough-right-actions");
        if (step.skipButtonText().isPresent()) {
            rightActions.getChildren().add(makeSkipButton(step, walkthrough));
        }
        if (step.continueButtonText().isPresent()) {
            rightActions.getChildren().add(makeContinueButton(step, walkthrough));
        }

        actions.getChildren().addAll(spacer, rightActions);

        return actions;
    }

    private VBox makeContent(@NonNull WalkthroughNode step, @NonNull Walkthrough walkthrough) {
        VBox contentBox = new VBox();
        contentBox.getStyleClass().add("walkthrough-content");
        contentBox.getChildren().addAll(step.content().stream().map(block ->
                switch (block) {
                    case TextBlock textBlock -> render(textBlock);
                    case InfoBlock infoBlock -> render(infoBlock);
                    case ArbitraryJFXBlock arbitraryBlock -> render(arbitraryBlock, walkthrough);
                }
        ).toArray(Node[]::new));
        return contentBox;
    }

    private Button makeContinueButton(@NonNull WalkthroughNode step, @NonNull Walkthrough walkthrough) {
        String buttonText = step.continueButtonText()
                                .orElse("Walkthrough continue button");

        Button continueButton = new Button(Localization.lang(buttonText));
        continueButton.getStyleClass().add("walkthrough-continue-button");
        continueButton.setOnAction(_ -> walkthrough.nextStep());
        return continueButton;
    }

    private Button makeSkipButton(@NonNull WalkthroughNode step, @NonNull Walkthrough walkthrough) {
        String buttonText = step.skipButtonText()
                                .orElse("Walkthrough skip to finish");

        Button skipButton = new Button(Localization.lang(buttonText));
        skipButton.getStyleClass().add("walkthrough-skip-button");
        skipButton.setOnAction(_ -> walkthrough.skip());
        return skipButton;
    }

    private Button makeBackButton(@NonNull WalkthroughNode step, @NonNull Walkthrough walkthrough) {
        String buttonText = step.backButtonText()
                                .orElse("Walkthrough back button");

        Button backButton = new Button(Localization.lang(buttonText));
        backButton.getStyleClass().add("walkthrough-back-button");
        backButton.setOnAction(_ -> walkthrough.previousStep());
        return backButton;
    }
}

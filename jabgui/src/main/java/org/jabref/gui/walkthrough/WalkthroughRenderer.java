package org.jabref.gui.walkthrough;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.gui.walkthrough.declarative.richtext.ArbitraryJFXBlock;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleComponent;
import org.jabref.logic.l10n.Localization;

/// Renders walkthrough steps and content blocks into JavaFX nodes.
public class WalkthroughRenderer {
    /// Renders a tooltip step into a JavaFX node.
    ///
    /// @param step           The tooltip step to render
    /// @param walkthrough    The walkthrough context for navigation
    /// @param beforeNavigate Runnable to execute before any navigation action
    /// @return The rendered tooltip content node
    public Node render(TooltipStep step, Walkthrough walkthrough, Runnable beforeNavigate) {
        VBox tooltip = new VBox();
        tooltip.getStyleClass().addAll("root", "walkthrough-tooltip-content-container");

        StackPane titleContainer = new StackPane();
        titleContainer.getStyleClass().add("walkthrough-title-container");
        MarkdownTextFlow titleFlow = new MarkdownTextFlow(titleContainer);
        titleFlow.getStyleClass().add("walkthrough-tooltip-title");
        titleFlow.setMarkdown(step.title());
        titleContainer.getChildren().add(titleFlow);

        VBox contentContainer = createContent(step, walkthrough, beforeNavigate);
        contentContainer.getStyleClass().add("walkthrough-content");
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        HBox actionsContainer = createActions(step, walkthrough, beforeNavigate);
        actionsContainer.getStyleClass().add("walkthrough-actions");

        step.maxHeight().ifPresent(tooltip::setMaxHeight);
        step.maxWidth().ifPresent(tooltip::setMaxWidth);

        tooltip.getChildren().addAll(titleContainer, contentContainer, actionsContainer);
        return tooltip;
    }

    /// Renders a panel step into a JavaFX node.
    ///
    /// @param step           The panel step to render
    /// @param walkthrough    The walkthrough context for navigation
    /// @param beforeNavigate Runnable to execute before any navigation action
    /// @return The rendered panel node
    public Node render(PanelStep step, Walkthrough walkthrough, Runnable beforeNavigate) {
        VBox panel = makePanel();
        configurePanelSize(panel, step);

        StackPane titleContainer = new StackPane();
        titleContainer.getStyleClass().add("walkthrough-title-container");
        MarkdownTextFlow titleFlow = new MarkdownTextFlow(titleContainer);
        titleFlow.getStyleClass().add("walkthrough-title");
        titleFlow.setMarkdown(step.title());
        titleContainer.getChildren().add(titleFlow);

        VBox contentContainer = createContent(step, walkthrough, beforeNavigate);
        HBox actionsContainer = createActions(step, walkthrough, beforeNavigate);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        panel.getChildren().addAll(titleContainer, contentContainer, actionsContainer);
        return panel;
    }

    private void configurePanelSize(VBox panel, PanelStep step) {
        boolean isVertical = step.position() == PanelPosition.LEFT || step.position() == PanelPosition.RIGHT;

        if (isVertical) {
            panel.getStyleClass().add("walkthrough-side-panel-vertical");
            VBox.setVgrow(panel, Priority.ALWAYS);
            panel.setMaxHeight(Double.MAX_VALUE);
            step.maxWidth().ifPresent(panel::setMaxWidth);
        } else if (step.position() == PanelPosition.TOP || step.position() == PanelPosition.BOTTOM) {
            panel.getStyleClass().add("walkthrough-side-panel-horizontal");
            HBox.setHgrow(panel, Priority.ALWAYS);
            panel.setMaxWidth(Double.MAX_VALUE);
            step.maxHeight().ifPresent(panel::setMaxHeight);
        }
    }

    private Node render(ArbitraryJFXBlock block, Walkthrough walkthrough, Runnable beforeNavigate) {
        return block.componentFactory().apply(walkthrough, beforeNavigate);
    }

    private Node render(TextBlock textBlock) {
        StackPane container = new StackPane();
        container.getStyleClass().add("walkthrough-text-container");

        MarkdownTextFlow textFlow = new MarkdownTextFlow(container);
        textFlow.getStyleClass().add("walkthrough-text-content");
        textFlow.setMarkdown(textBlock.text());

        container.getChildren().add(textFlow);
        return container;
    }

    private Node render(InfoBlock infoBlock) {
        HBox infoContainer = new HBox();
        infoContainer.getStyleClass().add("walkthrough-info-container");

        JabRefIconView icon = new JabRefIconView(IconTheme.JabRefIcons.INTEGRITY_INFO);

        StackPane textContainer = new StackPane();
        MarkdownTextFlow infoFlow = new MarkdownTextFlow(textContainer);
        infoFlow.getStyleClass().add("walkthrough-info-text");
        infoFlow.setMarkdown(infoBlock.text());
        textContainer.getChildren().add(infoFlow);

        HBox.setHgrow(textContainer, Priority.ALWAYS);
        infoContainer.getChildren().addAll(icon, textContainer);
        return infoContainer;
    }

    private VBox makePanel() {
        VBox container = new VBox();
        container.getStyleClass().add("walkthrough-panel");
        return container;
    }

    private HBox createActions(VisibleComponent component, Walkthrough walkthrough, Runnable beforeNavigate) {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("walkthrough-actions");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        component.backButtonText()
                 .ifPresent(text ->
                         actions.getChildren()
                                .add(makeButton(text, "walkthrough-back-button", beforeNavigate, walkthrough::previousStep)));

        HBox rightActions = new HBox();
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.getStyleClass().add("walkthrough-right-actions");

        component.skipButtonText()
                 .ifPresent(text ->
                         rightActions.getChildren()
                                     .add(makeButton(text, "walkthrough-skip-button", beforeNavigate, walkthrough::skip)));
        component.continueButtonText()
                 .ifPresent(text ->
                         rightActions.getChildren()
                                     .add(makeButton(text, "walkthrough-continue-button", beforeNavigate, walkthrough::nextStep)));
        actions.getChildren().addAll(spacer, rightActions);
        return actions;
    }

    private VBox createContent(VisibleComponent component, Walkthrough walkthrough, Runnable beforeNavigate) {
        VBox contentBox = new VBox();
        contentBox.getStyleClass().add("walkthrough-content");
        contentBox.getChildren().addAll(component.content().stream().map(block ->
                switch (block) {
                    case TextBlock textBlock ->
                            render(textBlock);
                    case InfoBlock infoBlock ->
                            render(infoBlock);
                    case ArbitraryJFXBlock arbitraryBlock ->
                            render(arbitraryBlock, walkthrough, beforeNavigate);
                }
        ).toArray(Node[]::new));
        return contentBox;
    }

    private Button makeButton(String text, String styleClass, Runnable beforeNavigate, Runnable navigationAction) {
        Button button = new Button(Localization.lang(text));
        button.getStyleClass().add(styleClass);
        button.setOnAction(_ -> {
            beforeNavigate.run();
            navigationAction.run();
        });
        return button;
    }
}

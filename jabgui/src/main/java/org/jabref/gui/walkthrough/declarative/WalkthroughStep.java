package org.jabref.gui.walkthrough.declarative;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Represents a single step in a walkthrough with support for different step types and rich content.
 *
 * @param title    The step title (internationalization key)
 * @param stepType The type of step (full screen, panels)
 * @param content  List of content blocks for rich formatting
 * @param resolver Optional function to resolve the target Node in the scene
 * @param actions  Optional custom button configuration
 */
public record WalkthroughStep(
        String title,
        StepType stepType,
        List<WalkthroughContentBlock> content,
        Optional<Function<Scene, Optional<Node>>> resolver,
        Optional<WalkthroughActionsConfig> actions) {

    public WalkthroughStep(String title, StepType stepType, List<WalkthroughContentBlock> content,
                           Function<Scene, Optional<Node>> resolver) {
        this(title, stepType, content, Optional.of(resolver), Optional.of(new WalkthroughActionsConfig(Optional.of("Continue"),
                Optional.of("Skip to finish"), Optional.of("Back"))));
    }

    public WalkthroughStep(String title, StepType stepType, List<WalkthroughContentBlock> content,
                           WalkthroughActionsConfig buttonConfig) {
        this(title, stepType, content, Optional.empty(), Optional.of(buttonConfig));
    }
}

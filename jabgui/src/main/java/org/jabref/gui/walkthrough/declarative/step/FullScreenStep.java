package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.scene.Node;
import javafx.scene.Scene;

import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;

public record FullScreenStep(
        String title,
        List<WalkthroughRichTextBlock> content,
        Optional<Function<Scene, Optional<Node>>> resolver,
        Optional<WalkthroughActionsConfig> actions,
        Optional<Consumer<Walkthrough>> nextStepAction,
        Optional<Consumer<Walkthrough>> previousStepAction,
        Optional<Consumer<Walkthrough>> skipAction,
        Optional<Consumer<Walkthrough>> clickOnNodeAction
) implements WalkthroughNode {
    public FullScreenStep(String title, List<WalkthroughRichTextBlock> content, WalkthroughActionsConfig actions) {
        this(title, content, Optional.empty(), Optional.of(actions),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}


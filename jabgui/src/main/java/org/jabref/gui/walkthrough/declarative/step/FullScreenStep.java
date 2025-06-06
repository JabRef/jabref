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
    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static class Builder {
        private final String title;
        private List<WalkthroughRichTextBlock> content = List.of();
        private Optional<Function<Scene, Optional<Node>>> resolver = Optional.empty();
        private Optional<WalkthroughActionsConfig> actions = Optional.empty();
        private Optional<Consumer<Walkthrough>> nextStepAction = Optional.empty();
        private Optional<Consumer<Walkthrough>> previousStepAction = Optional.empty();
        private Optional<Consumer<Walkthrough>> skipAction = Optional.empty();
        private Optional<Consumer<Walkthrough>> clickOnNodeAction = Optional.empty();

        private Builder(String title) {
            this.title = title;
        }

        public Builder content(WalkthroughRichTextBlock... blocks) {
            this.content = List.of(blocks);
            return this;
        }

        public Builder content(List<WalkthroughRichTextBlock> content) {
            this.content = content;
            return this;
        }

        public Builder resolver(Function<Scene, Optional<Node>> resolver) {
            this.resolver = Optional.of(resolver);
            return this;
        }

        public Builder actions(WalkthroughActionsConfig actions) {
            this.actions = Optional.of(actions);
            return this;
        }

        public Builder nextStepAction(Consumer<Walkthrough> nextStepAction) {
            this.nextStepAction = Optional.of(nextStepAction);
            return this;
        }

        public Builder previousStepAction(Consumer<Walkthrough> previousStepAction) {
            this.previousStepAction = Optional.of(previousStepAction);
            return this;
        }

        public Builder skipAction(Consumer<Walkthrough> skipAction) {
            this.skipAction = Optional.of(skipAction);
            return this;
        }

        public Builder clickOnNodeAction(Consumer<Walkthrough> clickOnNodeAction) {
            this.clickOnNodeAction = Optional.of(clickOnNodeAction);
            return this;
        }

        public FullScreenStep build() {
            return new FullScreenStep(title, content, resolver, actions, nextStepAction,
                                    previousStepAction, skipAction, clickOnNodeAction);
        }
    }
}


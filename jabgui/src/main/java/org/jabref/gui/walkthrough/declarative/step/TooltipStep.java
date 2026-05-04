package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.Trigger;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record TooltipStep(@NonNull String title,
                          @NonNull List<WalkthroughRichTextBlock> content,
                          @NonNull NodeResolver resolverValue,
                          @Nullable String continueButtonTextValue,
                          @Nullable String skipButtonTextValue,
                          @Nullable String backButtonTextValue,
                          @Nullable Trigger triggerValue,
                          @NonNull TooltipPosition position,
                          @Nullable Double widthValue,
                          @Nullable Double heightValue,
                          @Nullable WalkthroughEffect highlightValue,
                          @Nullable WindowResolver activeWindowResolverValue,
                          boolean showQuitButtonValue,
                          @NonNull QuitButtonPosition quitButtonPositionValue) implements VisibleComponent {
    public static final double DEFAULT_WIDTH = 384;

    @Override
    public Optional<NodeResolver> nodeResolver() {
        return Optional.of(resolverValue);
    }

    @Override
    public Optional<String> continueButtonText() {
        return Optional.ofNullable(continueButtonTextValue);
    }

    @Override
    public Optional<String> skipButtonText() {
        return Optional.ofNullable(skipButtonTextValue);
    }

    @Override
    public Optional<String> backButtonText() {
        return Optional.ofNullable(backButtonTextValue);
    }

    @Override
    public Optional<Trigger> trigger() {
        return Optional.ofNullable(triggerValue);
    }

    @Override
    public OptionalDouble maxWidth() {
        return widthValue != null ? OptionalDouble.of(widthValue) : OptionalDouble.of(DEFAULT_WIDTH);
    }

    @Override
    public OptionalDouble maxHeight() {
        return heightValue != null ? OptionalDouble.of(heightValue) : OptionalDouble.empty();
    }

    @Override
    public Optional<WalkthroughEffect> highlight() {
        return Optional.ofNullable(highlightValue);
    }

    @Override
    public Optional<WindowResolver> windowResolver() {
        return Optional.ofNullable(activeWindowResolverValue);
    }

    @Override
    public boolean showQuitButton() {
        return showQuitButtonValue;
    }

    @Override
    public QuitButtonPosition quitButtonPosition() {
        return quitButtonPositionValue;
    }

    public static Builder builder(@NonNull String title) {
        return new Builder(title);
    }

    public static class Builder {
        private final String title;
        private List<WalkthroughRichTextBlock> content = List.of();
        private @Nullable NodeResolver resolver;
        private @Nullable String continueButtonText;
        private @Nullable String skipButtonText;
        private @Nullable String backButtonText;
        private @Nullable Trigger trigger;
        private TooltipPosition position = TooltipPosition.AUTO;
        private @Nullable Double width;
        private @Nullable Double height;
        private @Nullable WalkthroughEffect highlight;
        private @Nullable WindowResolver activeWindowResolver;
        private boolean showQuitButton = true;
        private QuitButtonPosition quitButtonPosition = QuitButtonPosition.AUTO;

        private Builder(@NonNull String title) {
            this.title = title;
        }

        public Builder content(@NonNull WalkthroughRichTextBlock... blocks) {
            this.content = List.of(blocks);
            return this;
        }

        public Builder content(@NonNull List<WalkthroughRichTextBlock> content) {
            this.content = content;
            return this;
        }

        public Builder resolver(@NonNull NodeResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Builder continueButton(@NonNull String text) {
            this.continueButtonText = text;
            return this;
        }

        public Builder skipButton(@NonNull String text) {
            this.skipButtonText = text;
            return this;
        }

        public Builder backButton(@NonNull String text) {
            this.backButtonText = text;
            return this;
        }

        public Builder trigger(@NonNull Trigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder trigger(Trigger.@NonNull Builder triggerBuilder) {
            this.trigger = triggerBuilder.build();
            return this;
        }

        public Builder position(@NonNull TooltipPosition position) {
            this.position = position;
            return this;
        }

        public Builder width(double width) {
            this.width = width;
            return this;
        }

        public Builder height(double height) {
            this.height = height;
            return this;
        }

        public Builder highlight(@NonNull WalkthroughEffect highlight) {
            this.highlight = highlight;
            return this;
        }

        public Builder highlight(@NonNull WindowEffect effect) {
            return highlight(new WalkthroughEffect(effect));
        }

        public Builder highlight(@NonNull HighlightEffect effect) {
            return highlight(new WindowEffect(effect));
        }

        public Builder activeWindow(@NonNull WindowResolver activeWindowResolver) {
            this.activeWindowResolver = activeWindowResolver;
            return this;
        }

        public Builder showQuitButton(boolean showQuitButton) {
            this.showQuitButton = showQuitButton;
            return this;
        }

        public Builder quitButtonPosition(@NonNull QuitButtonPosition quitButtonPosition) {
            this.quitButtonPosition = quitButtonPosition;
            return this;
        }

        public TooltipStep build() {
            if (resolver == null) {
                throw new IllegalStateException("Node resolver is required for TooltipStep");
            }
            return new TooltipStep(title,
                    content,
                    resolver,
                    continueButtonText,
                    skipButtonText,
                    backButtonText,
                    trigger,
                    position,
                    width,
                    height,
                    highlight,
                    activeWindowResolver,
                    showQuitButton,
                    quitButtonPosition);
        }
    }
}

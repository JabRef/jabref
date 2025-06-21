package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record PanelStep(
        @NonNull String title,
        @NonNull List<WalkthroughRichTextBlock> content,
        @Nullable NodeResolver resolverValue,
        @Nullable String continueButtonTextValue,
        @Nullable String skipButtonTextValue,
        @Nullable String backButtonTextValue,
        @Nullable NavigationPredicate navigationPredicateValue,
        @NonNull PanelPosition position,
        @Nullable Double widthValue,
        @Nullable Double heightValue,
        @Nullable MultiWindowHighlight highlightValue,
        @Nullable WindowResolver activeWindowResolverValue) implements WalkthroughStep {

    @Override
    public Optional<NodeResolver> resolver() {
        return Optional.ofNullable(resolverValue);
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
    public Optional<NavigationPredicate> navigationPredicate() {
        return Optional.ofNullable(navigationPredicateValue);
    }

    @Override
    public OptionalDouble width() {
        return widthValue != null ? OptionalDouble.of(widthValue) : OptionalDouble.empty();
    }

    @Override
    public OptionalDouble height() {
        return heightValue != null ? OptionalDouble.of(heightValue) : OptionalDouble.empty();
    }

    @Override
    public Optional<MultiWindowHighlight> highlight() {
        return Optional.ofNullable(highlightValue);
    }

    @Override
    public Optional<WindowResolver> activeWindowResolver() {
        return Optional.ofNullable(activeWindowResolverValue);
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
        private @Nullable NavigationPredicate navigationPredicate;
        private PanelPosition position = PanelPosition.LEFT;
        private @Nullable Double width;
        private @Nullable Double height;
        private @Nullable MultiWindowHighlight highlight;
        private @Nullable WindowResolver activeWindowResolver;

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

        public Builder navigation(@NonNull NavigationPredicate navigationPredicate) {
            this.navigationPredicate = navigationPredicate;
            return this;
        }

        public Builder position(@NonNull PanelPosition position) {
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

        public Builder highlight(@NonNull MultiWindowHighlight highlight) {
            this.highlight = highlight;
            return this;
        }

        public Builder highlight(@NonNull WindowEffect effect) {
            return highlight(new MultiWindowHighlight(effect));
        }

        public Builder highlight(@NonNull HighlightEffect effect) {
            return highlight(new WindowEffect(effect));
        }

        public Builder activeWindow(@NonNull WindowResolver activeWindowResolver) {
            this.activeWindowResolver = activeWindowResolver;
            return this;
        }

        public PanelStep build() {
            if (height != null && (position == PanelPosition.LEFT || position == PanelPosition.RIGHT)) {
                throw new IllegalArgumentException("Height is not applicable for left/right positioned panels.");
            }
            if (width != null && (position == PanelPosition.TOP || position == PanelPosition.BOTTOM)) {
                throw new IllegalArgumentException("Width is not applicable for top/bottom positioned panels.");
            }
            return new PanelStep(title,
                    content,
                    resolver,
                    continueButtonText,
                    skipButtonText,
                    backButtonText,
                    navigationPredicate,
                    position,
                    width,
                    height,
                    highlight,
                    activeWindowResolver);
        }
    }
}

package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.MultiWindowHighlight;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

public record TooltipStep(
        String title,
        List<WalkthroughRichTextBlock> content,
        NodeResolver resolver,
        Optional<String> continueButtonText,
        Optional<String> skipButtonText,
        Optional<String> backButtonText,
        Optional<NavigationPredicate> navigationPredicate,
        TooltipPosition position,
        Optional<Double> preferredWidth,
        Optional<Double> preferredHeight,
        Optional<MultiWindowHighlight> highlight,
        boolean autoFallback,
        Optional<WindowResolver> activeWindowResolver
) implements WalkthroughNode {
    public static Builder builder(String key, Object... params) {
        return new Builder(Localization.lang(key, params));
    }

    public static class Builder {
        private final String title;
        private List<WalkthroughRichTextBlock> content = List.of();
        private NodeResolver resolver;
        private Optional<String> continueButtonText = Optional.empty();
        private Optional<String> skipButtonText = Optional.empty();
        private Optional<String> backButtonText = Optional.empty();
        private Optional<NavigationPredicate> navigationPredicate = Optional.empty();
        private TooltipPosition position = TooltipPosition.AUTO;
        private Optional<Double> preferredWidth = Optional.empty();
        private Optional<Double> preferredHeight = Optional.empty();
        private Optional<MultiWindowHighlight> highlight = Optional.empty();
        private boolean autoFallback = true;
        private Optional<WindowResolver> activeWindowResolver = Optional.empty();

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

        public Builder resolver(@NonNull NodeResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Builder continueButton(@NonNull String text) {
            this.continueButtonText = Optional.of(text);
            return this;
        }

        public Builder skipButton(@NonNull String text) {
            this.skipButtonText = Optional.of(text);
            return this;
        }

        public Builder backButton(@NonNull String text) {
            this.backButtonText = Optional.of(text);
            return this;
        }

        public Builder navigation(@NonNull NavigationPredicate navigationPredicate) {
            this.navigationPredicate = Optional.of(navigationPredicate);
            return this;
        }

        public Builder position(@NonNull TooltipPosition position) {
            this.position = position;
            return this;
        }

        public Builder preferredWidth(double width) {
            this.preferredWidth = Optional.of(width);
            return this;
        }

        public Builder preferredHeight(double height) {
            this.preferredHeight = Optional.of(height);
            return this;
        }

        public Builder highlight(@NonNull MultiWindowHighlight highlight) {
            this.highlight = Optional.of(highlight);
            return this;
        }

        public Builder highlight(@NonNull HighlightEffect effect) {
            this.highlight = Optional.of(MultiWindowHighlight.single(new WindowEffect(activeWindowResolver.orElse(() -> Optional.empty())::resolve, effect, Optional.empty())));
            return this;
        }

        public Builder autoFallback(boolean autoFallback) {
            this.autoFallback = autoFallback;
            return this;
        }

        public Builder activeWindow(@NonNull WindowResolver activeWindowResolver) {
            this.activeWindowResolver = Optional.of(activeWindowResolver);
            return this;
        }

        public TooltipStep build() {
            if (resolver == null) {
                throw new IllegalStateException("Node resolver is required for TooltipStep");
            }
            return new TooltipStep(title, content, resolver, continueButtonText, skipButtonText, backButtonText, navigationPredicate, position, preferredWidth, preferredHeight, highlight, autoFallback, activeWindowResolver);
        }
    }
}


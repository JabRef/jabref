package org.jabref.gui.walkthrough.declarative.step;

import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;

import org.jspecify.annotations.NonNull;

/// A walkthrough step that executes side effects without displaying any UI.
public record SideEffect(@NonNull String title,
                         @NonNull WalkthroughSideEffect sideEffect) implements WalkthroughStep {
    public static Builder builder(@NonNull String title) {
        return new Builder(title);
    }

    public static class Builder {
        private final String title;
        private WalkthroughSideEffect sideEffect;

        private Builder(@NonNull String title) {
            this.title = title;
        }

        public Builder sideEffect(@NonNull WalkthroughSideEffect sideEffect) {
            this.sideEffect = sideEffect;
            return this;
        }

        public SideEffect build() {
            if (sideEffect == null) {
                throw new IllegalStateException("Side effect is required for SideEffectStep");
            }
            return new SideEffect(title, sideEffect);
        }
    }
}

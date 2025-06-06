package org.jabref.gui.walkthrough.declarative;

import java.util.Optional;

/**
 * Configuration for walkthrough step buttons.
 *
 * @param continueButtonText Optional text for the continue button. If empty, button is
 *                           hidden.
 * @param skipButtonText     Optional text for the skip button. If empty, button is
 *                           hidden.
 * @param backButtonText     Optional text for the back button. If empty, button is
 *                           hidden.
 */
public record WalkthroughActionsConfig(
        Optional<String> continueButtonText,
        Optional<String> skipButtonText,
        Optional<String> backButtonText
) {

    public static Builder builder() {
        return new Builder();
    }

    public static WalkthroughActionsConfig all(String continueText, String skipText, String backText) {
        return new WalkthroughActionsConfig(Optional.of(continueText), Optional.of(skipText), Optional.of(backText));
    }

    public static class Builder {
        private Optional<String> continueButtonText = Optional.empty();
        private Optional<String> skipButtonText = Optional.empty();
        private Optional<String> backButtonText = Optional.empty();

        public Builder continueButton(String text) {
            this.continueButtonText = Optional.of(text);
            return this;
        }

        public Builder skipButton(String text) {
            this.skipButtonText = Optional.of(text);
            return this;
        }

        public Builder backButton(String text) {
            this.backButtonText = Optional.of(text);
            return this;
        }

        public WalkthroughActionsConfig build() {
            return new WalkthroughActionsConfig(continueButtonText, skipButtonText, backButtonText);
        }
    }
}

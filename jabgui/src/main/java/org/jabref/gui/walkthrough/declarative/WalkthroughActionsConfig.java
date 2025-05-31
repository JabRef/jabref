package org.jabref.gui.walkthrough.declarative;

import java.util.Optional;

/**
 * Configuration for walkthrough step buttons.
 *
 * @param continueButtonText Optional text for the continue button. If empty, button is hidden.
 * @param skipButtonText     Optional text for the skip button. If empty, button is hidden.
 * @param backButtonText     Optional text for the back button. If empty, button is hidden.
 */
public record WalkthroughActionsConfig(
        Optional<String> continueButtonText,
        Optional<String> skipButtonText,
        Optional<String> backButtonText
) {
}

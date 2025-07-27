package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;

public sealed interface WalkthroughStep permits PanelStep, TooltipStep {
    String title();

    List<WalkthroughRichTextBlock> content();

    Optional<NodeResolver> resolver();

    Optional<String> continueButtonText();

    Optional<String> skipButtonText();

    Optional<String> backButtonText();

    Optional<NavigationPredicate> navigationPredicate();

    OptionalDouble maxWidth();

    OptionalDouble maxHeight();

    Optional<WalkthroughEffect> highlight();

    Optional<WindowResolver> activeWindowResolver();

    default boolean showQuitButton() {
        return true;
    }

    default QuitButtonPosition quitButtonPosition() {
        return QuitButtonPosition.AUTO;
    }

    static TooltipStep.Builder tooltip(String key) {
        return TooltipStep.builder(key);
    }

    static PanelStep.Builder panel(String title) {
        return PanelStep.builder(title);
    }
}

package org.jabref.gui.walkthrough.declarative.step;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.Trigger;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.richtext.WalkthroughRichTextBlock;

/// Walkthrough steps that display visible UI elements.
public sealed interface VisibleComponent extends WalkthroughStep permits PanelStep, TooltipStep {
    /// Content blocks to display
    List<WalkthroughRichTextBlock> content();

    /// Node resolver for the node to highlight / tooltip to position / etc.
    Optional<NodeResolver> nodeResolver();

    /// Custom text for the continue button.
    /// - If [Optional#empty()], no continue button is shown.
    /// - If present, the button will be shown with the provided text.
    Optional<String> continueButtonText();

    /// Custom text for the skip button.
    /// - If [Optional#empty()], no skip button is shown.
    /// - If present, the button will be shown with the provided text.
    Optional<String> skipButtonText();

    /// Custom text for the back button.
    /// - If [Optional#empty()], no back button is shown.
    /// - If present, the button will be shown with the provided text.
    Optional<String> backButtonText();

    /// Navigation predicate that determines when to advance by attaching to the UI
    /// elements that could trigger navigation.
    Optional<Trigger> trigger();

    /// Maximum width for the display element.
    OptionalDouble maxWidth();

    /// Maximum height for the display element.
    OptionalDouble maxHeight();

    /// Highlight effect to apply.
    Optional<WalkthroughEffect> highlight();

    /// Window resolver for targeting specific windows.
    Optional<WindowResolver> windowResolver();

    /// Whether to show the quit button.
    boolean showQuitButton();

    /// Position of the quit button.
    QuitButtonPosition quitButtonPosition();
}

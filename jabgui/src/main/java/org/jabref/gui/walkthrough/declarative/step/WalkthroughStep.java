package org.jabref.gui.walkthrough.declarative.step;

public sealed interface WalkthroughStep permits SideEffectStep, VisibleWalkthroughStep {
    String title();

    static TooltipStep.Builder tooltip(String title) {
        return TooltipStep.builder(title);
    }

    static PanelStep.Builder panel(String title) {
        return PanelStep.builder(title);
    }

    static SideEffectStep.Builder sideEffect(String title) {
        return SideEffectStep.builder(title);
    }
}

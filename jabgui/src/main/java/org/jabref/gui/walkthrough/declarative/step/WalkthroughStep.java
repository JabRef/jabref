package org.jabref.gui.walkthrough.declarative.step;

public sealed interface WalkthroughStep permits SideEffect, VisibleComponent {
    String title();

    static TooltipStep.Builder tooltip(String title) {
        return TooltipStep.builder(title);
    }

    static PanelStep.Builder panel(String title) {
        return PanelStep.builder(title);
    }

    static SideEffect.Builder sideEffect(String title) {
        return SideEffect.builder(title);
    }
}

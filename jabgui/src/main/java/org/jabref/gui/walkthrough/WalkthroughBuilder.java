package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.jspecify.annotations.NonNull;

public class WalkthroughBuilder {
    private final StateManager stateManager;
    private final List<WalkthroughStep> steps;

    private WalkthroughBuilder(StateManager stateManager) {
        this.stateManager = stateManager;
        this.steps = new ArrayList<>();
    }

    public static WalkthroughBuilder create(StateManager stateManager) {
        return new WalkthroughBuilder(stateManager);
    }

    public WalkthroughBuilder addStep(@NonNull WalkthroughStep step) {
        steps.add(step);
        return this;
    }

    public Walkthrough build() {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Walkthrough must have at least one step.");
        }
        return new Walkthrough(stateManager, steps);
    }
}

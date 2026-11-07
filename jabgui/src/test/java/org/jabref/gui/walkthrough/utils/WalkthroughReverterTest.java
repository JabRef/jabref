package org.jabref.gui.walkthrough.utils;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.gui.walkthrough.declarative.sideeffect.ExpectedCondition;
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class WalkthroughReverterTest {

    @Test
    void reversesPrecedingSideEffectWithoutReversingCurrentStep() {
        AtomicInteger precedingStepBackwardCalls = new AtomicInteger();
        AtomicInteger currentStepBackwardCalls = new AtomicInteger();
        Walkthrough walkthrough = new Walkthrough(
                mock(StateManager.class),
                WalkthroughStep.sideEffect("Prepare preferences").sideEffect(new TrackingSideEffect(precedingStepBackwardCalls)).build(),
                WalkthroughStep.sideEffect("Current step").sideEffect(new TrackingSideEffect(currentStepBackwardCalls)).build()
        );
        walkthrough.nextStep();
        WalkthroughReverter reverter = new WalkthroughReverter(
                walkthrough,
                mock(Stage.class),
                new SideEffectExecutor()
        );

        reverter.findAndUndo();

        assertEquals(1, precedingStepBackwardCalls.get());
        assertEquals(0, currentStepBackwardCalls.get());
    }

    private record TrackingSideEffect(AtomicInteger backwardCalls) implements WalkthroughSideEffect {

        @Override
        public @NonNull ExpectedCondition expectedCondition() {
            return ExpectedCondition.ALWAYS_TRUE;
        }

        @Override
        public boolean forward(@NonNull Walkthrough walkthrough) {
            return true;
        }

        @Override
        public boolean backward(@NonNull Walkthrough walkthrough) {
            backwardCalls.incrementAndGet();
            return true;
        }

        @Override
        public @NonNull String description() {
            return "Track walkthrough reversal.";
        }
    }
}

package org.jabref.toolkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceShortenerTest {

    private final List<String> applied = new ArrayList<>();

    /// Returns the given page counts on successive `measure` calls (first call = baseline).
    private static IntSupplier scriptedPageCounts(int... counts) {
        int[] index = {0};
        return () -> counts[index[0]++];
    }

    private ReferenceShortener.Step step(String name) {
        return new ReferenceShortener.Step(name, () -> applied.add(name));
    }

    @Test
    void doesNothingWhenAlreadyWithinTarget() {
        ReferenceShortener shortener = new ReferenceShortener(List.of(step("a")), scriptedPageCounts(5));

        ReferenceShortener.Result result = shortener.shorten(OptionalInt.of(5));

        assertEquals(List.of(), applied);
        assertEquals(List.of(), result.appliedSteps());
        assertTrue(result.reachedTarget());
        assertEquals(5, result.baselinePages());
        assertEquals(5, result.finalPages());
    }

    @Test
    void stopsAtFirstStepThatReachesTarget() {
        ReferenceShortener shortener = new ReferenceShortener(
                List.of(step("a"), step("b"), step("c")),
                scriptedPageCounts(8, 6));

        ReferenceShortener.Result result = shortener.shorten(OptionalInt.of(6));

        assertEquals(List.of("a"), applied);
        assertEquals(List.of("a"), result.appliedSteps());
        assertTrue(result.reachedTarget());
        assertEquals(6, result.finalPages());
    }

    @Test
    void appliesAllStepsAndReportsFailureWhenTargetUnreachable() {
        ReferenceShortener shortener = new ReferenceShortener(
                List.of(step("a"), step("b"), step("c")),
                scriptedPageCounts(8, 7, 7, 7));

        ReferenceShortener.Result result = shortener.shorten(OptionalInt.of(3));

        assertEquals(List.of("a", "b", "c"), result.appliedSteps());
        assertFalse(result.reachedTarget());
        assertEquals(7, result.finalPages());
    }

    @Test
    void defaultTargetIsOnePageShorterThanBaseline() {
        ReferenceShortener shortener = new ReferenceShortener(
                List.of(step("a"), step("b")),
                scriptedPageCounts(5, 4));

        ReferenceShortener.Result result = shortener.shorten(OptionalInt.empty());

        assertEquals(4, result.targetPages());
        assertEquals(List.of("a"), result.appliedSteps());
        assertTrue(result.reachedTarget());
    }
}

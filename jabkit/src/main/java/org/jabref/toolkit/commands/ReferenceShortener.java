package org.jabref.toolkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntSupplier;

/// Measure-driven, escalating shortener: runs increasingly aggressive reference-shortening steps
/// one at a time, recompiling the document (via the injected [IntSupplier] page counter) after each,
/// and stops as soon as the page count reaches the target. Deliberately free of any LaTeX/IO/BibTeX
/// knowledge — the caller supplies the steps (as [Runnable]s that mutate its entries) and the
/// counter — so the stop-when-it-fits logic is unit-testable without a TeX installation.
class ReferenceShortener {

    /// A single shortening escalation level. `apply` mutates the caller's entries in place.
    record Step(String description, Runnable apply) {
    }

    record Result(int baselinePages, int finalPages, int targetPages, List<String> appliedSteps, boolean reachedTarget) {
    }

    private final List<Step> steps;
    private final IntSupplier measure;

    /// @param steps   escalation levels, applied in order until the target is met
    /// @param measure persists the current entry state, compiles, and returns the resulting page count
    ReferenceShortener(List<Step> steps, IntSupplier measure) {
        this.steps = List.copyOf(steps);
        this.measure = measure;
    }

    /// @param targetPages desired maximum page count; when empty, the target is "one page shorter than
    ///                    the baseline", matching "reduce until the paper is one page shorter"
    Result shorten(OptionalInt targetPages) {
        int baseline = measure.getAsInt();
        int target = targetPages.orElse(baseline - 1);
        List<String> applied = new ArrayList<>();

        int pages = baseline;
        for (Step step : steps) {
            if (pages <= target) {
                break;
            }
            step.apply().run();
            applied.add(step.description());
            pages = measure.getAsInt();
        }

        return new Result(baseline, pages, target, applied, pages <= target);
    }
}

package org.jabref.toolkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.jabref.toolkit.exception.CliException;

/// Measure-driven, escalating shortener: runs increasingly aggressive reference-shortening steps
/// one at a time, recompiling the document (via the injected [PageCounter]) after each, and stops as
/// soon as the page count reaches the target. Deliberately free of any LaTeX/IO/BibTeX knowledge —
/// the caller supplies the steps (as [Runnable]s that mutate its entries) and the counter — so the
/// stop-when-it-fits logic is unit-testable without a TeX installation.
class ReferenceShortener {

    /// Persists the current entry state, compiles the document, and returns its page count. A
    /// dedicated interface (rather than [java.util.function.IntSupplier]) so the measurement can
    /// declare its checked [CliException] instead of forcing a wrapper exception.
    @FunctionalInterface
    interface PageCounter {
        int measure() throws CliException;
    }

    /// A single shortening escalation level. `apply` mutates the caller's entries in place.
    record Step(String description, Runnable apply) {
    }

    record Result(int baselinePages, int finalPages, int targetPages, List<String> appliedSteps, boolean reachedTarget) {
    }

    private final List<Step> steps;
    private final PageCounter measure;

    /// @param steps   escalation levels, applied in order until the target is met
    /// @param measure persists the current entry state, compiles, and returns the resulting page count
    ReferenceShortener(List<Step> steps, PageCounter measure) {
        this.steps = List.copyOf(steps);
        this.measure = measure;
    }

    /// @param targetPages desired maximum page count; when empty, the target is one page shorter than the baseline ("reduce until the paper is one page shorter"), but never below 1 so a single-page paper is left untouched
    Result shorten(OptionalInt targetPages) throws CliException {
        int baseline = measure.measure();
        // Clamp the implicit "one page shorter" target to 1: a 0-page target is unreachable and would
        // otherwise apply every step (rewriting the .bib) on a paper that cannot shrink further.
        int target = targetPages.orElse(Math.max(1, baseline - 1));
        List<String> applied = new ArrayList<>();

        int pages = baseline;
        for (Step step : steps) {
            if (pages <= target) {
                break;
            }
            step.apply().run();
            applied.add(step.description());
            pages = measure.measure();
        }

        return new Result(baseline, pages, target, applied, pages <= target);
    }
}

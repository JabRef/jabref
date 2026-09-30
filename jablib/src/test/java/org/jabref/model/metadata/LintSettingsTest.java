package org.jabref.model.metadata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LintSettingsTest {

    @Test
    void aRuleNoSelectorNamesDoesNotRun() {
        LintSettings settings = new LintSettings(new LinkedHashMap<>());

        assertEquals(RuleMode.OFF, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    @Test
    void aSelectorNamingNoFieldCoversEveryField() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX);

        assertEquals(RuleMode.FIX, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    /// The point of naming a field: repair everywhere, report on one field alone.
    @Test
    void aSelectorNamingTheFieldBeatsOneNamingNone() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK);

        assertEquals(RuleMode.CHECK, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.FIX, settings.modeOf("lower-case", Optional.of(StandardField.JOURNAL)));
    }

    /// Order does not decide it: the field is named either way round.
    @Test
    void theFieldWinsWhereverItStands() {
        LintSettings settings = LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK)
                                           .and(RuleSelector.of("lower-case"), RuleMode.FIX);

        assertEquals(RuleMode.CHECK, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    @Test
    void aRuleNamedOnOneFieldRunsOnNoOther() {
        LintSettings settings = LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX);

        assertEquals(RuleMode.OFF, settings.modeOf("lower-case", Optional.of(StandardField.JOURNAL)));
    }

    /// A finding about the entry as a whole names no field, so only a selector naming none is about it.
    @Test
    void aFindingAboutNoFieldTakesTheSelectorNamingNoField() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.CHECK)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX);

        assertEquals(RuleMode.CHECK, settings.modeOf("lower-case", Optional.empty()));
    }

    @Test
    void namingASelectorTwiceTakesTheLastMode() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.of("lower-case"), RuleMode.OFF);

        assertEquals(RuleMode.OFF, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
    }

    /// A rule let loose on one field only still takes part in the run, on that field.
    @Test
    void aRuleNamedOnAFieldOnlyStillRuns() {
        LintSettings settings = LintSettings.of(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX);

        assertTrue(settings.runs("lower-case"));
        assertFalse(settings.runs("upper-case"));
    }

    /// Switching a rule off everywhere leaves it out of the run, rather than running it and
    /// reporting it on no field.
    @Test
    void aRuleSwitchedOffEverywhereDoesNotRun() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.OFF);

        assertFalse(settings.runs("lower-case"));
    }

    /// One field switched off is not the whole rule switched off.
    @Test
    void aRuleSwitchedOffOnOneFieldStillRuns() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.OFF);

        assertTrue(settings.runs("lower-case"));
    }

    /// A Save Action may be configured for a whole class of fields, and then its findings are about
    /// the concrete fields it visited. Such a selector is about all of them, or the rule would be
    /// named by nothing its findings ever match and would quietly stop running.
    @Test
    void aSelectorNamingAWholeClassOfFieldsIsAboutEveryFieldTheRuleVisits() {
        LintSettings settings = LintSettings.of(
                RuleSelector.on(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, "latex-to-unicode"), RuleMode.FIX);

        assertEquals(RuleMode.FIX, settings.modeOf("latex-to-unicode", Optional.of(StandardField.TITLE)));
    }

    @Test
    void namingAFieldBeatsNamingAWholeClassOfThem() {
        LintSettings settings = LintSettings.of(
                                        RuleSelector.on(InternalField.INTERNAL_ALL_FIELD, "lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.OFF);

        assertEquals(RuleMode.OFF, settings.modeOf("lower-case", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.FIX, settings.modeOf("lower-case", Optional.of(StandardField.JOURNAL)));
    }

    /// Otherwise the field-scoped entry would win, being the more specific, and an option given for
    /// one run would not reach the field the library named.
    @Test
    void anOverrideAlsoGivesWayFromAFieldScopedEntry() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.CHECK);

        LintSettings overridden = settings.overriddenBy(
                new LinkedHashMap<>(Map.of("lower-case", RuleMode.OFF)));

        assertEquals(RuleMode.OFF, overridden.modeOf("lower-case", Optional.of(StandardField.TITLE)));
        assertEquals(RuleMode.OFF, overridden.modeOf("lower-case", Optional.of(StandardField.JOURNAL)));
    }

    @Test
    void anOverrideLeavesEveryOtherRuleAlone() {
        LintSettings settings = LintSettings.of(RuleSelector.of("lower-case"), RuleMode.FIX)
                                           .and(RuleSelector.of("upper-case"), RuleMode.CHECK);

        LintSettings overridden = settings.overriddenBy(
                new LinkedHashMap<>(Map.of("lower-case", RuleMode.OFF)));

        assertEquals(RuleMode.CHECK, overridden.modeOf("upper-case", Optional.of(StandardField.TITLE)));
    }

    /// So that saving does not reshuffle a configuration somebody edited by hand.
    @Test
    void theOrderTheSelectorsWereNamedInIsKept() {
        LintSettings settings = LintSettings.of(RuleSelector.of("upper-case"), RuleMode.FIX)
                                           .and(RuleSelector.of("lower-case"), RuleMode.CHECK)
                                           .and(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.OFF);

        assertEquals(List.of(
                        RuleSelector.of("upper-case"),
                        RuleSelector.of("lower-case"),
                        RuleSelector.on(StandardField.TITLE, "lower-case")),
                List.copyOf(settings.ruleModes().keySet()));
    }
}

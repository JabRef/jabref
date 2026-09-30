package org.jabref.model.metadata;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSelectorTest {

    @Test
    void aKeyWithoutAFieldNamesTheRuleWhereverItLooks() {
        assertEquals(Optional.of(RuleSelector.of("lower-case")), RuleSelector.parse("lower-case"));
    }

    @Test
    void aKeyWithAFieldNamesTheRuleThere() {
        assertEquals(Optional.of(RuleSelector.on(StandardField.TITLE, "lower-case")),
                RuleSelector.parse("title:lower-case"));
    }

    /// A BibTeX field name may contain a colon; a rule id may not, so the last one separates them.
    @Test
    void theRuleIdIsWhatStandsAfterTheLastColon() {
        assertEquals(Optional.of("lower-case"),
                RuleSelector.parse("note:de:lower-case").map(RuleSelector::ruleId));
        assertEquals(Optional.of("note:de"),
                RuleSelector.parse("note:de:lower-case").flatMap(RuleSelector::field).map(field -> field.getName()));
    }

    /// The magic comments above an entry read these; a library-wide key does not, yet. Reading one
    /// as a field name that happens to contain a comma would make it mean something else later.
    @ParameterizedTest
    @ValueSource(strings = {"title,author:lower-case", "/.*title/:lower-case", "title:lower-case,upper-case",
            "title: lower-case", "title:\tlower-case", "", "   ", "title:", ":lower-case"})
    void aSpellingThisJabFixDoesNotReadIsNotRead(String key) {
        assertEquals(Optional.empty(), RuleSelector.parse(key));
    }

    static Stream<Arguments> roundTrips() {
        return Stream.of(
                Arguments.of(RuleSelector.of("lower-case")),
                Arguments.of(RuleSelector.on(StandardField.TITLE, "lower-case")),
                Arguments.of(RuleSelector.on(InternalField.INTERNAL_ALL_FIELD, "lower-case")),
                Arguments.of(RuleSelector.on(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, "latex-to-unicode")));
    }

    @ParameterizedTest
    @MethodSource("roundTrips")
    void whatIsWrittenIsReadBackTheSame(RuleSelector selector) {
        assertEquals(Optional.of(selector.ruleId()), RuleSelector.parse(selector.asKey()).map(RuleSelector::ruleId));
        assertEquals(selector.field().map(field -> field.getName()),
                RuleSelector.parse(selector.asKey()).flatMap(RuleSelector::field).map(field -> field.getName()));
    }

    /// Read back from a file, a marker is whatever the field factory made of the name, so covering
    /// every field must not depend on it being the very same object.
    @ParameterizedTest
    @ValueSource(strings = {"all:lower-case", "all-text-fields:lower-case"})
    void aMarkerReadFromAFileStillCoversEveryField(String key) {
        assertTrue(RuleSelector.parse(key).orElseThrow().coversEveryField());
    }

    @Test
    void aNamedFieldDoesNotCoverEveryField() {
        assertFalse(RuleSelector.parse("title:lower-case").orElseThrow().coversEveryField());
    }
}

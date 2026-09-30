package org.jabref.logic.lint.rule;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.IdentityFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveActionIdsTest {

    static Stream<Arguments> everyFormatter() {
        return Formatters.getAll().stream().map(Arguments::of);
    }

    /// The transform lower-cases and replaces underscores, so it cannot be reversed: `escapeAmpersands`
    /// becomes `escapeampersands`, which no formatter is registered under.
    @ParameterizedTest
    @MethodSource("everyFormatter")
    void everyFormatterIsFoundAgainByItsId(Formatter formatter) {
        assertEquals(Optional.of(formatter.getKey()),
                SaveActionIds.formatterFor(SaveActionIds.idOf(formatter)).map(Formatter::getKey));
    }

    /// Which is what lets an id stand for exactly one Save Action in the stored configuration.
    @Test
    void noTwoFormattersShareAnId() {
        Set<String> ids = new HashSet<>();
        Formatters.getAll().forEach(formatter -> assertTrue(ids.add(SaveActionIds.idOf(formatter)),
                "two formatters share the id " + SaveActionIds.idOf(formatter)));
    }

    /// A Save Action naming a formatter JabRef cannot resolve is read as this one, so it has to be
    /// findable like any other.
    @Test
    void theIdentityFormatterIsFoundToo() {
        assertEquals(Optional.of("identity"),
                SaveActionIds.formatterFor(SaveActionIds.idOf(new IdentityFormatter())).map(Formatter::getKey));
    }

    /// The rules JabFix ships with are not formatters, so nothing may be reconstructed from them.
    @ParameterizedTest
    @ValueSource(strings = {"surrounding-whitespace", "repeated-whitespace", "magic-comment", "rule-of-a-newer-jabfix"})
    void anIdThatNamesNoFormatterFindsNothing(String ruleId) {
        assertEquals(Optional.empty(), SaveActionIds.formatterFor(ruleId));
    }
}

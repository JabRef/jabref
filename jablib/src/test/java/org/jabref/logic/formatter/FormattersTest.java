package org.jabref.logic.formatter;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormattersTest {

    /// A formatter missing from [Formatters#getAll()] cannot be picked in the cleanup and save action dialogs,
    /// and a save action stored under its key in a `.bib` file does not resolve back to it.
    @ParameterizedTest
    @ValueSource(strings = {"add_braces", "normalize_en_dashes"})
    void formatterIsResolvableByItsKey(String key) {
        assertEquals(Optional.of(key), Formatters.getFormatterForKey(key).map(Formatter::getKey));
    }
}

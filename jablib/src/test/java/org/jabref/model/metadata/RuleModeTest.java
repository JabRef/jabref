package org.jabref.model.metadata;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleModeTest {

    @ParameterizedTest
    @EnumSource(RuleMode.class)
    void whatIsWrittenIsReadBackTheSame(RuleMode mode) {
        assertEquals(Optional.of(mode), RuleMode.fromKey(mode.asKey()));
    }

    @Test
    void aModeIsSpelledInLowerCase() {
        assertEquals("fix", RuleMode.FIX.asKey());
    }

    /// A newer JabFix may know it; taking it for one of these would change what the library said.
    @ParameterizedTest
    @ValueSource(strings = {"FIX", "Fix", "warn", "", "error"})
    void aSpellingThisJabFixDoesNotKnowIsNotRead(String key) {
        assertEquals(Optional.empty(), RuleMode.fromKey(key));
    }
}

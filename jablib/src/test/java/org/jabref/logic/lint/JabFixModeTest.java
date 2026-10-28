package org.jabref.logic.lint;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabFixModeTest {

    private static final List<String> KNOWN_IDS = RuleSet.known(new FieldPreferences(true, List.of(), List.of())).ids();

    /// A mode naming a rule that does not exist would quietly do less than it says.
    @ParameterizedTest
    @EnumSource(JabFixMode.class)
    void everyModeNamesOnlyRulesThatExist(JabFixMode mode) {
        assertEquals(List.of(), mode.getSettings().ruleModes().keySet().stream()
                                    .map(RuleSelector::ruleId)
                                    .filter(ruleId -> !KNOWN_IDS.contains(ruleId))
                                    .toList());
    }

    @ParameterizedTest
    @EnumSource(JabFixMode.class)
    void everyModeIsFoundAgainByItsKey(JabFixMode mode) {
        assertEquals(Optional.of(mode), JabFixMode.fromKey(mode.getKey()));
    }
}

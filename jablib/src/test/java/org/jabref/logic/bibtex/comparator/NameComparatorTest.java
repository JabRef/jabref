package org.jabref.logic.bibtex.comparator;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class NameComparatorTest {
    private final NameComparator comparator = new NameComparator();

    @Test
    void prefixChangesPositionAmongControlNames() {
        String without = "family=Hasselt, given=Hado P., prefix=van, useprefix=false";
        String with = without.replace("false", "true");

        assertEquals(List.of("Foster, Anna", without, "Miller, Max", with, "Zimmer, Zoe"),
                List.of(with, "Zimmer, Zoe", without, "Foster, Anna", "Miller, Max").stream().sorted(comparator).toList());
    }

    @Test
    void equivalentConventionalAndExtendedNamesCompareEqually() {
        assertEquals(0, comparator.compare("Hasselt, Hado P.", "given=Hado P., family=Hasselt"));
    }

    @Test
    void absentNamesSort() {
        assertEquals(0, comparator.compare("", ""));
        assertEquals(-1, Integer.signum(comparator.compare("", "Smith, Alice")));
    }

    @Test
    void explicitInitialsDoNotOverrideFullGivenNamesInSorting() {
        assertEquals(-1, Integer.signum(comparator.compare("family=Smith, given=Aaron, given-i=Z", "family=Smith, given=Alice, given-i=A")));
    }
}

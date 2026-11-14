package org.jabref.gui.util.comparator;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollationKeyComparatorTest {

    private static final Collator GERMAN_COLLATOR = Collator.getInstance(Locale.GERMAN);

    private final CollationKeyComparator comparator = new CollationKeyComparator(GERMAN_COLLATOR);

    @Test
    void sortsLikeCollator() {
        List<String> values = List.of("Zebra", "apfel", "Äpfel", "Apfel", "ångström", "Ölberg", "Oase", "", "id000010", "id000002", "_underscore", "über");
        List<String> sortedByCollator = new ArrayList<>(values);
        sortedByCollator.sort(GERMAN_COLLATOR);
        List<String> sortedByCollationKey = new ArrayList<>(values);
        sortedByCollationKey.sort(comparator);

        assertEquals(sortedByCollator, sortedByCollationKey);
    }

    @Test
    void sortsUmlautNextToBaseLetter() {
        List<String> values = new ArrayList<>(List.of("Zebra", "Äpfel", "Birne"));
        values.sort(comparator);

        assertEquals(List.of("Äpfel", "Birne", "Zebra"), values);
    }

    @Test
    void equalValuesOfDifferentInstancesAreEqual() {
        String title = "Title";
        String sameTitleOtherInstance = "Tit".concat("le");

        assertEquals(0, comparator.compare(title, sameTitleOtherInstance));
    }

    @Test
    void nullsSortBeforeValues() {
        List<String> values = Arrays.asList("Apfel", null, "Birne", null);
        values.sort(comparator);

        assertEquals(Arrays.asList(null, null, "Apfel", "Birne"), values);
    }
}

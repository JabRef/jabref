package org.jabref.model.groups;

import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticDateGroupTest {

    @Test
    void createsYearBucketFromDateField() {
        BibEntry e = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        AutomaticDateGroup byYear = new AutomaticDateGroup("By Year", GroupHierarchyType.INCLUDING, StandardField.DATE);

        var children = byYear.createSubgroups(e);
        assertEquals(1, children.size());
        GroupTreeNode node = children.iterator().next();
        assertEquals("2024", node.getName());
        assertTrue(node.getGroup().contains(e));
    }

    @Test
    void createsYearBucketFromYearField() {
        BibEntry e = new BibEntry().withField(StandardField.YEAR, "2023");
        AutomaticDateGroup byYear = new AutomaticDateGroup("By Year", GroupHierarchyType.INCLUDING, StandardField.YEAR);

        var children = byYear.createSubgroups(e);
        assertEquals(1, children.size());
        assertEquals("2023", children.iterator().next().getName());
    }

    @Test
    void mergesSameYearAcrossEntries() {
        BibEntry e1 = new BibEntry().withField(StandardField.YEAR, "2023");
        BibEntry e2 = new BibEntry().withField(StandardField.DATE, "2023-05");

        AutomaticDateGroup byYear = new AutomaticDateGroup("By Year", GroupHierarchyType.INCLUDING, StandardField.DATE);
        var merged = byYear.createSubgroups(FXCollections.observableArrayList(List.of(e1, e2)));

        // Only one "2023" node after merge (relies on DateGroup.equals/hashCode)
        assertEquals(1, merged.size());
        assertEquals("2023", merged.getFirst().getName());
    }

    @Test
    void automaticDateGroupBuildsBucketAndFindsMatches() {
        // Parent automatic group using DATE field
        AutomaticDateGroup byYear = new AutomaticDateGroup("By Year", GroupHierarchyType.INCLUDING, StandardField.DATE);

        BibEntry e1 = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        BibEntry e2 = new BibEntry().withField(StandardField.DATE, "2024-01-02");
        BibEntry e3 = new BibEntry().withField(StandardField.DATE, "2023-12-31");

        var entries = FXCollections.observableArrayList(List.of(e1, e2, e3));

        // Build subgroups (merged by equals/hashCode of DateGroup)
        var nodes = byYear.createSubgroups(entries);

        // Find the "2024" bucket
        GroupTreeNode bucket2024 = nodes.stream()
                                        .filter(n -> "2024".equals(n.getName()))
                                        .findFirst()
                                        .orElseThrow();

        // It should match e1 and e2, but not e3
        var matches = bucket2024.findMatches(List.of(e1, e2, e3));
        assertEquals(2, matches.size());
        assertTrue(matches.contains(e1));
        assertTrue(matches.contains(e2));
        assertFalse(matches.contains(e3));
    }

    // MONTH granularity from DATE field (YYYY-MM-DD -> bucket "YYYY-MM")
    @Test
    void createsMonthBucketFromDateField() {
        BibEntry e = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        AutomaticDateGroup byMonth = new AutomaticDateGroup("By Month", GroupHierarchyType.INCLUDING,
                StandardField.DATE, DateGranularity.MONTH);

        var children = byMonth.createSubgroups(e);
        assertEquals(1, children.size());
        GroupTreeNode node = children.iterator().next();
        assertEquals("2024-10", node.getName());
        assertTrue(node.getGroup().contains(e));
    }

    @Test
    void mergesSameMonthAcrossEntries() {
        BibEntry e1 = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        BibEntry e2 = new BibEntry().withField(StandardField.DATE, "2024-10");
        BibEntry e3 = new BibEntry().withField(StandardField.DATE, "2024-11-01");

        AutomaticDateGroup byMonth = new AutomaticDateGroup("By Month", GroupHierarchyType.INCLUDING,
                StandardField.DATE, DateGranularity.MONTH);

        var merged = byMonth.createSubgroups(FXCollections.observableArrayList(List.of(e1, e2, e3)));
        assertEquals(2, merged.size()); // 2024-10 and 2024-11 present

        // Find the 2024-10 bucket and assert matches
        GroupTreeNode bucket = merged.stream()
                                     .filter(n -> "2024-10".equals(n.getName()))
                                     .findFirst()
                                     .orElseThrow();
        var matches = bucket.findMatches(List.of(e1, e2, e3));
        assertEquals(2, matches.size());
        assertTrue(matches.contains(e1));
        assertTrue(matches.contains(e2));
        assertFalse(matches.contains(e3));
    }

    // FULL_DATE granularity creates day bucket when full date is present
    @Test
    void createsFullDateBucketFromDateField() {
        BibEntry e = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        AutomaticDateGroup byDay = new AutomaticDateGroup("By Day", GroupHierarchyType.INCLUDING,
                StandardField.DATE, DateGranularity.FULL_DATE);

        var children = byDay.createSubgroups(e);
        assertEquals(1, children.size());
        GroupTreeNode node = children.iterator().next();
        assertEquals("2024-10-14", node.getName());
        assertTrue(node.getGroup().contains(e));
    }

    // FULL_DATE bucket matches only exact date
    @Test
    void fullDateBucketMatchesOnlyExactDate() {
        BibEntry e1 = new BibEntry().withField(StandardField.DATE, "2024-10-14");
        BibEntry e2 = new BibEntry().withField(StandardField.DATE, "2024-10-03");
        BibEntry e3 = new BibEntry().withField(StandardField.DATE, "2024-10-14");

        AutomaticDateGroup byDay = new AutomaticDateGroup("By Day", GroupHierarchyType.INCLUDING,
                StandardField.DATE, DateGranularity.FULL_DATE);

        var nodes = byDay.createSubgroups(FXCollections.observableArrayList(List.of(e1, e2, e3)));
        // after merge we should have a node "2024-10-14" and maybe one "2024-10-03"
        GroupTreeNode bucket = nodes.stream()
                                    .filter(n -> "2024-10-14".equals(n.getName()))
                                    .findFirst()
                                    .orElseThrow();

        var matches = bucket.findMatches(List.of(e1, e2, e3));
        assertEquals(2, matches.size()); // e1 and e3
        assertTrue(matches.contains(e1));
        assertTrue(matches.contains(e3));
        assertFalse(matches.contains(e2));
    }
}

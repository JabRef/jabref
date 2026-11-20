package org.jabref.logic.groups;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.search.inmemory.InMemoryLibrarySearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class GroupsFactoryTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            0, 0, RANKING, rank1, false
            0, 1, RANKING, rank2, false
            0, 2, RANKING, rank3, false
            0, 3, RANKING, rank4, false
            0, 4, RANKING, rank5, false
            1, 0, RELEVANCE, relevant, false
            1, 1, RELEVANCE, relevant, true
            2, 0, QUALITY, qualityAssured, false
            2, 1, QUALITY, qualityAssured, true
            3, 0, PRINTED, printed, false
            3, 1, PRINTED, printed, true
            4, 0, PRIORITY, prio1, false
            4, 1, PRIORITY, prio2, false
            4, 2, PRIORITY, prio3, false
            5, 0, READ_STATUS, read, false
            5, 1, READ_STATUS, skimmed, false
            """)
    void markingFiltersMatchExactValues(int parentIndex, int leafIndex, SpecialField field, String value, boolean negative) {
        BibEntry exact = new BibEntry().withField(field, value);
        BibEntry similar = new BibEntry().withField(field, "not" + value);
        BibEntry absent = new BibEntry();
        BibDatabaseContext library = new BibDatabaseContext();
        library.getDatabase().insertEntries(List.of(exact, similar, absent));
        GroupTreeNode marking = GroupsFactory.createMarkingAndGradingGroups();
        SearchGroup leaf = (SearchGroup) marking.getChildren().get(parentIndex).getChildren().get(leafIndex).getGroup();
        InMemoryLibrarySearcher searcher = createSearcher(library);

        assertEquals(negative ? Set.of(similar, absent) : Set.of(exact), Set.copyOf(searcher.getMatches(leaf.getSearchQuery())));
        SearchGroup parent = (SearchGroup) marking.getChildren().get(parentIndex).getGroup();
        assertEquals(Set.of(exact, similar), Set.copyOf(searcher.getMatches(parent.getSearchQuery())));
    }

    @Test
    void markingAndGradingGroupShowsEntriesWithAnySpecialField() {
        List<BibEntry> marked = Stream.of(SpecialField.RANKING, SpecialField.RELEVANCE, SpecialField.QUALITY,
                                              SpecialField.PRINTED, SpecialField.PRIORITY, SpecialField.READ_STATUS)
                                      .map(field -> new BibEntry().withField(field, "any"))
                                      .toList();
        BibDatabaseContext library = new BibDatabaseContext();
        library.getDatabase().insertEntries(marked);
        library.getDatabase().insertEntry(new BibEntry());
        SearchGroup marking = (SearchGroup) GroupsFactory.createMarkingAndGradingGroups().getGroup();
        InMemoryLibrarySearcher searcher = createSearcher(library);

        assertEquals(Set.copyOf(marked), Set.copyOf(searcher.getMatches(marking.getSearchQuery())));
    }

    private InMemoryLibrarySearcher createSearcher(BibDatabaseContext library) {
        BibEntryPreferences preferences = mock(BibEntryPreferences.class);
        when(preferences.getKeywordSeparator()).thenReturn(',');
        return new InMemoryLibrarySearcher(library, preferences);
    }

    /// Like the expressions of the marking and grading groups, these are stored in libraries and identify the groups there
    @Test
    void groupsWithoutFilesOrGroupsHaveStableExpressions() {
        assertEquals(List.of("file !=~.*", "groups !=~.*"),
                Stream.of(GroupsFactory.createWithoutFilesGroup(), GroupsFactory.createWithoutGroupsGroup())
                      .map(SearchGroup::getSearchExpression)
                      .toList());
    }

    /// The search expressions are stored in the libraries of the users and identify the suggested groups there.
    /// Changing one makes the existing groups unrecognizable, so that "Add JabRef suggested groups" adds them a second time.
    @Test
    // [utest->req~ux.groups.suggested-marking-groups~1]
    void markingAndGradingGroupsHaveStableExpressionsIconsAndColors() {
        String expected = """
                ranking =~.* OR relevance =~.* OR qualityassured =~.* OR printed =~.* OR priority =~.* OR readstatus =~.*
                  ranking =~.* | RANKING
                    ranking == rank1 | RANK1
                    ranking == rank2 | RANK2
                    ranking == rank3 | RANK3
                    ranking == rank4 | RANK4
                    ranking == rank5 | RANK5
                  relevance =~.* | RELEVANCE
                    relevance == relevant | RELEVANCE
                    relevance !== relevant
                  qualityassured =~.* | QUALITY
                    qualityassured == qualityAssured | QUALITY_ASSURED
                    qualityassured !== qualityAssured
                  printed =~.* | PRINTED
                    printed == printed | PRINTED
                    printed !== printed
                  priority =~.* | PRIORITY
                    priority == prio1 | PRIORITY_HIGH | #ff0000
                    priority == prio2 | PRIORITY_MEDIUM | #ffa500
                    priority == prio3 | PRIORITY_LOW | #6fcc75
                  readstatus =~.* | READ_STATUS
                    readstatus == read | READ_STATUS_READ | #6fcc75
                    readstatus == skimmed | READ_STATUS_SKIMMED | #ffa500""";

        assertEquals(expected, describe(GroupsFactory.createMarkingAndGradingGroups()));
    }

    /// Including the subgroups would make "Relevance" show the whole library, because "Not relevant" also contains the entries without the field.
    @Test
    void markingAndGradingGroupsAreIndependentSearchGroups() {
        Set<String> kinds = groups(GroupsFactory.createMarkingAndGradingGroups())
                .map(group -> group.getClass().getSimpleName() + " " + group.getHierarchicalContext())
                .collect(Collectors.toSet());

        assertEquals(Set.of(SearchGroup.class.getSimpleName() + " " + GroupHierarchyType.INDEPENDENT), kinds);
    }

    /// Group names are unique within a library, see [GroupTreeNode#findGroupByName(String)].
    @Test
    void suggestedGroupNamesAreUnique() {
        List<String> names = groups(SuggestedGroups.createSuggestions()).map(AbstractGroup::getName).toList();

        assertEquals(names.size(), Set.copyOf(names).size(), () -> "Duplicate names in " + names);
    }

    private static Stream<AbstractGroup> groups(GroupTreeNode root) {
        return root.iterateOverTree().map(GroupTreeNode::getGroup);
    }

    private static String describe(GroupTreeNode root) {
        return root.iterateOverTree()
                   .map(node -> {
                       SearchGroup group = (SearchGroup) node.getGroup();
                       return "  ".repeat(node.getLevel())
                               + group.getSearchExpression()
                               + group.getIconName().map(icon -> " | " + icon).orElse("")
                               + group.getColor().map(color -> " | " + color).orElse("");
                   })
                   .collect(Collectors.joining("\n"));
    }
}

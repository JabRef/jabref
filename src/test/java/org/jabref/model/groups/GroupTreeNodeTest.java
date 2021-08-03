package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.matchers.AndMatcher;
import org.jabref.model.search.matchers.OrMatcher;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GroupTreeNodeTest {

    private final List<BibEntry> entries = new ArrayList<>();
    private BibEntry entry;

    /**
     * Gets the marked node in the following tree of explicit groups:
     * Root
     *      A ExplicitA, Including
     *      A ExplicitParent, Independent (= parent)
     *          B ExplicitNode, Refining (<-- this)
     */
    public static GroupTreeNode getNodeInSimpleTree(GroupTreeNode root) {
        root.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode parent = root
                .addSubgroup(new ExplicitGroup("ExplicitParent", GroupHierarchyType.INDEPENDENT, ','));
        return parent.addSubgroup(new ExplicitGroup("ExplicitNode", GroupHierarchyType.REFINING, ','));
    }

    /**
     * Gets the marked node in the following tree:
     * Root
     *      A SearchA
     *      A ExplicitA, Including
     *      A ExplicitGrandParent (= grand parent)
     *          B ExplicitB
     *          B KeywordParent (= parent)
     *              C KeywordNode (<-- this)
     *                  D ExplicitChild (= child)
     *              C SearchC
     *              C ExplicitC
     *              C KeywordC
     *          B SearchB
     *          B KeywordB
     *      A KeywordA
     */
    public static GroupTreeNode getNodeInComplexTree(GroupTreeNode root) {
        root.addSubgroup(getSearchGroup("SearchA"));
        root.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode grandParent = root
                .addSubgroup(new ExplicitGroup("ExplicitGrandParent", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(getKeywordGroup("KeywordA"));

        grandParent.addSubgroup(getExplict("ExplicitB"));
        GroupTreeNode parent = grandParent.addSubgroup(getKeywordGroup("KeywordParent"));
        grandParent.addSubgroup(getSearchGroup("SearchB"));
        grandParent.addSubgroup(getKeywordGroup("KeywordB"));

        GroupTreeNode node = parent.addSubgroup(getKeywordGroup("KeywordNode"));
        parent.addSubgroup(getSearchGroup("SearchC"));
        parent.addSubgroup(getExplict("ExplicitC"));
        parent.addSubgroup(getKeywordGroup("KeywordC"));

        node.addSubgroup(getExplict("ExplicitChild"));
        return node;
    }

    private static AbstractGroup getKeywordGroup(String name) {
        return new WordKeywordGroup(name, GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "searchExpression", true, ',', false);
    }

    private static AbstractGroup getSearchGroup(String name) {
        return new SearchGroup(name, GroupHierarchyType.INCLUDING, "searchExpression", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE));
    }

    private static AbstractGroup getExplict(String name) {
        return new ExplicitGroup(name, GroupHierarchyType.REFINING, ',');
    }

    /**
     * Gets the marked in the following tree:
     * Root
     *      A
     *      A
     *      A (<- this)
     *      A
     */
    /*
    GroupTreeNode getNodeAsChild(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        root.addChild(new TreeNodeMock());
        return node;
    }
    */
    public static GroupTreeNode getRoot() {
        return GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
    }

    @BeforeEach
    void setUp() {
        entries.clear();
        entry = new BibEntry();
        entries.add(entry);
        entries.add(new BibEntry().withField(StandardField.AUTHOR, "author1 and author2"));
        entries.add(new BibEntry().withField(StandardField.AUTHOR, "author1"));
    }

    /*
    GroupTreeNode getNodeInComplexTree() {
        return getNodeInComplexTree(new TreeNodeMock());
    }
    */

    private GroupTreeNode getNodeInSimpleTree() {
        return getNodeInSimpleTree(getRoot());
    }

    @Test
    void getSearchRuleForIndependentGroupReturnsGroupAsMatcher() {
        GroupTreeNode node = GroupTreeNode
                .fromGroup(new ExplicitGroup("node", GroupHierarchyType.INDEPENDENT, ','));
        assertEquals(node.getGroup(), node.getSearchMatcher());
    }

    @Test
    void getSearchRuleForRefiningGroupReturnsParentAndGroupAsMatcher() {
        GroupTreeNode parent = GroupTreeNode
                .fromGroup(
                        new ExplicitGroup("parent", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode node = parent
                .addSubgroup(new ExplicitGroup("node", GroupHierarchyType.REFINING, ','));

        AndMatcher matcher = new AndMatcher();
        matcher.addRule(node.getGroup());
        matcher.addRule(parent.getGroup());
        assertEquals(matcher, node.getSearchMatcher());
    }

    @Test
    void getSearchRuleForIncludingGroupReturnsGroupOrSubgroupAsMatcher() {
        GroupTreeNode node = GroupTreeNode.fromGroup(new ExplicitGroup("node", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode child = node.addSubgroup(new ExplicitGroup("child", GroupHierarchyType.INDEPENDENT, ','));

        OrMatcher matcher = new OrMatcher();
        matcher.addRule(node.getGroup());
        matcher.addRule(child.getGroup());
        assertEquals(matcher, node.getSearchMatcher());
    }

    @Test
    void findMatchesReturnsEmptyForEmptyList() {
        assertEquals(Collections.emptyList(), getNodeInSimpleTree().findMatches(Collections.emptyList()));
    }

    @Test
    void findMatchesOneEntry() {
        GroupTreeNode parent = getNodeInSimpleTree();
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, "author2", true, ',', false));
        assertEquals(1, node.findMatches(entries).size());
    }

    @Test
    void findMatchesMultipleEntries() {
        GroupTreeNode parent = getNodeInSimpleTree();
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, "author1", true, ',', false));
        assertEquals(2, node.findMatches(entries).size());
    }

    @Test
    void findMatchesWorksForRefiningGroups() {
        GroupTreeNode grandParent = getNodeInSimpleTree();
        GroupTreeNode parent = grandParent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, "author2", true, ',', false));
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.REFINING, StandardField.AUTHOR, "author1", true, ',', false));
        assertEquals(1, node.findMatches(entries).size());
    }

    @Test
    void findMatchesWorksForHierarchyOfIndependentGroups() {
        GroupTreeNode grandParent = getNodeInSimpleTree();
        GroupTreeNode parent = grandParent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, "author2", true, ',', false));
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR, "author1", true, ',', false));
        assertEquals(2, node.findMatches(entries).size());
    }

    @Test
    void setGroupChangesUnderlyingGroup() {
        GroupTreeNode node = getNodeInSimpleTree();
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertEquals(newGroup, node.getGroup());
    }

    @Test
    void setGroupAddsPreviousAssignmentsExplicitToExplicit() {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertTrue(newGroup.isMatch(entry));
    }

    @Test
    void setGroupWithFalseDoesNotAddsPreviousAssignments() {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, false, false, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    void setGroupAddsOnlyPreviousAssignments() {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        assertFalse(oldGroup.isMatch(entry));
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    void setGroupExplicitToSearchDoesNotKeepPreviousAssignments() {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new SearchGroup("NewGroup", GroupHierarchyType.INDEPENDENT, "test", EnumSet.noneOf(SearchFlags.class));

        node.setGroup(newGroup, true, true, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    void setGroupExplicitToExplicitIsRenameAndSoRemovesPreviousAssignment() {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertFalse(oldGroup.isMatch(entry));
    }

    @Test
    void getChildByPathFindsCorrectChildInSecondLevel() {
        GroupTreeNode root = getRoot();
        GroupTreeNode child = getNodeInSimpleTree(root);

        assertEquals(Optional.of(child), root.getChildByPath("ExplicitParent > ExplicitNode"));
    }

    @Test
    void getChildByPathDoesNotFindChildWhenInvalidPath() {
        GroupTreeNode root = getRoot();

        // use side effect of method, which builds the group tree
        getNodeInSimpleTree(root);

        assertEquals(Optional.empty(), root.getChildByPath("ExplicitParent > ExplicitChildNode"));
    }

    @Test
    void getPathSimpleTree() {
        GroupTreeNode node = getNodeInSimpleTree();

        assertEquals("ExplicitParent > ExplicitNode", node.getPath());
    }

    @Test
    void onlyRootAndChildNodeContainAtLeastOneEntry() {
        GroupTreeNode rootNode = getRoot();
        rootNode.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode parent = rootNode
                .addSubgroup(new ExplicitGroup("ExplicitParent", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode child = parent.addSubgroup(new ExplicitGroup("ExplicitNode", GroupHierarchyType.REFINING, ','));

        BibEntry newEntry = new BibEntry().withField(StandardField.AUTHOR, "Stephen King");
        child.addEntriesToGroup(Collections.singletonList(newEntry));
        entries.add(newEntry);

        assertEquals(rootNode.getContainingGroups(entries, false), Arrays.asList(rootNode, child));
    }

    @Test
    void onlySubgroupsContainAllEntries() {
        GroupTreeNode rootNode = getRoot();
        rootNode.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode parent = rootNode
                .addSubgroup(new ExplicitGroup("ExplicitParent", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode firstChild = parent.addSubgroup(new ExplicitGroup("ExplicitNode", GroupHierarchyType.REFINING, ','));
        GroupTreeNode secondChild = parent.addSubgroup(new ExplicitGroup("ExplicitSecondNode", GroupHierarchyType.REFINING, ','));
        GroupTreeNode grandChild = secondChild.addSubgroup(new ExplicitGroup("ExplicitNodeThirdLevel", GroupHierarchyType.REFINING, ','));

        parent.addEntriesToGroup(Collections.singletonList(entry));
        firstChild.addEntriesToGroup(entries);
        secondChild.addEntriesToGroup(entries);
        grandChild.addEntriesToGroup(entries);
        assertEquals(parent.getContainingGroups(entries, true), Arrays.asList(firstChild, secondChild, grandChild));
    }

    @Test
    void addEntriesToGroupWorksNotForGroupsNotSupportingExplicitAddingOfEntries() {
        GroupTreeNode searchGroup = new GroupTreeNode(new SearchGroup("Search A", GroupHierarchyType.INCLUDING, "searchExpression", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)));
        List<FieldChange> fieldChanges = searchGroup.addEntriesToGroup(entries);

        assertEquals(Collections.emptyList(), fieldChanges);
    }

    @Test
    void removeEntriesFromGroupWorksNotForGroupsNotSupportingExplicitRemovalOfEntries() {
        GroupTreeNode searchGroup = new GroupTreeNode(new SearchGroup("Search A", GroupHierarchyType.INCLUDING, "searchExpression", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)));
        List<FieldChange> fieldChanges = searchGroup.removeEntriesFromGroup(entries);

        assertEquals(Collections.emptyList(), fieldChanges);
    }
}

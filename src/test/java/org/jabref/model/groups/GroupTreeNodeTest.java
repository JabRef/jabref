package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.matchers.AndMatcher;
import org.jabref.model.search.matchers.OrMatcher;

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
        return new WordKeywordGroup(name, GroupHierarchyType.INDEPENDENT, "searchField", "searchExpression", true,',', false);
    }

    private static AbstractGroup getSearchGroup(String name) {
        return new SearchGroup(name, GroupHierarchyType.INCLUDING, "searchExpression", true, false);
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
    public GroupTreeNode getNodeAsChild(TreeNodeMock root) {
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
    public void setUp() throws Exception {
        entries.clear();
        entry = new BibEntry();
        entries.add(entry);
        entries.add(new BibEntry().withField("author", "author1 and author2"));
        entries.add(new BibEntry().withField("author", "author1"));
    }

    /*
    public GroupTreeNode getNodeInComplexTree() {
        return getNodeInComplexTree(new TreeNodeMock());
    }
    */

    private GroupTreeNode getNodeInSimpleTree() {
        return getNodeInSimpleTree(getRoot());
    }

    @Test
    public void getSearchRuleForIndependentGroupReturnsGroupAsMatcher() {
        GroupTreeNode node = GroupTreeNode
                .fromGroup(new ExplicitGroup("node", GroupHierarchyType.INDEPENDENT, ','));
        assertEquals(node.getGroup(), node.getSearchMatcher());
    }

    @Test
    public void getSearchRuleForRefiningGroupReturnsParentAndGroupAsMatcher() {
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
    public void getSearchRuleForIncludingGroupReturnsGroupOrSubgroupAsMatcher() {
        GroupTreeNode node = GroupTreeNode.fromGroup(new ExplicitGroup("node", GroupHierarchyType.INCLUDING, ','));
        GroupTreeNode child = node.addSubgroup(new ExplicitGroup("child", GroupHierarchyType.INDEPENDENT, ','));

        OrMatcher matcher = new OrMatcher();
        matcher.addRule(node.getGroup());
        matcher.addRule(child.getGroup());
        assertEquals(matcher, node.getSearchMatcher());
    }

    @Test
    public void numberOfHitsReturnsZeroForEmptyList() throws Exception {
        assertEquals(0, getNodeInSimpleTree().calculateNumberOfMatches(Collections.emptyList()));
    }

    @Test
    public void numberOfHitsMatchesOneEntry() throws Exception {
        GroupTreeNode parent = getNodeInSimpleTree();
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, "author", "author2", true, ',', false));
        assertEquals(1, node.calculateNumberOfMatches(entries));
    }

    @Test
    public void numberOfHitsMatchesMultipleEntries() throws Exception {
        GroupTreeNode parent = getNodeInSimpleTree();
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, "author", "author1", true, ',', false));
        assertEquals(2, node.calculateNumberOfMatches(entries));
    }

    @Test
    public void numberOfHitsWorksForRefiningGroups() throws Exception {
        GroupTreeNode grandParent = getNodeInSimpleTree();
        GroupTreeNode parent = grandParent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, "author", "author2", true, ',', false));
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.REFINING, "author", "author1", true, ',', false));
        assertEquals(1, node.calculateNumberOfMatches(entries));
    }

    @Test
    public void numberOfHitsWorksForHierarchyOfIndependentGroups() throws Exception {
        GroupTreeNode grandParent = getNodeInSimpleTree();
        GroupTreeNode parent = grandParent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, "author", "author2", true, ',', false));
        GroupTreeNode node = parent.addSubgroup(
                new WordKeywordGroup("node", GroupHierarchyType.INDEPENDENT, "author", "author1", true, ',', false));
        assertEquals(2, node.calculateNumberOfMatches(entries));
    }

    @Test
    public void setGroupChangesUnderlyingGroup() throws Exception {
        GroupTreeNode node = getNodeInSimpleTree();
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertEquals(newGroup, node.getGroup());
    }

    @Test
    public void setGroupAddsPreviousAssignmentsExplicitToExplicit() throws Exception {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertTrue(newGroup.isMatch(entry));
    }

    @Test
    public void setGroupWithFalseDoesNotAddsPreviousAssignments() throws Exception {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, false, false, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    public void setGroupAddsOnlyPreviousAssignments() throws Exception {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        assertFalse(oldGroup.isMatch(entry));
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    public void setGroupExplicitToSearchDoesNotKeepPreviousAssignments() throws Exception {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new SearchGroup("NewGroup", GroupHierarchyType.INDEPENDENT, "test", false, false);

        node.setGroup(newGroup, true, true, entries);

        assertFalse(newGroup.isMatch(entry));
    }

    @Test
    public void setGroupExplicitToExplicitIsRenameAndSoRemovesPreviousAssignment() throws Exception {
        ExplicitGroup oldGroup = new ExplicitGroup("OldGroup", GroupHierarchyType.INDEPENDENT, ',');
        oldGroup.add(entry);
        GroupTreeNode node = GroupTreeNode.fromGroup(oldGroup);
        AbstractGroup newGroup = new ExplicitGroup("NewGroup", GroupHierarchyType.INDEPENDENT, ',');

        node.setGroup(newGroup, true, true, entries);

        assertFalse(oldGroup.isMatch(entry));
    }

    @Test
    public void getChildByPathFindsCorrectChildInSecondLevel() throws Exception {
        GroupTreeNode root = getRoot();
        GroupTreeNode child = getNodeInSimpleTree(root);

        assertEquals(Optional.of(child), root.getChildByPath("ExplicitParent > ExplicitNode"));
    }

    @Test
    public void getPathSimpleTree() throws Exception {
        GroupTreeNode node = getNodeInSimpleTree();

        assertEquals("ExplicitParent > ExplicitNode", node.getPath());
    }
}

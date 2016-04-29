package net.sf.jabref.logic.groups;

import java.util.Arrays;
import java.util.List;

import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.search.matchers.AndMatcher;
import net.sf.jabref.logic.search.matchers.OrMatcher;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupTreeNodeTest {

    /**
     * Gets the marked node in the following tree of explicit groups:
     * Root
     *      A ExplicitA, Including
     *      A ExplicitParent, Independent (= parent)
     *          B ExplicitNode, Refining (<-- this)
     */
    public GroupTreeNode getNodeInSimpleTree(GroupTreeNode root) throws ParseException {
        root.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING));
        GroupTreeNode parent = root.addSubgroup(new ExplicitGroup("ExplicitParent", GroupHierarchyType.INDEPENDENT));
        GroupTreeNode node = parent.addSubgroup(new ExplicitGroup("ExplicitNode", GroupHierarchyType.REFINING));
        return node;
    }

    public GroupTreeNode getNodeInSimpleTree() throws ParseException {
        return getNodeInSimpleTree(getRoot());
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
    public GroupTreeNode getNodeInComplexTree(GroupTreeNode root) throws ParseException {
        root.addSubgroup(getSearchGroup("SearchA"));
        root.addSubgroup(new ExplicitGroup("ExplicitA", GroupHierarchyType.INCLUDING));
        GroupTreeNode grandParent = root.addSubgroup(new ExplicitGroup("ExplicitGrandParent", GroupHierarchyType.INDEPENDENT));
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

    private AbstractGroup getKeywordGroup(String name) throws ParseException {
        return new KeywordGroup(name, "searchField", "searchExpression", true, false, GroupHierarchyType.INDEPENDENT);
    }

    private AbstractGroup getSearchGroup(String name) {
        return new SearchGroup(name, "searchExpression", true, false, GroupHierarchyType.INCLUDING);
    }

    private AbstractGroup getExplict(String name) throws ParseException {
        return new ExplicitGroup(name, GroupHierarchyType.REFINING);
    }

    /*
    public GroupTreeNode getNodeInComplexTree() {
        return getNodeInComplexTree(new TreeNodeMock());
    }
    */

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

    public GroupTreeNode getRoot() {
        return new GroupTreeNode(new AllEntriesGroup());
    }

    @Test
    public void getTreeAsStringInSimpleTree() throws Exception {
        GroupTreeNode root = getRoot();
        getNodeInSimpleTree(root);

        List<String> expected = Arrays.asList(
                "0 AllEntriesGroup:",
                "1 ExplicitGroup:ExplicitA;2;",
                "1 ExplicitGroup:ExplicitParent;0;",
                "2 ExplicitGroup:ExplicitNode;1;"
        );
        assertEquals(expected, root.getTreeAsString());
    }

    @Test
    public void getTreeAsStringInComplexTree() throws Exception {
        GroupTreeNode root = getRoot();
        getNodeInComplexTree(root);

        List<String> expected = Arrays.asList(
                "0 AllEntriesGroup:",
                "1 SearchGroup:SearchA;2;searchExpression;1;0;",
                "1 ExplicitGroup:ExplicitA;2;",
                "1 ExplicitGroup:ExplicitGrandParent;0;",
                "2 ExplicitGroup:ExplicitB;1;",
                "2 KeywordGroup:KeywordParent;0;searchField;searchExpression;1;0;",
                "3 KeywordGroup:KeywordNode;0;searchField;searchExpression;1;0;",
                "4 ExplicitGroup:ExplicitChild;1;",
                "3 SearchGroup:SearchC;2;searchExpression;1;0;",
                "3 ExplicitGroup:ExplicitC;1;",
                "3 KeywordGroup:KeywordC;0;searchField;searchExpression;1;0;",
                "2 SearchGroup:SearchB;2;searchExpression;1;0;",
                "2 KeywordGroup:KeywordB;0;searchField;searchExpression;1;0;",
                "1 KeywordGroup:KeywordA;0;searchField;searchExpression;1;0;"
        );
        assertEquals(expected, root.getTreeAsString());
    }

    @Test
    public void getSearchRuleForIndependentGroupReturnsGroupAsMatcher() throws ParseException {
        GroupTreeNode node = new GroupTreeNode(new ExplicitGroup("node", GroupHierarchyType.INDEPENDENT));
        assertEquals(node.getGroup(), node.getSearchRule());
    }

    @Test
    public void getSearchRuleForRefiningGroupReturnsParentAndGroupAsMatcher() throws ParseException {
        GroupTreeNode parent = new GroupTreeNode(new ExplicitGroup("parent", GroupHierarchyType.INDEPENDENT));
        GroupTreeNode node = parent.addSubgroup(new ExplicitGroup("node", GroupHierarchyType.REFINING));

        AndMatcher matcher = new AndMatcher();
        matcher.addRule(node.getGroup());
        matcher.addRule(parent.getGroup());
        assertEquals(matcher, node.getSearchRule());
    }

    @Test
    public void getSearchRuleForIncludingGroupReturnsGroupOrSubgroupAsMatcher() throws ParseException {
        GroupTreeNode node = new GroupTreeNode(new ExplicitGroup("node", GroupHierarchyType.INCLUDING));
        GroupTreeNode child = node.addSubgroup(new ExplicitGroup("child", GroupHierarchyType.INDEPENDENT));

        OrMatcher matcher = new OrMatcher();
        matcher.addRule(node.getGroup());
        matcher.addRule(child.getGroup());
        assertEquals(matcher, node.getSearchRule());
    }
}

package net.sf.jabref.logic.exporter;

import java.util.Arrays;
import java.util.List;

import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.GroupTreeNodeTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupSerializerTest {

    @Test
    public void getTreeAsStringInSimpleTree() throws Exception {
        GroupTreeNode root = GroupTreeNodeTest.getRoot();
        GroupTreeNodeTest.getNodeInSimpleTree(root);

        List<String> expected = Arrays.asList(
                "0 AllEntriesGroup:",
                "1 ExplicitGroup:ExplicitA;2;",
                "1 ExplicitGroup:ExplicitParent;0;",
                "2 ExplicitGroup:ExplicitNode;1;"
        );
        assertEquals(expected, new GroupSerializer().serializeTree(root));
    }

    @Test
    public void getTreeAsStringInComplexTree() throws Exception {
        GroupTreeNode root = GroupTreeNodeTest.getRoot();
        GroupTreeNodeTest.getNodeInComplexTree(root);

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
        assertEquals(expected, new GroupSerializer().serializeTree(root));
    }

}

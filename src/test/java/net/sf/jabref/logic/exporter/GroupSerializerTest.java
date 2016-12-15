package net.sf.jabref.logic.exporter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.GroupTreeNodeTest;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.RegexKeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.model.groups.WordKeywordGroup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupSerializerTest {

    private GroupSerializer groupSerializer;

    @Before
    public void setUp() throws Exception {
        groupSerializer = new GroupSerializer();
    }

    @Test
    public void serializeSingleAllEntriesGroup() {
        AllEntriesGroup group = new AllEntriesGroup("");
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 AllEntriesGroup:"), serialization);
    }

    @Test
    public void serializeSingleExplicitGroup() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 ExplicitGroup:myExplicitGroup;0;"), serialization);
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1681
    public void serializeSingleExplicitGroupWithEscapedSlash() {
        ExplicitGroup group = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 ExplicitGroup:B{\\\\\"{o}}hmer;0;"), serialization);
    }

    @Test
    public void serializeSingleSimpleKeywordGroup() {
        WordKeywordGroup group = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, "keywords", "test", false, ',', false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 KeywordGroup:name;0;keywords;test;0;0;"), serialization);
    }

    @Test
    public void serializeSingleRegexKeywordGroup() {
        KeywordGroup group = new RegexKeywordGroup("myExplicitGroup", GroupHierarchyType.REFINING, "author", "asdf", false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 KeywordGroup:myExplicitGroup;1;author;asdf;0;1;"), serialization);
    }

    @Test
    public void serializeSingleSearchGroup() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "author=harrer", true, true);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 SearchGroup:myExplicitGroup;0;author=harrer;1;1;"), serialization);
    }

    @Test
    public void serializeSingleSearchGroupWithRegex() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, "author=\"harrer\"", true, false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 SearchGroup:myExplicitGroup;2;author=\"harrer\";1;0;"), serialization);
    }

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
        assertEquals(expected, groupSerializer.serializeTree(root));
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
        assertEquals(expected, groupSerializer.serializeTree(root));
    }

}

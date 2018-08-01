package org.jabref.logic.exporter;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.scene.paint.Color;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupSerializerTest {

    private GroupSerializer groupSerializer;

    @BeforeEach
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
        assertEquals(Collections.singletonList("0 StaticGroup:myExplicitGroup;0;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleExplicitGroupWithIconAndDescription() {
        ExplicitGroup group = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        group.setIconName("test icon");
        group.setExpanded(true);
        group.setColor(Color.ALICEBLUE);
        group.setDescription("test description");
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 StaticGroup:myExplicitGroup;0;1;0xf0f8ffff;test icon;test description;"), serialization);
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1681
    public void serializeSingleExplicitGroupWithEscapedSlash() {
        ExplicitGroup group = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 StaticGroup:B{\\\\\"{o}}hmer;0;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleSimpleKeywordGroup() {
        WordKeywordGroup group = new WordKeywordGroup("name", GroupHierarchyType.INDEPENDENT, "keywords", "test", false, ',', false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 KeywordGroup:name;0;keywords;test;0;0;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleRegexKeywordGroup() {
        KeywordGroup group = new RegexKeywordGroup("myExplicitGroup", GroupHierarchyType.REFINING, "author", "asdf", false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 KeywordGroup:myExplicitGroup;1;author;asdf;0;1;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleSearchGroup() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, "author=harrer", true, true);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 SearchGroup:myExplicitGroup;0;author=harrer;1;1;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleSearchGroupWithRegex() {
        SearchGroup group = new SearchGroup("myExplicitGroup", GroupHierarchyType.INCLUDING, "author=\"harrer\"", true, false);
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 SearchGroup:myExplicitGroup;2;author=\"harrer\";1;0;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleAutomaticKeywordGroup() {
        AutomaticGroup group = new AutomaticKeywordGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, "keywords", ',', '>');
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 AutomaticKeywordGroup:myAutomaticGroup;0;keywords;,;>;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleAutomaticPersonGroup() {
        AutomaticPersonsGroup group = new AutomaticPersonsGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, "authors");
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 AutomaticPersonsGroup:myAutomaticGroup;0;authors;1;;;;"), serialization);
    }

    @Test
    public void serializeSingleTexGroup() throws Exception {
        TexGroup group = new TexGroup("myTexGroup", GroupHierarchyType.INDEPENDENT, Paths.get("path", "To", "File"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor());
        List<String> serialization = groupSerializer.serializeTree(GroupTreeNode.fromGroup(group));
        assertEquals(Collections.singletonList("0 TexGroup:myTexGroup;0;path/To/File;1;;;;"), serialization);
    }

    @Test
    public void getTreeAsStringInSimpleTree() throws Exception {
        GroupTreeNode root = getRoot();
        getNodeInSimpleTree(root);

        List<String> expected = Arrays.asList(
                "0 AllEntriesGroup:",
                "1 StaticGroup:ExplicitA;2;1;;;;",
                "1 StaticGroup:ExplicitParent;0;1;;;;",
                "2 StaticGroup:ExplicitNode;1;1;;;;"
        );
        assertEquals(expected, groupSerializer.serializeTree(root));
    }

    @Test
    public void getTreeAsStringInComplexTree() throws Exception {
        GroupTreeNode root = getRoot();
        getNodeInComplexTree(root);

        List<String> expected = Arrays.asList(
                "0 AllEntriesGroup:",
                "1 SearchGroup:SearchA;2;searchExpression;1;0;1;;;;",
                "1 StaticGroup:ExplicitA;2;1;;;;",
                "1 StaticGroup:ExplicitGrandParent;0;1;;;;",
                "2 StaticGroup:ExplicitB;1;1;;;;",
                "2 KeywordGroup:KeywordParent;0;searchField;searchExpression;1;0;1;;;;",
                "3 KeywordGroup:KeywordNode;0;searchField;searchExpression;1;0;1;;;;",
                "4 StaticGroup:ExplicitChild;1;1;;;;",
                "3 SearchGroup:SearchC;2;searchExpression;1;0;1;;;;",
                "3 StaticGroup:ExplicitC;1;1;;;;",
                "3 KeywordGroup:KeywordC;0;searchField;searchExpression;1;0;1;;;;",
                "2 SearchGroup:SearchB;2;searchExpression;1;0;1;;;;",
                "2 KeywordGroup:KeywordB;0;searchField;searchExpression;1;0;1;;;;",
                "1 KeywordGroup:KeywordA;0;searchField;searchExpression;1;0;1;;;;"
        );
        assertEquals(expected, groupSerializer.serializeTree(root));
    }

    // TODO: Java 9: following code is duplicated from GroupTreeNodeTest in the test sources of jabref-model, which became inaccessible

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
}

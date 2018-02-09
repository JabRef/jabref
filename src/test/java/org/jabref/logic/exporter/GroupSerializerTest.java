package org.jabref.logic.exporter;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.scene.paint.Color;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.GroupTreeNodeTest;
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
        GroupTreeNode root = GroupTreeNodeTest.getRoot();
        GroupTreeNodeTest.getNodeInSimpleTree(root);

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
        GroupTreeNode root = GroupTreeNodeTest.getRoot();
        GroupTreeNodeTest.getNodeInComplexTree(root);

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

}

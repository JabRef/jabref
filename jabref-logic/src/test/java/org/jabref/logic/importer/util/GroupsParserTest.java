package org.jabref.logic.importer.util;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javafx.scene.paint.Color;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GroupsParserTest {
    private FileUpdateMonitor fileMonitor;

    @BeforeEach
    public void setUp() throws Exception {
        fileMonitor = new DummyFileUpdateMonitor();
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1681
    public void fromStringParsesExplicitGroupWithEscapedCharacterInName() throws Exception {
        ExplicitGroup expected = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup parsed = GroupsParser.fromString("ExplicitGroup:B{\\\\\"{o}}hmer;0;", ',', fileMonitor);

        assertEquals(expected, parsed);
    }

    @Test
    public void keywordDelimiterThatNeedsToBeEscaped() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, "keywords", ';', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;\\;;>;1;;;;;", ';', fileMonitor);
        assertEquals(expected, parsed);
    }

    @Test
    public void hierarchicalDelimiterThatNeedsToBeEscaped() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, "keywords", ',', ';');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;,;\\;;1;;;;;", ';', fileMonitor);
        assertEquals(expected, parsed);
    }

    @Test
    public void fromStringThrowsParseExceptionForNotEscapedGroupName() throws Exception {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("ExplicitGroup:slit\\\\;0\\;mertsch_slit2_2007\\;;", ',', fileMonitor));
    }

    @Test
    public void testImportSubGroups() throws Exception {

        List<String> orderedData = Arrays.asList("0 AllEntriesGroup:", "1 ExplicitGroup:1;0;",
                "2 ExplicitGroup:2;0;", "0 ExplicitGroup:3;0;");
        //Create group hierarchy:
        //  Level 0 Name: All entries
        //  Level 1 Name: 1
        //  Level 2 Name: 2
        //  Level 1 Name: 3

        GroupTreeNode rootNode = new GroupTreeNode(
                new ExplicitGroup("All entries", GroupHierarchyType.INDEPENDENT, ','));

        AbstractGroup firstSubGrpLvl1 = new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ',');
        rootNode.addSubgroup(firstSubGrpLvl1);

        AbstractGroup subLvl2 = new ExplicitGroup("2", GroupHierarchyType.INDEPENDENT, ',');
        rootNode.getFirstChild().ifPresent(c -> c.addSubgroup(subLvl2));

        AbstractGroup thirdSubGrpLvl1 = new ExplicitGroup("3", GroupHierarchyType.INDEPENDENT, ',');
        rootNode.addSubgroup(thirdSubGrpLvl1);

        GroupTreeNode parsedNode = GroupsParser.importGroups(orderedData, ',', fileMonitor);
        assertEquals(rootNode.getChildren(), parsedNode.getChildren());

    }

    @Test
    public void fromStringParsesExplicitGroupWithIconAndDescription() throws Exception {
        ExplicitGroup expected = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        expected.setIconName("test icon");
        expected.setExpanded(true);
        expected.setColor(Color.ALICEBLUE);
        expected.setDescription("test description");
        AbstractGroup parsed = GroupsParser.fromString("StaticGroup:myExplicitGroup;0;1;0xf0f8ffff;test icon;test description;", ',', fileMonitor);

        assertEquals(expected, parsed);
    }

    @Test
    public void fromStringParsesAutomaticKeywordGroup() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, "keywords", ',', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:myAutomaticGroup;0;keywords;,;>;1;;;;", ',', fileMonitor);
        assertEquals(expected, parsed);
    }

    @Test
    public void fromStringParsesAutomaticPersonGroup() throws Exception {
        AutomaticPersonsGroup expected = new AutomaticPersonsGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, "authors");
        AbstractGroup parsed = GroupsParser.fromString("AutomaticPersonsGroup:myAutomaticGroup;0;authors;1;;;;", ',', fileMonitor);
        assertEquals(expected, parsed);
    }

    @Test
    public void fromStringParsesTexGroup() throws Exception {
        TexGroup expected = new TexGroup("myTexGroup", GroupHierarchyType.INDEPENDENT, Paths.get("path", "To", "File"), new DefaultAuxParser(new BibDatabase()), fileMonitor);
        AbstractGroup parsed = GroupsParser.fromString("TexGroup:myTexGroup;0;path/To/File;1;;;;", ',', fileMonitor);
        assertEquals(expected, parsed);
    }

    @Test
    public void fromStringUnknownGroupThrowsException() throws Exception {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("0 UnknownGroup:myUnknownGroup;0;;1;;;;", ',', fileMonitor));
    }
}

package org.jabref.logic.importer.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javafx.scene.paint.Color;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupsParserTest {
    private FileUpdateMonitor fileMonitor;
    private MetaData metaData;

    @BeforeEach
    void setUp() throws Exception {
        fileMonitor = new DummyFileUpdateMonitor();
        metaData = new MetaData();
    }

    @Test
    // For https://github.com/JabRef/jabref/issues/1681
    void fromStringParsesExplicitGroupWithEscapedCharacterInName() throws Exception {
        ExplicitGroup expected = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup parsed = GroupsParser.fromString("ExplicitGroup:B{\\\\\"{o}}hmer;0;", ',', fileMonitor, metaData);

        assertEquals(expected, parsed);
    }

    @Test
    void keywordDelimiterThatNeedsToBeEscaped() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ';', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;\\;;>;1;;;;;", ';', fileMonitor, metaData);
        assertEquals(expected, parsed);
    }

    @Test
    void hierarchicalDelimiterThatNeedsToBeEscaped() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', ';');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;,;\\;;1;;;;;", ';', fileMonitor, metaData);
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringThrowsParseExceptionForNotEscapedGroupName() throws Exception {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("ExplicitGroup:slit\\\\;0\\;mertsch_slit2_2007\\;;", ',', fileMonitor, metaData));
    }

    @Test
    void testImportSubGroups() throws Exception {

        List<String> orderedData = Arrays.asList("0 AllEntriesGroup:", "1 ExplicitGroup:1;0;",
                "2 ExplicitGroup:2;0;", "0 ExplicitGroup:3;0;");
        // Create group hierarchy:
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

        GroupTreeNode parsedNode = GroupsParser.importGroups(orderedData, ',', fileMonitor, metaData);
        assertEquals(rootNode.getChildren(), parsedNode.getChildren());
    }

    @Test
    void fromStringParsesExplicitGroupWithIconAndDescription() throws Exception {
        ExplicitGroup expected = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        expected.setIconName("test icon");
        expected.setExpanded(true);
        expected.setColor(Color.ALICEBLUE);
        expected.setDescription("test description");
        AbstractGroup parsed = GroupsParser.fromString("StaticGroup:myExplicitGroup;0;1;0xf0f8ffff;test icon;test description;", ',', fileMonitor, metaData);

        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticKeywordGroup() throws Exception {
        AutomaticGroup expected = new AutomaticKeywordGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:myAutomaticGroup;0;keywords;,;>;1;;;;", ',', fileMonitor, metaData);
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticPersonGroup() throws Exception {
        AutomaticPersonsGroup expected = new AutomaticPersonsGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR);
        AbstractGroup parsed = GroupsParser.fromString("AutomaticPersonsGroup:myAutomaticGroup;0;author;1;;;;", ',', fileMonitor, metaData);
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesTexGroup() throws Exception {
        TexGroup expected = TexGroup.createWithoutFileMonitoring("myTexGroup", GroupHierarchyType.INDEPENDENT, Path.of("path", "To", "File"), new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData);
        AbstractGroup parsed = GroupsParser.fromString("TexGroup:myTexGroup;0;path/To/File;1;;;;", ',', fileMonitor, metaData);
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringUnknownGroupThrowsException() throws Exception {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("0 UnknownGroup:myUnknownGroup;0;;1;;;;", ',', fileMonitor, metaData));
    }

    @Test
    void fromStringParsesSearchGroup() throws Exception {
        SearchGroup expected = new SearchGroup("Data", GroupHierarchyType.INCLUDING, "project=data|number|quant*", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));
        AbstractGroup parsed = GroupsParser.fromString("SearchGroup:Data;2;project=data|number|quant*;0;1;1;;;;;", ',', fileMonitor, metaData);
        assertEquals(expected, parsed);

    }
}

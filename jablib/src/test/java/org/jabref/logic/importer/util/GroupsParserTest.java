package org.jabref.logic.importer.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javafx.scene.paint.Color;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticDateGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.DateGranularity;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupsParserTest {
    private FileUpdateMonitor fileMonitor;
    private DirectoryUpdateMonitor directoryUpdateMonitor;
    private BibDatabaseContext database;

    @BeforeEach
    void setUp() {
        fileMonitor = new DummyFileUpdateMonitor();
        directoryUpdateMonitor = new DummyDirectoryUpdateMonitor();
        database = new BibDatabaseContext();
    }

    // For https://github.com/JabRef/jabref/issues/1681
    @Test
    void fromStringParsesExplicitGroupWithEscapedCharacterInName() throws ParseException {
        ExplicitGroup expected = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup parsed = GroupsParser.fromString("ExplicitGroup:B{\\\\\"{o}}hmer;0;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");

        assertEquals(expected, parsed);
    }

    @Test
    void keywordDelimiterThatNeedsToBeEscaped() throws ParseException {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ';', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;\\;;>;1;;;;;", ';', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void hierarchicalDelimiterThatNeedsToBeEscaped() throws ParseException {
        AutomaticGroup expected = new AutomaticKeywordGroup("group1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', ';');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:group1;0;keywords;,;\\;;1;;;;;", ';', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringThrowsParseExceptionForNotEscapedGroupName() {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("ExplicitGroup:slit\\\\;0\\;mertsch_slit2_2007\\;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost"));
    }

    @Test
    void importSubGroups() throws ParseException {

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

        GroupTreeNode parsedNode = GroupsParser.importGroups(orderedData, ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(rootNode.getChildren(), parsedNode.getChildren());
    }

    @Test
    void fromStringParsesExplicitGroupWithIconAndDescription() throws ParseException {
        ExplicitGroup expected = new ExplicitGroup("myExplicitGroup", GroupHierarchyType.INDEPENDENT, ',');
        expected.setIconName("test icon");
        expected.setExpanded(true);
        expected.setColor(Color.ALICEBLUE.toString());
        expected.setDescription("test description");
        AbstractGroup parsed = GroupsParser.fromString("StaticGroup:myExplicitGroup;0;1;0xf0f8ffff;test icon;test description;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");

        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticKeywordGroup() throws ParseException {
        AutomaticGroup expected = new AutomaticKeywordGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        AbstractGroup parsed = GroupsParser.fromString("AutomaticKeywordGroup:myAutomaticGroup;0;keywords;,;>;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticPersonGroup() throws ParseException {
        AutomaticPersonsGroup expected = new AutomaticPersonsGroup("myAutomaticGroup", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR);
        AbstractGroup parsed = GroupsParser.fromString("AutomaticPersonsGroup:myAutomaticGroup;0;author;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesTexGroup() throws ParseException, IOException {
        TexGroup expected = TexGroup.create("myTexGroup", GroupHierarchyType.INDEPENDENT, Path.of("path", "To", "File"), new DefaultAuxParser(new BibDatabase()), database.getMetaData(), "userAndHost");
        AbstractGroup parsed = GroupsParser.fromString("TexGroup:myTexGroup;0;path/To/File;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringUnknownGroupThrowsException() {
        assertThrows(ParseException.class, () -> GroupsParser.fromString("0 UnknownGroup:myUnknownGroup;0;;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost"));
    }

    @Test
    void fromStringParsesSearchGroup() throws ParseException {
        SearchGroup expected = new SearchGroup("Data", GroupHierarchyType.INCLUDING, "project=data|number|quant*", EnumSet.of(SearchFlags.REGULAR_EXPRESSION));
        AbstractGroup parsed = GroupsParser.fromString("SearchGroup:Data;2;project=data|number|quant*;0;1;1;;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticDateGroupWithYearGranularity() throws ParseException {
        AutomaticDateGroup expected = new AutomaticDateGroup("By Year", GroupHierarchyType.INDEPENDENT, StandardField.DATE, DateGranularity.YEAR);
        AbstractGroup parsed = GroupsParser.fromString("AutomaticDateGroup:By Year;0;date;YEAR;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticDateGroupWithMonthGranularity() throws ParseException {
        AutomaticDateGroup expected = new AutomaticDateGroup("By Month", GroupHierarchyType.INCLUDING, StandardField.YEAR, DateGranularity.MONTH);
        AbstractGroup parsed = GroupsParser.fromString("AutomaticDateGroup:By Month;2;year;MONTH;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticDateGroupWithFullDateGranularity() throws ParseException {
        AutomaticDateGroup expected = new AutomaticDateGroup("By Date", GroupHierarchyType.REFINING, StandardField.DATE, DateGranularity.FULL_DATE);
        AbstractGroup parsed = GroupsParser.fromString("AutomaticDateGroup:By Date;1;date;FULL_DATE;1;;;;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }

    @Test
    void fromStringParsesAutomaticDateGroupWithColorAndIcon() throws ParseException {
        AutomaticDateGroup expected = new AutomaticDateGroup("Publications", GroupHierarchyType.INDEPENDENT, StandardField.YEAR, DateGranularity.YEAR);
        expected.setColor(Color.BLUE.toString());
        expected.setIconName("calendar");
        expected.setDescription("Group by publication year");
        AbstractGroup parsed = GroupsParser.fromString("AutomaticDateGroup:Publications;0;year;YEAR;1;0x0000ffff;calendar;Group by publication year;", ',', fileMonitor, directoryUpdateMonitor, database, "userAndHost");
        assertEquals(expected, parsed);
    }
}

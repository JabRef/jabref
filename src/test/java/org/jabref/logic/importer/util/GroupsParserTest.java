package org.jabref.logic.importer.util;

import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupsParserTest {

    @Test
    // For https://github.com/JabRef/jabref/issues/1681
    public void fromStringParsesExplicitGroupWithEscapedCharacterInName() throws Exception {
        ExplicitGroup expected = new ExplicitGroup("B{\\\"{o}}hmer", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup parsed = GroupsParser.fromString("ExplicitGroup:B{\\\\\"{o}}hmer;0;", ',');

        assertEquals(expected, parsed);
    }

    @Test(expected = ParseException.class)
    public void fromStringThrowsParseExceptionForNotEscapedGroupName() throws Exception {
        GroupsParser.fromString("ExplicitGroup:slit\\\\;0\\;mertsch_slit2_2007\\;;", ',');
    }

    @Test
    public void testImportSubGroups() throws ParseException {

        List<String> orderedData = Arrays.asList("0 AllEntriesGroup:", "1 ExplicitGroup:1;0;",
                "2 ExplicitGroup:2;0;;", "0 ExplicitGroup:3;0;;");
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

        GroupTreeNode parsedNode = GroupsParser.importGroups(orderedData, ',');
        assertEquals(rootNode.getChildren(), parsedNode.getChildren());

    }

}

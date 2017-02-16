package net.sf.jabref.logic.importer.util;

import net.sf.jabref.logic.importer.ParseException;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;

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
}

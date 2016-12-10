package net.sf.jabref.logic.importer.util;

import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;

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

}

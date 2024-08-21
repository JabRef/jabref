package org.jabref.model.groups;

import java.nio.file.Path;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseLogic("because class under test relies on logic classes")
class TexGroupTest {

    private MetaData metaData;

    @BeforeEach
    void setUp() throws Exception {
        metaData = new MetaData();
    }

    @Test
    void containsReturnsTrueForEntryInAux() throws Exception {
        Path auxFile = Path.of(TexGroupTest.class.getResource("paper.aux").toURI());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData);
        BibEntry inAux = new BibEntry();
        inAux.setCitationKey("Darwin1888");

        assertTrue(group.contains(inAux));
    }

    @Test
    void containsReturnsTrueForEntryNotInAux() throws Exception {
        Path auxFile = Path.of(TexGroupTest.class.getResource("paper.aux").toURI());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData);
        BibEntry notInAux = new BibEntry();
        notInAux.setCitationKey("NotInAux2017");

        assertFalse(group.contains(notInAux));
    }

    @Test
    void getFilePathReturnsRelativePath() throws Exception {
        Path auxFile = Path.of(TexGroupTest.class.getResource("paper.aux").toURI());
        String user = "Darwin";
        metaData.setLatexFileDirectory(user, auxFile.getParent());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, user);

        assertEquals("paper.aux", group.getFilePath().toString());
    }
}

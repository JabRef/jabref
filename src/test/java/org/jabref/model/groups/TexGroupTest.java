package org.jabref.model.groups;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TexGroupTest {

    private MetaData metaData;

    @BeforeEach
    public void setUp() throws Exception {
        metaData = new MetaData();
    }

    @Test
    public void containsReturnsTrueForEntryInAux() throws Exception {
        Path auxFile = Paths.get(TexGroupTest.class.getResource("paper.aux").toURI());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData);
        BibEntry inAux = new BibEntry();
        inAux.setCiteKey("Darwin1888");

        assertTrue(group.contains(inAux));
    }

    @Test
    public void containsReturnsTrueForEntryNotInAux() throws Exception {
        Path auxFile = Paths.get(TexGroupTest.class.getResource("paper.aux").toURI());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData);
        BibEntry notInAux = new BibEntry();
        notInAux.setCiteKey("NotInAux2017");

        assertFalse(group.contains(notInAux));
    }

    @Test
    public void ReturnsTrueIfPathRelative() throws Exception {
        Path auxFile = Paths.get(TexGroupTest.class.getResource("paper.aux").toURI());
        String user = System.getProperty("user.name") + '-' + InetAddress.getLocalHost().getHostName();
        metaData.setLaTexFileDirectory(user, auxFile.getParent().toString());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData);

        assertTrue("paper.aux".matches(group.getFilePath().toString()));
    }
}

package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.exporter.GroupSerializer;
import org.jabref.logic.importer.util.GroupsParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryStructureGroupTest {

    private final BibEntry rootEntry = new BibEntry().withCitationKey("root");
    private final BibEntry conferenceEntry = new BibEntry().withCitationKey("conference");
    private final BibEntry nestedEntry = new BibEntry().withCitationKey("nested");

    private final Function<BibEntry, Optional<Path>> lookup = entry -> Optional.ofNullable(Map.of(
            rootEntry, Path.of("root.yml"),
            conferenceEntry, Path.of("conference", "zygos.yml"),
            nestedEntry, Path.of("conference", "2020", "smith.yml")).get(entry));

    private final DirectoryStructureGroup group = new DirectoryStructureGroup("library", GroupHierarchyType.INDEPENDENT, lookup);

    @Test
    void rootLevelEntryCreatesNoSubgroup() {
        assertEquals(0, group.createSubgroups(rootEntry).size());
    }

    @Test
    void nestedEntryCreatesChainOfDirectoryGroups() {
        GroupTreeNode top = group.createSubgroups(nestedEntry).iterator().next();

        assertEquals("conference", top.getGroup().getName());
        assertEquals(1, top.getChildren().size());
        assertEquals("2020", top.getChildren().getFirst().getGroup().getName());
    }

    @Test
    void subgroupsOfAllEntriesMergeIntoOneTree() {
        ObservableList<BibEntry> entries = FXCollections.observableArrayList(rootEntry, conferenceEntry, nestedEntry);

        ObservableList<GroupTreeNode> subgroups = group.createSubgroups(entries);

        assertEquals(1, subgroups.size());
        GroupTreeNode conference = subgroups.getFirst();
        assertEquals("conference", conference.getGroup().getName());
        assertEquals(1, conference.getChildren().size());
    }

    @Test
    void directoryGroupContainsEntriesOfSubdirectoriesToo() {
        GroupTreeNode conference = group.createSubgroups(conferenceEntry).iterator().next();

        assertTrue(conference.getGroup().contains(conferenceEntry));
        assertTrue(conference.getGroup().contains(nestedEntry));
        assertFalse(conference.getGroup().contains(rootEntry));
    }

    @Test
    void serializedGroupParsesBackAsEmptyDirectoryStructureGroup() throws Exception {
        GroupTreeNode root = new GroupTreeNode(new AllEntriesGroup("All entries"));
        root.addSubgroup(group);

        List<String> serialized = new GroupSerializer().serializeTree(root);
        GroupTreeNode parsed = GroupsParser.importGroups(serialized, ',', new DummyFileUpdateMonitor(), new MetaData(), "user-host");

        AbstractGroup parsedGroup = parsed.getChildren().getFirst().getGroup();
        assertEquals(group, parsedGroup);
        assertEquals(0, ((DirectoryStructureGroup) parsedGroup).createSubgroups(nestedEntry).size());
    }
}

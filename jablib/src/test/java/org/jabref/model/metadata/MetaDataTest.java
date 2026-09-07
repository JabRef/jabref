package org.jabref.model.metadata;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.Version;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaDataTest {

    private MetaData metaData;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
    }

    @Test
    void emptyGroupsIfNotSet() {
        assertEquals(Optional.empty(), metaData.getGroups());
    }

    @Test
    void getLatexFileDirectoryReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), metaData.getLatexFileDirectory("user-host"));
    }

    @Test
    void storesAiLibraryId() {
        metaData.setAiLibraryId("test-ai-library-id");

        assertEquals(Optional.of("test-ai-library-id"), metaData.getAiLibraryId());
    }

    @Test
    void takingOverContentsCopiesTheSettings() {
        MetaData other = new MetaData();
        other.setMode(BibDatabaseMode.BIBLATEX);
        other.setEncoding(StandardCharsets.ISO_8859_1);
        other.setUserFileDirectory("user-host", "/tmp/files");
        other.markAsProtected();

        metaData.copyFrom(other);

        assertEquals(other, metaData);
    }

    /// [MetaData#equals] ignores these four, so the test above would pass without them.
    @Test
    void takingOverContentsCopiesWhatEqualsDoesNotCompare() {
        MetaData other = new MetaData();
        other.setGroupSearchSyntaxVersion(Version.parse("6.0"));
        other.setBlgFilePath("user-host", Path.of("/tmp/library.blg"));
        other.putUnknownMetaDataItem("unknown", List.of("value"));
        other.setContainsSearchGroups(true);

        metaData.copyFrom(other);

        assertEquals(Optional.of(Version.parse("6.0")), metaData.getGroupSearchSyntaxVersion());
        assertEquals(Optional.of(Path.of("/tmp/library.blg")), metaData.getBlgFilePath("user-host"));
        assertEquals(Map.of("unknown", List.of("value")), metaData.getUnknownMetaData());
        assertTrue(metaData.containsSearchGroups());
    }

    /// Group operations mutate nodes in place, so a shared tree would let a later edit rewrite what
    /// a recorded change is supposed to restore.
    @Test
    void takingOverContentsCopiesTheGroupTreeRatherThanSharingIt() {
        MetaData other = new MetaData();
        other.setGroups(GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ',')));

        metaData.copyFrom(other);
        metaData.getGroups().orElseThrow().addSubgroup(new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','));

        assertEquals(List.of(), other.getGroups().orElseThrow().getChildren(), "the source tree was edited too");
    }

    @Test
    void aCopyHoldsTheSameContentsAndItsOwnListeners() {
        metaData.setMode(BibDatabaseMode.BIBLATEX);
        List<MetaDataChangedEvent> events = new ArrayList<>();
        metaData.registerListener(new Object() {
            @Subscribe
            public void listen(MetaDataChangedEvent event) {
                events.add(event);
            }
        });

        MetaData copy = MetaData.copyOf(metaData);
        copy.setEncoding(StandardCharsets.ISO_8859_1);

        assertEquals(BibDatabaseMode.BIBLATEX, copy.getMode().orElseThrow());
        assertEquals(List.of(), events, "the copy notified the source's listeners");
    }

    @Test
    void takingOverContentsDropsSettingsTheOtherDoesNotHave() {
        metaData.setMode(BibDatabaseMode.BIBTEX);
        metaData.setUserFileDirectory("user-host", "/tmp/files");

        metaData.copyFrom(new MetaData());

        assertEquals(new MetaData(), metaData);
    }

    @Test
    void takingOverContentsKeepsListenersSubscribed() {
        List<MetaDataChangedEvent> events = new ArrayList<>();
        metaData.registerListener(new Object() {
            @Subscribe
            public void listen(MetaDataChangedEvent event) {
                events.add(event);
            }
        });

        MetaData other = new MetaData();
        other.setMode(BibDatabaseMode.BIBLATEX);
        metaData.copyFrom(other);
        assertEquals(1, events.size());

        metaData.setEncoding(StandardCharsets.ISO_8859_1);
        assertEquals(2, events.size());
    }
}

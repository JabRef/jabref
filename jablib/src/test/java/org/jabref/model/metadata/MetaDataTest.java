package org.jabref.model.metadata;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /// The group panel writes its tree back after every operation, usually handing back the node
    /// already installed. Subscribing again each time would make one later edit post an event per
    /// operation performed - and the panel rebuild itself from inside its own rebuild.
    @Test
    void installingTheSameGroupRootAgainDoesNotSubscribeTwice() {
        List<GroupUpdatedEvent> events = new ArrayList<>();
        metaData.registerListener(new Object() {
            @Subscribe
            public void listen(GroupUpdatedEvent event) {
                events.add(event);
            }
        });
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        metaData.setGroups(root);
        metaData.setGroups(root);
        metaData.setGroups(root);
        events.clear();

        root.addSubgroup(new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','));

        assertEquals(1, events.size(), "one edit posted an event per write-back");
    }

    /// Group operations mutate nodes in place, so a shared tree would let a later edit rewrite what
    /// a recorded change is supposed to restore.
    @Test
    void takingOverContentsCopiesTheGroupTreeRatherThanSharingIt() {
        MetaData other = new MetaData();
        other.setGroups(GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ',')));

        metaData.overwriteWith(other);
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

        metaData.overwriteWith(new MetaData());

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
        metaData.overwriteWith(other);
        // How many events one overwrite posts is not a contract - taking over groups posts its own
        // - so this asks only that the listener is still there to hear them.
        assertFalse(events.isEmpty(), "the overwrite was silent");

        int afterOverwrite = events.size();
        metaData.setEncoding(StandardCharsets.ISO_8859_1);
        assertTrue(events.size() > afterOverwrite, "a later edit no longer reaches the listener");
    }
}

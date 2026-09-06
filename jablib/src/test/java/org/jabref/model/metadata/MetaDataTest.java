package org.jabref.model.metadata;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        metaData.setContentsFrom(other);

        assertEquals(other, metaData);
    }

    @Test
    void takingOverContentsDropsSettingsTheOtherDoesNotHave() {
        metaData.setMode(BibDatabaseMode.BIBTEX);
        metaData.setUserFileDirectory("user-host", "/tmp/files");

        metaData.setContentsFrom(new MetaData());

        assertEquals(new MetaData(), metaData);
    }

    /// Listeners are registered on the instance, so taking over another's contents has to keep
    /// them subscribed - `LibraryTab` and `CoarseChangeFilter` depend on it for the modified
    /// marker, autosave and backups.
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
        metaData.setContentsFrom(other);
        assertEquals(1, events.size());

        metaData.setEncoding(StandardCharsets.ISO_8859_1);
        assertEquals(2, events.size());
    }
}

package org.jabref.model.metadata;

import java.nio.file.Path;
import java.util.Optional;

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
    void storesLibraryPath() {
        Path libraryPath = Path.of("library.bib");
        metaData.setLibraryPath(libraryPath);

        assertEquals(Optional.of(libraryPath), metaData.getLibraryPath());
    }

    @Test
    void storesAiLibraryId() {
        metaData.setAiLibraryId("test-ai-library-id");

        assertEquals(Optional.of("test-ai-library-id"), metaData.getAiLibraryId());
    }
}

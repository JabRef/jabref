package org.jabref.http.server.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.http.SrvStateManager;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// A directory library carries no `.bib` path, only a root directory. These tests pin that
/// jabsrv identifies it by that root — so the library listing, the existence-check query, and
/// the add endpoints can all address it.
/// [utest->req~directory-library.rest-api~1]
class ServerUtilsTest {

    @TempDir
    Path root;

    private BibDatabaseContext directoryLibrary() {
        BibDatabaseContext context = new BibDatabaseContext();
        context.convertToDirectoryLibrary(root);
        return context;
    }

    private String rootId() {
        return ServerUtils.libraryId(directoryLibrary()).orElseThrow();
    }

    private SrvStateManager stateManagerWith(BibDatabaseContext context) {
        SrvStateManager stateManager = mock(SrvStateManager.class);
        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList(context));
        return stateManager;
    }

    @Test
    void directoryLibraryIsListedByItsRootDerivedId() {
        assertEquals(List.of(rootId()), ServerUtils.openLibraryIds(stateManagerWith(directoryLibrary())));
    }

    @Test
    void directoryLibraryResolvesByIdToItsRoot() {
        assertEquals(root, ServerUtils.getLibraryPath(rootId(), stateManagerWith(directoryLibrary())));
    }

    @Test
    void directoryLibraryContextResolvesById() throws IOException {
        BibDatabaseContext context = directoryLibrary();
        assertSame(context, ServerUtils.getBibDatabaseContext(rootId(), stateManagerWith(context), mock(ImportFormatPreferences.class)));
    }

    @Test
    void directoryLibraryFileIsItsMirror() {
        assertEquals(root.resolve(root.getFileName() + ".bib"), ServerUtils.getLibraryFile(rootId(), stateManagerWith(directoryLibrary())));
    }

    @Test
    void unsavedLibraryHasNoId() {
        assertEquals(Optional.empty(), ServerUtils.libraryId(new BibDatabaseContext()));
    }

    @Test
    void unknownIdIsNotFound() {
        assertThrows(NotFoundException.class, () -> ServerUtils.getLibraryPath("does-not-exist", stateManagerWith(directoryLibrary())));
    }
}

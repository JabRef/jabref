package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.LibraryTab;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class OpenLibrarySideEffectTest {
    @Test
    void doesNotFindUserLibraryWithMatchingTitle() {
        LibraryTab userLibrary = mock(LibraryTab.class);
        when(userLibrary.getText()).thenReturn("Example Library (Chocolate.bib)");

        Optional<LibraryTab> foundLibrary = OpenLibrarySideEffect.findWalkthroughLibraryTab(
                List.of(userLibrary), null);

        assertEquals(Optional.empty(), foundLibrary);
    }

    @Test
    void findsCreatedWalkthroughLibraryAfterItsTitleChanges() {
        LibraryTab createdLibrary = mock(LibraryTab.class);
        when(createdLibrary.getText()).thenReturn("untitled");

        Optional<LibraryTab> foundLibrary = OpenLibrarySideEffect.findWalkthroughLibraryTab(
                List.of(createdLibrary), createdLibrary);

        assertEquals(Optional.of(createdLibrary), foundLibrary);
    }
}

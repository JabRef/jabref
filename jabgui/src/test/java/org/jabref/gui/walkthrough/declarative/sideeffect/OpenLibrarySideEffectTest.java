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
    void findsOnlyTheNamedWalkthroughLibrary() {
        LibraryTab untitledLibrary = mock(LibraryTab.class);
        when(untitledLibrary.getText()).thenReturn("Untitled library");
        LibraryTab walkthroughLibrary = mock(LibraryTab.class);
        when(walkthroughLibrary.getText()).thenReturn("Example Library (Chocolate.bib)");

        Optional<LibraryTab> foundLibrary = OpenLibrarySideEffect.findWalkthroughLibraryTab(
                List.of(untitledLibrary, walkthroughLibrary), "Chocolate.bib");

        assertEquals(Optional.of(walkthroughLibrary), foundLibrary);
    }
}

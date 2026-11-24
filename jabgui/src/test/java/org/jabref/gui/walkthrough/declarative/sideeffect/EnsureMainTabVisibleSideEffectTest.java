package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class EnsureMainTabVisibleSideEffectTest {

    @Test
    void showsAndRestoresHiddenMainTab() {
        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class);
        when(entryEditorPreferences.isTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS)).thenReturn(false);
        EnsureMainTabVisibleSideEffect sideEffect = new EnsureMainTabVisibleSideEffect(entryEditorPreferences);
        Walkthrough walkthrough = mock(Walkthrough.class);

        sideEffect.forward(walkthrough);
        sideEffect.backward(walkthrough);

        verify(entryEditorPreferences).setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
        verify(entryEditorPreferences).setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, false);
    }

    @Test
    void keepsVisibleMainTabVisibleAfterBackward() {
        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class);
        when(entryEditorPreferences.isTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS)).thenReturn(true);
        EnsureMainTabVisibleSideEffect sideEffect = new EnsureMainTabVisibleSideEffect(entryEditorPreferences);

        sideEffect.forward(mock(Walkthrough.class));
        sideEffect.backward(mock(Walkthrough.class));

        verify(entryEditorPreferences).setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
    }
}

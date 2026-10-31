package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/// Makes the Main entry-editor tab available while the PDF walkthrough is active.
@NullMarked
public class EnsureMainTabVisibleSideEffect implements WalkthroughSideEffect {
    private final EntryEditorPreferences entryEditorPreferences;
    private boolean mainTabWasVisible;

    public EnsureMainTabVisibleSideEffect(EntryEditorPreferences entryEditorPreferences) {
        this.entryEditorPreferences = entryEditorPreferences;
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        mainTabWasVisible = entryEditorPreferences.isTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS);
        if (!mainTabWasVisible) {
            entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
        }
        return true;
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.ALL_FIELDS, mainTabWasVisible);
        return true;
    }

    @Override
    public @NonNull String description() {
        return "Show Main tab for PDF walkthrough.";
    }
}

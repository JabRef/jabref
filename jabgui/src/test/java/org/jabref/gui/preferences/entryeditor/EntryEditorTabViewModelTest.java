package org.jabref.gui.preferences.entryeditor;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EntryEditorTabViewModelTest {

    private DialogService dialogService;
    private EntryEditorPreferences entryEditorPreferences;
    private EntryEditorTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        entryEditorPreferences = EntryEditorPreferences.getDefault();

        viewModel = new EntryEditorTabViewModel(
                dialogService,
                entryEditorPreferences,
                MrDlibPreferences.getDefault(),
                AbbreviationPreferences.getDefault(),
                mock(TaskExecutor.class));
    }

    @Test
    void classicTabsAreInsertedAfterMainAndNotDuplicated() {
        viewModel.setValues();

        viewModel.addClassicTabs();
        viewModel.addClassicTabs();

        List<String> names = viewModel.getTabs().stream().map(EditorTabViewModel::getDisplayName).toList();
        assertEquals(List.of(Localization.lang("Main"), Localization.lang("General"), Localization.lang("Abstract")), names.subList(0, 3));
        assertEquals(1, names.stream().filter(Localization.lang("General")::equals).count());
    }

    @Test
    void customTabRoundTripsThroughStoreSettings() {
        viewModel.setValues();

        EditorTabViewModel tab = viewModel.addCustomTab("General").orElseThrow();
        assertTrue(viewModel.addFieldPattern(tab, "keywords"));
        assertTrue(viewModel.addFieldPattern(tab, "comment-.*"));
        // duplicates within the same tab are rejected
        assertFalse(viewModel.addFieldPattern(tab, "Keywords"));

        viewModel.storeSettings();

        assertEquals(
                new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("keywords", "comment-.*")),
                entryEditorPreferences.getTabModels().getLast());
        // Preview stays in front even though the working copy excludes it
        assertTrue(entryEditorPreferences.getTabModels().getFirst().isPreview());
    }

    @Test
    void addCustomTabWithExistingNameReturnsExistingTab() {
        viewModel.setValues();
        EditorTabViewModel tab = viewModel.addCustomTab("General").orElseThrow();
        int tabCount = viewModel.getTabs().size();

        assertEquals(tab, viewModel.addCustomTab("general").orElseThrow());
        assertEquals(tabCount, viewModel.getTabs().size());
    }

    @Test
    void fieldPatternOnMultipleTabsIsFlaggedAsDuplicate() {
        viewModel.setValues();
        EditorTabViewModel first = viewModel.addCustomTab("First").orElseThrow();
        EditorTabViewModel second = viewModel.addCustomTab("Second").orElseThrow();
        viewModel.addFieldPattern(first, "keywords");
        viewModel.addFieldPattern(first, "doi");
        viewModel.addFieldPattern(second, "keywords");

        assertTrue(viewModel.isFieldPatternDuplicated("keywords"));
        assertFalse(viewModel.isFieldPatternDuplicated("doi"));
    }

    @Test
    void tabOrderIsStored() {
        viewModel.setValues();
        EditorTabViewModel first = viewModel.getTabs().removeFirst();
        viewModel.getTabs().add(first);

        viewModel.storeSettings();

        assertEquals(first.toModel(), entryEditorPreferences.getTabModels().getLast());
    }

    @Test
    void resetToDefaultsRemovesCustomTabs() {
        viewModel.setValues();
        viewModel.addCustomTab("General");

        viewModel.resetToDefaults();

        assertTrue(viewModel.getTabs().stream().noneMatch(EditorTabViewModel::isCustom));
    }

    @Test
    void mscPopupShowsWhenCheckboxTurnsOnButNotDuringInitialization() {
        viewModel.setValues();

        verifyNoInteractions(dialogService);

        when(dialogService.showConfirmationDialogAndWait(
                eq(Localization.lang("License agreement for MSC codes")),
                anyString(),
                eq(Localization.lang("Accept")),
                eq(Localization.lang("Decline"))))
                .thenReturn(false);

        viewModel.enableMscKeywordDescriptionsProperty().set(true);

        verify(dialogService).showConfirmationDialogAndWait(
                eq(Localization.lang("License agreement for MSC codes")),
                anyString(),
                eq(Localization.lang("Accept")),
                eq(Localization.lang("Decline")));
        assertFalse(viewModel.enableMscKeywordDescriptionsProperty().get());
    }
}

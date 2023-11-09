package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class SciteTabTest {

    public static final String SAMPLE_DOI = "10.1109/ICECS.2010.5724443";

    @Mock
    private PreferencesService preferencesService;
    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(entryEditorPreferences.shouldShowSciteTab()).thenReturn(true);
        when(preferencesService.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
    }

    @Test
    public void testName() {
        Assertions.assertEquals(SciteTab.NAME, "Scite");
    }

    @Test
    public void testShouldShow() {
        var tab = new SciteTab(preferencesService, taskExecutor, dialogService);
        boolean shouldShow = tab.shouldShow(null);
        Assertions.assertTrue(shouldShow);
    }

    @Test
    public void testBindNullEntry() {
        var tab = new SciteTab(preferencesService, taskExecutor, dialogService);
        tab.bindToEntry(null);
    }

    @Test
    public void testBindEntry() {
        var tab = new SciteTab(preferencesService, taskExecutor, dialogService);
        var entry = new BibEntry()
                .withField(StandardField.DOI, SAMPLE_DOI);

        tab.bindToEntry(entry);
    }
}

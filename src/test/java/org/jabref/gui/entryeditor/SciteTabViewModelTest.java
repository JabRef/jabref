package org.jabref.gui.entryeditor;

import java.util.Optional;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.core.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SciteTabViewModelTest {

    @Mock
    private GuiPreferences preferences;
    @Mock
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(entryEditorPreferences.shouldShowSciteTab()).thenReturn(true);
        when(preferences.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
    }

    @Test
    void sciteTallyDTO() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", 1);
        jsonObject.put("supporting", 2);
        jsonObject.put("contradicting", 3);
        jsonObject.put("mentioning", 4);
        jsonObject.put("unclassified", 5);
        jsonObject.put("citingPublications", 6);
        jsonObject.put("doi", "test_doi");

        var dto = SciteTallyModel.fromJSONObject(jsonObject);

        assertEquals(1, dto.total());
        assertEquals(2, dto.supporting());
        assertEquals(3, dto.contradicting());
        assertEquals(4, dto.mentioning());
        assertEquals(5, dto.unclassified());
        assertEquals(6, dto.citingPublications());
        assertEquals("test_doi", dto.doi());
    }

    @Test
    void fetchTallies() throws FetcherException {
        var viewModel = new SciteTabViewModel(preferences, taskExecutor);
        DOI doi = new DOI(SciteTabTest.SAMPLE_DOI);
        var actual = DOI.parse(viewModel.fetchTallies(doi).doi());
        assertEquals(Optional.of(doi), actual);
    }
}

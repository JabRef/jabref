package org.jabref.gui.entryeditor;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.preferences.PreferencesService;
import org.jabref.testutils.category.GUITest;

import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
public class SciteTabViewModelTest {

    @Mock
    private PreferencesService preferencesService;
    @Mock
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(entryEditorPreferences.shouldShowSciteTab()).thenReturn(true);
        when(preferencesService.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);
    }

    @Test
    public void testSciteTallyDTO() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("total", 1);
        jsonObject.put("supporting", 2);
        jsonObject.put("contradicting", 3);
        jsonObject.put("mentioning", 4);
        jsonObject.put("unclassified", 5);
        jsonObject.put("citingPublications", 6);
        jsonObject.put("doi", "test_doi");

        var dto = SciteTallyDTO.fromJSONObject(jsonObject);

        Assertions.assertEquals(1, dto.total());
        Assertions.assertEquals(2, dto.supporting());
        Assertions.assertEquals(3, dto.contradicting());
        Assertions.assertEquals(4, dto.mentioning());
        Assertions.assertEquals(5, dto.unclassified());
        Assertions.assertEquals(6, dto.citingPublications());
        Assertions.assertEquals("test_doi", dto.doi());
    }

    @Test
    void testFetchTallies() {
        var viewModel = new SciteTabViewModel(preferencesService, taskExecutor);
        DOI doi = new DOI(SciteTabTest.SAMPLE_DOI);
        var actual = viewModel.fetchTallies(doi);
        Assertions.assertTrue(doi.getDOI().equalsIgnoreCase(actual.doi()));
    }
}

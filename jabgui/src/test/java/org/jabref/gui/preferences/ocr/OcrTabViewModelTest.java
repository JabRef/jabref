package org.jabref.gui.preferences.ocr;

import java.util.List;

import javafx.beans.property.ListProperty;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ocr.OcrLanguage;
import org.jabref.logic.ocr.OcrPreferences;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OcrTabViewModelTest {

    private OcrPreferences ocrPreferences;
    private OcrTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        DialogService dialogService = mock(DialogService.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        ocrPreferences = OcrPreferences.getDefault();
        viewModel = new OcrTabViewModel(dialogService, filePreferences, ocrPreferences, taskExecutor);
    }

    @Test
    void setValuesLoadsEnglishAsDefault() {
        viewModel.setValues();

        assertEquals(List.of(OcrLanguage.ENGLISH), viewModel.selectedOcrLanguagesProperty().get());
    }

    @Test
    void setValuesLoadsLanguagesFromPreferences() {
        ocrPreferences.setOcrLanguages(List.of(OcrLanguage.GERMAN, OcrLanguage.FRENCH));
        viewModel.setValues();

        assertEquals(List.of(OcrLanguage.GERMAN, OcrLanguage.FRENCH), viewModel.selectedOcrLanguagesProperty().get());
    }

    @Test
    void storeSettingsPushesLanguagesToPreferences() {
        viewModel.setValues();
        viewModel.selectedOcrLanguagesProperty().setAll(OcrLanguage.SPANISH, OcrLanguage.JAPANESE);

        viewModel.storeSettings();

        assertEquals(List.of(OcrLanguage.SPANISH, OcrLanguage.JAPANESE), ocrPreferences.getOcrLanguages());
    }

    @Test
    void suggestionsContainValuesAndFilterCorrectly() {
        List<OcrLanguage> allSuggestions = viewModel.getOcrLanguageSuggestions("");
        assertTrue(allSuggestions.contains(OcrLanguage.ENGLISH));
        assertTrue(allSuggestions.contains(OcrLanguage.GERMAN));

        List<OcrLanguage> filtered = viewModel.getOcrLanguageSuggestions("eng");
        assertTrue(filtered.contains(OcrLanguage.ENGLISH));
        assertTrue(!filtered.contains(OcrLanguage.GERMAN));
    }
}

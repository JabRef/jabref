package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrMyPdfEngineTest {

    private OcrPreferences ocrPreferences;
    private OcrMyPdfEngine engine;
    private Path inputPath;
    private Path outputPath;

    @BeforeEach
    void setUp() {
        ocrPreferences = OcrPreferences.getDefault();
        engine = new OcrMyPdfEngine(ocrPreferences);
        inputPath = Path.of("input.pdf");
        outputPath = Path.of("output.pdf");
    }

    @Test
    void defaultCommandConstructsWithEnglish() {
        List<String> command = engine.buildCommand(inputPath, outputPath);

        assertEquals(List.of("ocrmypdf", "--skip-text", "--language", "eng", "input.pdf", "output.pdf"), command);
    }

    @Test
    void multipleLanguagesAreJoinedWithPlusSign() {
        ocrPreferences.setOcrLanguages(List.of(OcrLanguage.ENGLISH, OcrLanguage.GERMAN, OcrLanguage.FRENCH));

        List<String> command = engine.buildCommand(inputPath, outputPath);

        assertEquals(List.of("ocrmypdf", "--skip-text", "--language", "eng+deu+fra", "input.pdf", "output.pdf"), command);
    }

    @Test
    void orderingOfLanguagesIsPreserved() {
        ocrPreferences.setOcrLanguages(List.of(OcrLanguage.SPANISH, OcrLanguage.ENGLISH, OcrLanguage.JAPANESE));

        List<String> command = engine.buildCommand(inputPath, outputPath);

        assertEquals(List.of("ocrmypdf", "--skip-text", "--language", "spa+eng+jpn", "input.pdf", "output.pdf"), command);
    }

    @Test
    void fallbackToEnglishIfLanguageListIsEmpty() {
        ocrPreferences.setOcrLanguages(List.of());

        List<String> command = engine.buildCommand(inputPath, outputPath);

        assertEquals(List.of("ocrmypdf", "--skip-text", "--language", "eng", "input.pdf", "output.pdf"), command);
    }
}

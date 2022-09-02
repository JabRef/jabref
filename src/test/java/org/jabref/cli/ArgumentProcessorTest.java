package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.cli.ArgumentProcessor.Mode;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentProcessorTest {

    private ArgumentProcessor processor;
    private final PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    private final SavePreferences savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);

    @BeforeEach()
    void setup() {
        when(savePreferences.getSaveOrder()).thenReturn(SaveOrderConfig.getDefaultSaveOrder());
        when(preferencesService.getSavePreferences()).thenReturn(savePreferences);
    }

    @Test
    void testAuxImport(@TempDir Path tempDir) throws Exception {

        String auxFile = Path.of(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toAbsolutePath().toString();
        String originBib = Path.of(AuxCommandLineTest.class.getResource("origin.bib").toURI()).toAbsolutePath().toString();

        Path outputBib = tempDir.resolve("output.bisb").toAbsolutePath();
        String outputBibFile = outputBib.toAbsolutePath().toString();

        List<String> args = List.of("--nogui", "--debug", "--aux", auxFile + "," + outputBibFile, originBib);

        processor = new ArgumentProcessor(args.toArray(String[]::new), Mode.INITIAL_START, preferencesService);

        assertTrue(Files.exists(outputBib));
    }
}

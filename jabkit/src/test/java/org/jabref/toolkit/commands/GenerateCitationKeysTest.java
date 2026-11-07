package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~jabkit.cli.citation-key-avoid-overwrite~1]
@NullMarked
class GenerateCitationKeysTest extends AbstractJabKitTest {

    private static final String KEYED_ENTRY = """
            @Article{MyStableKey,
              author = {Smith, Alice},
              title = {Example study},
              journal = {Example Journal},
              year = {2024},
            }
            """;

    private static final String KEYLESS_ENTRY = """
            @Article{,
              author = {Smith, Alice},
              title = {Example study},
              journal = {Example Journal},
              year = {2024},
            }
            """;

    @Test
    void avoidOverwriteOptionPreservesExistingCitationKey(@TempDir Path tempDir) throws IOException, ParseException {
        setStoredAvoidOverwritePreference(false);

        assertEquals("MyStableKey", generateCitationKey(tempDir, KEYED_ENTRY, "--avoid-overwrite"));
    }

    @Test
    void avoidOverwriteOptionGeneratesKeyWhenCitationKeyIsMissing(@TempDir Path tempDir) throws IOException, ParseException {
        setStoredAvoidOverwritePreference(false);

        assertEquals("Smith2024", generateCitationKey(tempDir, KEYLESS_ENTRY, "--avoid-overwrite"));
    }

    @Test
    void existingCitationKeyIsRegeneratedWhenAvoidOverwriteIsDisabled(@TempDir Path tempDir) throws IOException, ParseException {
        setStoredAvoidOverwritePreference(false);

        assertEquals("Smith2024", generateCitationKey(tempDir, KEYED_ENTRY));
    }

    @Test
    void storedAvoidOverwritePreferenceIsUsedWhenOptionIsOmitted(@TempDir Path tempDir) throws IOException, ParseException {
        setStoredAvoidOverwritePreference(true);

        assertEquals("MyStableKey", generateCitationKey(tempDir, KEYED_ENTRY));
    }

    private void setStoredAvoidOverwritePreference(boolean avoidOverwrite) {
        CitationKeyPatternPreferences citationKeyPatternPreferences = preferences.getCitationKeyPatternPreferences();
        citationKeyPatternPreferences.setAvoidOverwriteCiteKey(avoidOverwrite);
    }

    private String generateCitationKey(Path tempDir, String bibtex, String... options) throws IOException, ParseException {
        Path inputFile = tempDir.resolve("input.bib");
        Path outputFile = tempDir.resolve("output.bib");
        Files.writeString(inputFile, bibtex);

        List<String> arguments = new ArrayList<>(List.of(
                "citationkeys", "generate", inputFile.toString(),
                "--porcelain", "--pattern=[auth][year]", "--output=" + outputFile));
        arguments.addAll(List.of(options));
        assertEquals(0, commandLine.execute(arguments.toArray(String[]::new)));

        return BibtexParser.singleFromString(Files.readString(outputFile), importFormatPreferences)
                           .orElseThrow()
                           .getCitationKey()
                           .orElseThrow();
    }
}

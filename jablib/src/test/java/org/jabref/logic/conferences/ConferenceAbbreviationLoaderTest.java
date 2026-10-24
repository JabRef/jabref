/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.conferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class ConferenceAbbreviationLoaderTest {
    private static final String CONFERENCE_NAME = "International Conference on Business Process Management";

    @Test
    void loadsExternalConferenceCsv(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, CONFERENCE_NAME + ",BPM");

        ConferenceAbbreviationRepository repository = ConferenceAbbreviationLoader.loadRepository(
                new ConferenceAbbreviationPreferences(List.of(csv.toString())));

        assertEquals(Optional.of("BPM"), repository.getDefaultAbbreviation(CONFERENCE_NAME));
        assertEquals(Optional.of(new Abbreviation(CONFERENCE_NAME, "BPM")), repository.get("BPM"));
    }

    @Test
    void usesSharedCsvParser(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, CONFERENCE_NAME + ",BPM,BPM");

        assertEquals(List.of(new Abbreviation(CONFERENCE_NAME, "BPM", "BPM")),
                List.copyOf(AbbreviationLoader.readAbbreviationsFromCsvFile(csv)));
    }

    @Test
    void loadingDoesNotReverseConfiguredLists(@TempDir Path directory) throws IOException {
        Path first = directory.resolve("first.csv");
        Path second = directory.resolve("second.csv");
        Files.writeString(first, CONFERENCE_NAME + ",BPM");
        Files.writeString(second, "International Conference on Industrial Informatics,INDIN");
        List<String> paths = List.of(first.toString(), second.toString());
        ConferenceAbbreviationPreferences preferences = new ConferenceAbbreviationPreferences(paths);

        ConferenceAbbreviationRepository repository = ConferenceAbbreviationLoader.loadRepository(preferences);
        ConferenceAbbreviationRepository reloaded = ConferenceAbbreviationLoader.loadRepository(preferences);

        assertEquals(paths, preferences.getExternalLists());
        assertEquals(List.copyOf(repository.getCustomAbbreviations()), List.copyOf(reloaded.getCustomAbbreviations()));
        assertEquals(2, repository.getCustomAbbreviations().size());
    }

    @Test
    void unreadableExternalListDoesNotPreventLoadingOtherLists(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, CONFERENCE_NAME + ",BPM");

        ConferenceAbbreviationRepository repository = ConferenceAbbreviationLoader.loadRepository(
                new ConferenceAbbreviationPreferences(List.of(csv.toString(), directory.resolve("missing.csv").toString())));

        assertEquals(Optional.of("BPM"), repository.getDefaultAbbreviation(CONFERENCE_NAME));
    }

    @Test
    void conferenceListsAreNotAddedToJournalRepository(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, "Test Conference for Loader Isolation,TCLI");
        ConferenceAbbreviationLoader.loadRepository(new ConferenceAbbreviationPreferences(List.of(csv.toString())));

        assertEquals(Optional.empty(), JournalAbbreviationLoader.loadRepository(JournalAbbreviationPreferences.getDefault())
                .get("Test Conference for Loader Isolation"));
    }
}

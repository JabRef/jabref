/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.conferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class ConferenceAbbreviationRepositoryTest {
    private static final Abbreviation BPM = new Abbreviation("International Conference on Business Process Management", "BPM");

    @Test
    void emptyConferenceRepositoryDoesNotContainJournalDemonstrationData() {
        ConferenceAbbreviationRepository repository = new ConferenceAbbreviationRepository();

        assertEquals(List.of(), List.copyOf(repository.getAllLoaded()));
        assertEquals(Optional.empty(), repository.get("Demonstration"));
    }

    @Test
    void customConferenceCanBeLookedUpByFullNameAndAbbreviation() {
        ConferenceAbbreviationRepository repository = new ConferenceAbbreviationRepository();
        repository.addCustomAbbreviation(BPM);

        assertEquals(Optional.of(BPM), repository.get(BPM.getName()));
        assertEquals(Optional.of(BPM), repository.get("BPM"));
        assertEquals(Optional.of("BPM"), repository.getDefaultAbbreviation(BPM.getName()));
    }

    @Test
    void loadsConferenceFileWithoutLtwaData(@TempDir Path directory) {
        Path file = directory.resolve("conferences.mv");
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).open()) {
            MVMap<String, Abbreviation> abbreviations = store.openMap("FullToAbbreviation");
            abbreviations.put(BPM.getName(), BPM);
        }

        ConferenceAbbreviationRepository repository = new ConferenceAbbreviationRepository(file);

        assertEquals(Optional.of(BPM), repository.get("BPM"));
        assertEquals(List.of(BPM), List.copyOf(repository.getAllLoaded()));
    }

    @Test
    void journalAndConferenceRecordsAreIndependent() {
        JournalAbbreviationRepository journals = new JournalAbbreviationRepository();
        ConferenceAbbreviationRepository conferences = new ConferenceAbbreviationRepository();
        journals.addCustomAbbreviation(new Abbreviation("Shared Name", "Journal"));
        conferences.addCustomAbbreviation(new Abbreviation("Shared Name", "Conference"));

        assertEquals(Optional.of("Journal"), journals.getDefaultAbbreviation("Shared Name"));
        assertEquals(Optional.of("Conference"), conferences.getDefaultAbbreviation("Shared Name"));
    }

    @Test
    void loadedAndCustomRecordsSupportAllAbbreviationForms(@TempDir Path directory) {
        Abbreviation builtIn = new Abbreviation("Conference on Example Systems", "Conf. Ex. Syst.", "CES");
        Path file = directory.resolve("conferences.mv");
        try (MVStore store = new MVStore.Builder().fileName(file.toString()).open()) {
            MVMap<String, Abbreviation> abbreviations = store.openMap("FullToAbbreviation");
            abbreviations.put(builtIn.getName(), builtIn);
        }
        ConferenceAbbreviationRepository repository = new ConferenceAbbreviationRepository(file);

        assertEquals(Optional.of(builtIn), repository.get("Conf Ex Syst"));
        assertEquals(Optional.of(builtIn), repository.get("CES"));
        repository.addCustomAbbreviation(new Abbreviation(builtIn.getName(), "Custom"));
        assertEquals(Optional.of("Custom"), repository.getDefaultAbbreviation(builtIn.getName()));
    }
}

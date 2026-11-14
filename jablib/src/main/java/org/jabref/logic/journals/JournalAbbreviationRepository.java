package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.journals.ltwa.LtwaRepository;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class JournalAbbreviationRepository extends AbbreviationRepository {
    private final LtwaRepository ltwaRepository;

    public JournalAbbreviationRepository(Path journalList, LtwaRepository ltwaRepository) {
        super(journalList);
        this.ltwaRepository = ltwaRepository;
    }

    public JournalAbbreviationRepository() {
        super(new Abbreviation("Demonstration", "Demo", "Dem"));
        ltwaRepository = new LtwaRepository();
    }

    public Optional<String> getLtwaAbbreviation(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return Optional.of(journalName);
        }
        return ltwaRepository.abbreviate(journalName);
    }
}

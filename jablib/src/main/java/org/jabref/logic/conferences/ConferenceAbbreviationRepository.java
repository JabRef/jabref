package org.jabref.logic.conferences;

import java.nio.file.Path;

import org.jabref.logic.journals.AbbreviationRepository;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConferenceAbbreviationRepository extends AbbreviationRepository {
    public ConferenceAbbreviationRepository() {
    }

    public ConferenceAbbreviationRepository(Path conferenceList) {
        super(conferenceList);
    }
}

package org.jabref.logic.layout.format;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatter;

import org.jspecify.annotations.NonNull;

/**
 * JournalAbbreviator formats the given text in an abbreviated form according to the journal abbreviation lists.
 * <p>
 * The given input text is abbreviated according to the journal abbreviation lists. If no abbreviation for input is
 * found (e.g. not in list or already abbreviated), the input will be returned unmodified.
 * <p>
 * Usage: \format[JournalAbbreviator]{\journal} Example result: "Phys. Rev. Lett." instead of "Physical Review Letters"
 */
public class JournalAbbreviator implements LayoutFormatter {

    private final JournalAbbreviationRepository repository;

    public JournalAbbreviator(@NonNull JournalAbbreviationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String format(String fieldText) {
        return repository.getDefaultAbbreviation(fieldText).orElse(fieldText);
    }
}

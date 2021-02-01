package org.jabref.logic.layout.format;

import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatter;

/**
 * JournalAbbreviator formats the given text in an abbreviated form according to the journal abbreviation lists.
 *
 * The given input text is abbreviated according to the journal abbreviation lists. If no abbreviation for input is
 * found (e.g. not in list or already abbreviated), the input will be returned unmodified.
 *
 * Usage: \format[JournalAbbreviator]{\journal} Example result: "Phys. Rev. Lett." instead of "Physical Review Letters"
 */
public class JournalAbbreviator implements LayoutFormatter {

    private final JournalAbbreviationRepository repository;

    public JournalAbbreviator(JournalAbbreviationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public String format(String fieldText) {
        return repository.getDefaultAbbreviation(fieldText).orElse(fieldText);
    }
}

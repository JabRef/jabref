package org.jabref.logic.layout.format;

import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.LayoutFormatter;

/**
 * JournalAbbreviator formats the given text in an abbreviated form
 * according to the journal abbreviation lists.
 *
 * The given input text is abbreviated according to the journal abbreviation lists.
 * If no abbreviation for input is found (e.g. not in list or already abbreviated), the input will be returned unmodified.
 *
 * Usage:
 *    \format[JournalAbbreviator]{\journal}
 * Example result:
 *    "Phys. Rev. Lett." instead of "Physical Review Letters"
 *
 * @author  Meigel
 *
 */
public class JournalAbbreviator implements LayoutFormatter {

    private final JournalAbbreviationLoader repostioryLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;


    public JournalAbbreviator(JournalAbbreviationLoader repostioryLoader,
            JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.repostioryLoader = Objects.requireNonNull(repostioryLoader);
        this.journalAbbreviationPreferences = Objects.requireNonNull(journalAbbreviationPreferences);
    }

    @Override
    public String format(String fieldText) {
        return repostioryLoader.getRepository(journalAbbreviationPreferences)
                .getIsoAbbreviation(fieldText).orElse(fieldText);
    }
}

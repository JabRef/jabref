package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class AbbreviationChecker implements ValueChecker {

    private final JournalAbbreviationRepository abbreviationRepository;

    public AbbreviationChecker(JournalAbbreviationRepository abbreviationRepository) {
        this.abbreviationRepository = abbreviationRepository;
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (abbreviationRepository.isAbbreviatedName(value)) {
            return Optional.of(Localization.lang("abbreviation detected"));
        }

        return Optional.empty();
    }
}

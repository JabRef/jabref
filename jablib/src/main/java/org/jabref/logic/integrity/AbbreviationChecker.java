package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class AbbreviationChecker implements EntryChecker {

    private final ConferenceAbbreviationRepository abbreviationRepository;
    private final Set<Field> fields = FieldFactory.getBookNameFields();

    public AbbreviationChecker(ConferenceAbbreviationRepository abbreviationRepository) {
        this.abbreviationRepository = abbreviationRepository;
    }

    // [impl->req~abbreviations.conference-integrity~1]
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> messages = new ArrayList<>();
        for (Field field : fields) {
            Optional<String> value = entry.getFieldLatexFree(field);
            value.filter(abbreviationRepository::isAbbreviatedName)
                 .ifPresent(_ -> messages.add(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, field)));
        }
        return messages;
    }
}

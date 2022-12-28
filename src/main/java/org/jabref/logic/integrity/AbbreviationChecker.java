package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class AbbreviationChecker implements EntryChecker {

    private final JournalAbbreviationRepository abbreviationRepository;
    private final Set<Field> fields = FieldFactory.getBookNameFields();

    public AbbreviationChecker(JournalAbbreviationRepository abbreviationRepository) {
        this.abbreviationRepository = abbreviationRepository;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> messages = new ArrayList<>();
        for (Field field : fields) {
            Optional<String> value = entry.getLatexFreeField(field);
            value.filter(abbreviationRepository::isAbbreviatedName)
                 .ifPresent(val -> messages.add(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, field)));
        }
        return messages;
    }
}

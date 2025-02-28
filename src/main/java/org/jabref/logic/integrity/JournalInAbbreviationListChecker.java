package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class JournalInAbbreviationListChecker implements EntryChecker {

    private final Field field;
    private final JournalAbbreviationRepository abbreviationRepository;

    public JournalInAbbreviationListChecker(Field field, JournalAbbreviationRepository abbreviationRepository) {
        this.field = Objects.requireNonNull(field);
        this.abbreviationRepository = Objects.requireNonNull(abbreviationRepository);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getFieldOrAliasLatexFree(field);
        if (value.isEmpty()) {
            return Collections.emptyList();
        }

        final String journal = value.get();
        if (!abbreviationRepository.isKnownName(journal)) {
            return Collections.singletonList(new IntegrityMessage(IntegrityIssue.JOURNAL_NOT_FOUND_IN_ABBREVIATION_LIST.getText(), entry, field));
        }

        return Collections.emptyList();
    }
}

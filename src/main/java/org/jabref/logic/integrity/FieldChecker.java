package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.OptionalUtil;

public class FieldChecker implements IntegrityCheck.Checker {
    protected final String field;
    private final ValueChecker checker;

    public FieldChecker(String field, ValueChecker checker) {
        this.field = field;
        this.checker = Objects.requireNonNull(checker);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        return OptionalUtil.toList(checker.checkValue(value.get()).map(message -> new IntegrityMessage(message, entry, field)));
    }
}

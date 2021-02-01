package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.OptionalUtil;

public class FieldChecker implements EntryChecker {
    protected final Field field;
    private final ValueChecker checker;

    public FieldChecker(Field field, ValueChecker checker) {
        this.field = field;
        this.checker = Objects.requireNonNull(checker);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (value.isEmpty()) {
            return Collections.emptyList();
        }

        return OptionalUtil.toList(checker.checkValue(value.get()).map(message -> new IntegrityMessage(message, entry, field)));
    }
}

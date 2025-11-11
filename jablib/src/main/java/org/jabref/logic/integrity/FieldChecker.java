package org.jabref.logic.integrity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.OptionalUtil;

/**
 * This is a "proxy" to use a {@link ValueChecker} as {@link EntryChecker}.
 * The "proxy" is configured using the field to handle and the value checker to apply.
 */
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
        return value.map(s -> OptionalUtil.toList(checker.checkValue(s).map(message -> new IntegrityMessage(message, entry, field)))).orElseGet(List::of);
    }
}

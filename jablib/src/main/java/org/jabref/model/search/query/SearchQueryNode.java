package org.jabref.model.search.query;

import java.util.Optional;

import org.jabref.model.entry.field.Field;

public record SearchQueryNode(Optional<Field> field, String term) {
}

package org.jabref.model.undo;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Changes a library's optional keyword separator.
@NullMarked
public record UndoableKeywordSeparatorChange(MetaData metaData, Optional<Character> before, Optional<Character> after) implements BibChange {

    @Override
    public UndoableKeywordSeparatorChange inverted() {
        return new UndoableKeywordSeparatorChange(metaData, after, before);
    }

    @Override
    public ApplyResult apply() {
        if (!Objects.equals(metaData.getKeywordSeparator(), before)) {
            return ApplyResult.of(this, "the keyword separator is %s, not the recorded %s".formatted(metaData.getKeywordSeparator(), before));
        }
        after.ifPresentOrElse(metaData::setKeywordSeparator, metaData::clearKeywordSeparator);
        return ApplyResult.SUCCESS;
    }
}

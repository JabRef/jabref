package org.jabref.model.undo;

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
    public void apply() {
        after.ifPresentOrElse(metaData::setKeywordSeparator, metaData::clearKeywordSeparator);
    }
}

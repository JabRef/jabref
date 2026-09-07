package org.jabref.logic.cleanup;

import org.jabref.logic.formatter.Formatter;

import org.jspecify.annotations.NullMarked;

/// A formatter whose result depends on the keyword separator of the library it runs on.
///
/// Formatters are stateless singletons looked up by key, so a library-specific separator cannot be stored in them.
/// Callers that know the library (save, cleanup dialog, load) obtain a bound copy via [#withKeywordSeparator(Character)];
/// an unbound instance falls back to the global preference.
@NullMarked
public interface KeywordSeparatorAware {

    Formatter withKeywordSeparator(Character keywordSeparator);
}

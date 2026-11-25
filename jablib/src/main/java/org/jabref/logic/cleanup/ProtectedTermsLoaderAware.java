package org.jabref.logic.cleanup;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

import org.jspecify.annotations.NullMarked;

/// A formatter whose result depends on the active [ProtectedTermsLoader].
///
/// Formatters are stateless singletons looked up by key, so callers that run cleanups or save actions bind a copy
/// to the active [ProtectedTermsLoader] via [#withProtectedTermsLoader(ProtectedTermsLoader)]; an unbound instance
/// falls back to the built-in protected terms lists.
@NullMarked
public interface ProtectedTermsLoaderAware {

    Formatter withProtectedTermsLoader(ProtectedTermsLoader protectedTermsLoader);
}

package org.jabref.http.server.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Builds a Search.g4 expression for the `POST /libraries/query` endpoint.
///
/// The endpoint accepts lists of DOIs and URLs and matches any entry whose `doi` or `url`
/// field exactly equals one of the inputs. This helper turns those lists into an
/// expression like:
///
///   doi == "10.1000/xyz123" OR doi == "10.1234/abc" OR url == "https://example.com/paper"
///
/// using the case-insensitive exact-match operator (`==`).
@NullMarked
public final class LibraryQueryExpressionBuilder {

    private LibraryQueryExpressionBuilder() {
    }

    /// @return the expression, or [Optional#empty] if both lists are empty.
    public static Optional<String> build(List<String> dois, List<String> urls) {
        List<String> clauses = new ArrayList<>();
        String doiField = StandardField.DOI.getName();
        String urlField = StandardField.URL.getName();

        Stream.concat(
                dois.stream().map(doi -> clause(doiField, doi)),
                urls.stream().map(url -> clause(urlField, url))
        ).forEach(clauses::add);

        if (clauses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(" OR ", clauses));
    }

    private static String clause(String fieldName, String value) {
        return fieldName + " == \"" + escapeStringLiteral(value) + "\"";
    }

    /// Escapes a value for use inside a Search.g4 `STRING_LITERAL`.
    /// Grammar: `STRING_LITERAL: '"' ('\\"' | ~["])* '"';` — only `"` needs escaping.
    private static String escapeStringLiteral(String value) {
        return value.replace("\"", "\\\"");
    }
}

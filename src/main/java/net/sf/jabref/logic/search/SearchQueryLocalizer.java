package net.sf.jabref.logic.search;

import net.sf.jabref.logic.l10n.Localization;

import java.util.Objects;

public class SearchQueryLocalizer {

    public static String localize(SearchQuery searchQuery) {
        Objects.requireNonNull(searchQuery);

        return String.format("\"%s\" (%s, %s)",
                searchQuery.getQuery(),
                getLocalizedCaseSensitiveDescription(searchQuery),
                getLocalizedRegularExpressionDescription(searchQuery));
    }

    private static String getLocalizedCaseSensitiveDescription(SearchQuery searchQuery) {
        if (searchQuery.isCaseSensitive()) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }

    private static String getLocalizedRegularExpressionDescription(SearchQuery searchQuery) {
        if (searchQuery.isRegularExpression()) {
            return Localization.lang("regular expression");
        } else {
            return Localization.lang("plain text");
        }
    }

}

package net.sf.jabref.gui.search;

import net.sf.jabref.logic.l10n.Localization;

public class SearchQuery {

    public final String query;
    public final boolean caseSensitive;
    public final boolean regularExpression;

    SearchQuery(String query, boolean caseSensitive, boolean regularExpression) {
        this.query = query;
        this.caseSensitive = caseSensitive;
        this.regularExpression = regularExpression;
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s, %s)", query, getCaseSensitiveDescription(), getRegularExpressionDescription());
    }

    private String getCaseSensitiveDescription() {
        if (caseSensitive) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }

    private String getRegularExpressionDescription() {
        if (regularExpression) {
            return Localization.lang("regular expression");
        } else {
            return Localization.lang("plain text");
        }
    }

}

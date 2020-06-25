package org.jabref.preferences;

import org.jabref.gui.search.SearchDisplayMode;

public class SearchPreferences {

    private final SearchDisplayMode searchDisplayMode;
    private final boolean isCaseSensitive;
    private final boolean isRegularExpression;

    public SearchPreferences(SearchDisplayMode searchDisplayMode, boolean isCaseSensitive, boolean isRegularExpression) {
        this.searchDisplayMode = searchDisplayMode;
        this.isCaseSensitive = isCaseSensitive;
        this.isRegularExpression = isRegularExpression;
    }

    public SearchDisplayMode getSearchDisplayMode() {
        return searchDisplayMode;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public boolean isRegularExpression() {
        return isRegularExpression;
    }

    public Builder getBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private SearchDisplayMode searchDisplayMode;
        private boolean isCaseSensitive;
        private boolean isRegularExpression;

        public Builder(SearchPreferences initialPreferences) {
            this.searchDisplayMode = initialPreferences.searchDisplayMode;
            this.isCaseSensitive = initialPreferences.isCaseSensitive;
            this.isRegularExpression = initialPreferences.isRegularExpression;
        }

        public Builder withSearchDisplayMode(SearchDisplayMode searchDisplayMode) {
            this.searchDisplayMode = searchDisplayMode;
            return this;
        }

        public Builder withCaseSensitive(boolean isCaseSensitive) {
            this.isCaseSensitive = isCaseSensitive;
            return this;
        }

        public Builder withRegularExpression(boolean regularExpression) {
            this.isRegularExpression = regularExpression;
            return this;
        }

        public SearchPreferences build() {
            return new SearchPreferences(searchDisplayMode, isCaseSensitive, isRegularExpression);
        }
    }
}

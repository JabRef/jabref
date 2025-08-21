package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import org.jspecify.annotations.NonNull;

/// Ensures the search bar starts with unfiltered mode and regex disabled, then restores
/// previous settings on exit.
public class EnsureSearchSettingsSideEffect implements WalkthroughSideEffect {
    private final boolean previousRegex;
    private final boolean previousCaseSensitive;
    private final SearchDisplayMode previousDisplayMode;
    private final SearchPreferences searchPreferences;

    public EnsureSearchSettingsSideEffect(SearchPreferences searchPreferences) {
        this.searchPreferences = searchPreferences;
        this.previousRegex = searchPreferences.isRegularExpression();
        this.previousCaseSensitive = searchPreferences.isCaseSensitive();
        this.previousDisplayMode = searchPreferences.getSearchDisplayMode();
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        searchPreferences.setSearchFlag(SearchFlags.REGULAR_EXPRESSION, false);
        searchPreferences.setSearchFlag(SearchFlags.CASE_SENSITIVE, false);
        searchPreferences.setSearchDisplayMode(SearchDisplayMode.FLOAT);
        return true;
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        searchPreferences.setSearchFlag(SearchFlags.REGULAR_EXPRESSION, previousRegex);
        searchPreferences.setSearchFlag(SearchFlags.CASE_SENSITIVE, previousCaseSensitive);
        searchPreferences.setSearchDisplayMode(previousDisplayMode);
        return true;
    }

    @Override
    public @NonNull String description() {
        return Localization.lang("Prepare search settings");
    }
}

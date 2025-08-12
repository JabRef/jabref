package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

/// Ensures the search bar starts with unfiltered mode and regex disabled, then restores
/// previous settings on exit.
public class EnsureSearchSettingsSideEffect implements WalkthroughSideEffect {
    private final boolean previousRegex;
    private final boolean previousCaseSensitive;
    private final SearchDisplayMode previousDisplayMode;

    public EnsureSearchSettingsSideEffect() {
        var preferences = Injector.instantiateModelOrService(GuiPreferences.class).getSearchPreferences();
        this.previousRegex = preferences.isRegularExpression();
        this.previousCaseSensitive = preferences.isCaseSensitive();
        this.previousDisplayMode = preferences.getSearchDisplayMode();
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        var preferences = Injector.instantiateModelOrService(GuiPreferences.class).getSearchPreferences();
        preferences.setSearchFlag(SearchFlags.REGULAR_EXPRESSION, false);
        preferences.setSearchFlag(SearchFlags.CASE_SENSITIVE, false);
        preferences.setSearchDisplayMode(SearchDisplayMode.FLOAT);
        return true;
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        var preferences = Injector.instantiateModelOrService(GuiPreferences.class).getSearchPreferences();
        preferences.setSearchFlag(SearchFlags.REGULAR_EXPRESSION, previousRegex);
        preferences.setSearchFlag(SearchFlags.CASE_SENSITIVE, previousCaseSensitive);
        preferences.setSearchDisplayMode(previousDisplayMode);
        return true;
    }

    @Override
    public @NonNull String description() {
        return Localization.lang("Prepare search settings");
    }
}



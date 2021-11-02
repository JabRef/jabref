package org.jabref.gui.search.rules.describer;

import java.util.EnumSet;

import javafx.scene.text.TextFlow;

import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class LuceneBasedSearchRuleDescriber implements SearchDescriber {

    private final EnumSet<SearchFlags> searchFlags;
    private final String query;

    public LuceneBasedSearchRuleDescriber(EnumSet<SearchFlags> searchFlags, String query) {
        this.searchFlags = searchFlags;
        this.query = query;
    }

    @Override
    public TextFlow getDescription() {
        TextFlow searchDescription = new TextFlow();
        return searchDescription;
    }
}

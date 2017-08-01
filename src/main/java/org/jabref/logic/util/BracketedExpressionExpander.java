package org.jabref.logic.util;
import org.jabref.model.entry.BibEntry;
/**
 * @author saulius
 * The BracketedExpressionExpander provides methods to expand bracketed expressions,
 * such as [year]_[author]_[firstpage], using information from a provided BibEntry.
 * The above-mentioned expression would yield 2017_Kizune_123 when expanded using the
 * BibTeX entry "@Article{ authors = {A. Kizune}, year = {2017}, pages={123-6}}".
 */
public class BracketedExpressionExpander {

    private final BibEntry bibentry;

    /**
     * @param bibentry
     */
    public BracketedExpressionExpander(BibEntry bibentry) {
        this.bibentry = bibentry;
    }

    @Override
    public String toString() {
        return "BracketedExpressionExpander [bibentry=" + bibentry + "]";
    }

    public String expandBrackets(String pattern) {
        String expandedString = pattern;

        return expandedString;
    }

}

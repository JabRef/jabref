package org.jabref.logic.util;

import java.lang.StringBuilder;
import java.util.StringTokenizer;
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
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(pattern,"[]",true);

        String separator = "'";
        while(st.hasMoreTokens()) {
            sb.append(separator);
            sb.append(st.nextToken());
            sb.append("'");
            separator = ", '";
        }

        return sb.toString();
    }

}

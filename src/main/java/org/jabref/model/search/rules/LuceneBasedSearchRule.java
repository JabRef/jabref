package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search rule for a search based on String.contains()
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public class LuceneBasedSearchRule extends FullTextSearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneBasedSearchRule.class);

    // We use SyntaxParser to be consistent with org.jabref.logic.importer
    // We do not use "PrecedenceQueryParser", because this parser keeps the term "AND"
    private SyntaxParser parser = new StandardSyntaxParser();
    private LoadingCache<String, QueryNode> cache = CacheBuilder.newBuilder()
                                                            .maximumSize(100)
                                                            .build(
                                                             new CacheLoader<>() {
                                                                 @Override
                                                                 public QueryNode load(String query) throws Exception {
                                                                     return parser.parse(query, "");
                                                                 }
                                                             }
                                                     );

    public LuceneBasedSearchRule(EnumSet<SearchFlags> searchFlags) {
        super(searchFlags);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        // Using the Lucene parser and checking for an exception is the only way to check whether the query is valid
        try {
            parser.parse(query, "");
        } catch (QueryNodeException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean applyRule(String queryString, BibEntry bibEntry) {
        String searchString = queryString;
        QueryNode query;
        try {
            query = cache.get(searchString);
        } catch (ExecutionException e) {
            LOGGER.error("Could not parse query {}", queryString, e);
            return false;
        }

        BibQueryVisitor bibQueryVisitor = new BibQueryVisitor(bibEntry, searchFlags);
        if (bibQueryVisitor.matchFound(query)) {
            return true;
        }

        return getFulltextResults(queryString, bibEntry).numSearchResults() > 0; // Didn't match all words.
    }

}

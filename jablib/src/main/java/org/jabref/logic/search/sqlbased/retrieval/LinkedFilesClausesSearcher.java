package org.jabref.logic.search.sqlbased.retrieval;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javafx.util.Pair;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

public final class LinkedFilesClausesSearcher {

    public static Map<String, TopDocs> getClausesResults(IndexSearcher indexSearcher, Query searchQuery) throws IOException {
        Map<String, TopDocs> result = new HashMap<>();
        Map<String, Set<Query>> termQueries = getQueries(searchQuery);

        for (Entry<String, Set<Query>> fieldToQueries : termQueries.entrySet()) {
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            fieldToQueries.getValue().forEach(term -> queryBuilder.add(
                    new BooleanQuery.Builder().add(term, BooleanClause.Occur.FILTER).build(), BooleanClause.Occur.SHOULD)
            );
            result.put(fieldToQueries.getKey(), indexSearcher.search(queryBuilder.build(), Integer.MAX_VALUE));
        }
        return result;
    }

    private static Map<String, Set<Query>> getQueries(Query searchQuery) {
        return getQueries(searchQuery, new HashMap<>());
    }

    private static Map<String, Set<Query>> getQueries(Query searchQuery, Map<String, Set<Query>> result) {
        Optional<Pair<String, Query>> newQuery = Optional.empty();
        if (searchQuery instanceof TermQuery termQuery) {
            newQuery = Optional.of(new Pair<>(getTermQueryValue(termQuery), termQuery));
        } else if (searchQuery instanceof PhraseQuery phraseQuery) {
            newQuery = Optional.of(new Pair<>(getPhraseQueryValue(phraseQuery), phraseQuery));
        } else if (searchQuery instanceof BooleanQuery booleanQuery) {
            for (BooleanClause clause : booleanQuery.clauses()) {
                getQueries(clause.query(), result);
            }
        }
        if (newQuery.isPresent()) {
            String key = newQuery.get().getKey();
            Query query = newQuery.get().getValue();
            if (result.containsKey(key)) {
                result.get(key).add(query);
            } else {
                result.put(key, new HashSet<>(List.of(query)));
            }
        }
        return result;
    }

    private static String getTermQueryValue(TermQuery termQuery) {
        return termQuery.toString(termQuery.getTerm().field());
    }

    private static String getPhraseQueryValue(PhraseQuery phraseQuery) {
        return phraseQuery.toString(phraseQuery.getField());
    }
}

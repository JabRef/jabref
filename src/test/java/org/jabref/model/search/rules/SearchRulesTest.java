package org.jabref.model.search.rules;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SearchRulesTest {

    @Test
    void simpleLuceneQueryReturnsContainsBasedSearchRule() {
        SearchRule searchRule = SearchRules.getSearchRuleByQuery("test", EnumSet.noneOf(SearchRules.SearchFlags.class));
        assertInstanceOf(ContainsBasedSearchRule.class, searchRule);
    }

    @Test
    void andLuceneQueryReturnsLuceneBasedSearchRule() {
        SearchRule searchRule = SearchRules.getSearchRuleByQuery("test AND lucene", EnumSet.noneOf(SearchRules.SearchFlags.class));
        assertInstanceOf(LuceneBasedSearchRule.class, searchRule);
    }

    @Test
    void simpleFieldedLuceneQueryReturnsLuceneBasedSearchRule() {
        SearchRule searchRule = SearchRules.getSearchRuleByQuery("title:test", EnumSet.noneOf(SearchRules.SearchFlags.class));
        assertInstanceOf(LuceneBasedSearchRule.class, searchRule);
    }

}

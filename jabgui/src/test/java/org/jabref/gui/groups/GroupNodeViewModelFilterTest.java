package org.jabref.gui.groups;

import org.jabref.logic.search.query.GroupNameFilterVisitor;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupNodeViewModelFilterTest {

    private boolean matches(String groupName, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        try {
            var ctx = SearchQuery.getStartContext(query);
            return new GroupNameFilterVisitor(groupName).visit(ctx);
        } catch (org.antlr.v4.runtime.misc.ParseCancellationException e) {
            return groupName.toLowerCase().contains(query.toLowerCase());
        }
    }

    @Test
    void spaceImpliesOrFirstToken() {
        assertTrue(matches("machine", "machine learning"));
    }

    @Test
    void spaceImpliesOrSecondToken() {
        assertTrue(matches("learning", "machine learning"));
    }

    @Test
    void spaceImpliesOrBothTokens() {
        assertTrue(matches("machine learning", "machine learning"));
    }

    @Test
    void explicitAndRequiresBothWords() {
        assertFalse(matches("machine", "machine AND neural"));
        assertTrue(matches("machine neural", "machine AND neural"));
    }

    @Test
    void explicitOrWorks() {
        assertTrue(matches("machine", "machine OR neural"));
        assertTrue(matches("neural", "machine OR neural"));
        assertFalse(matches("unrelated", "machine OR neural"));
    }

    @Test
    void notWorks() {
        assertFalse(matches("machine", "NOT machine"));
        assertTrue(matches("learning", "NOT machine"));
    }

    @Test
    void blankQueryMatchesAll() {
        assertTrue(matches("anything", ""));
        assertTrue(matches("anything", "   "));
    }

    @Test
    void caseInsensitiveMatch() {
        assertTrue(matches("Machine Learning", "machine"));
        assertTrue(matches("machine learning", "MACHINE"));
    }

    @Test
    void simpleTermMatches() {
        assertTrue(matches("test group", "test"));
    }

    @Test
    void parenthesesWithNotWork() {
        assertTrue(matches("Deep Learning", "(deep OR neural) NOT vision"));
        assertTrue(matches("Neural Networks", "(deep OR neural) NOT vision"));
        assertFalse(matches("Computer Vision", "(deep OR neural) NOT vision"));
        assertFalse(matches("Machine Learning", "(deep OR neural) NOT vision"));
    }

    @Test
    void complexExpressionWithNotLearning() {
        assertTrue(matches("Computer Vision", "(machine OR computer) NOT learning"));
        assertFalse(matches("Machine Learning", "(machine OR computer) NOT learning"));
        assertFalse(matches("Neural Networks", "(machine OR computer) NOT learning"));
    }
}

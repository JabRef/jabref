package org.jabref.gui.groups;

import org.jabref.logic.search.query.GroupNameFilterVisitor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupNodeViewModelFilterTest {

    @ParameterizedTest
    @CsvSource({
            "machine, machine learning, true",
            "learning, machine learning, true",
            "machine learning, machine learning, true",
            "Neural Networks, machine learning, false",
            "test group, test, true"
    })
    void spaceImpliesOr(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "machine neural, machine AND neural, true",
            "machine, machine AND neural, false",
            "neural, machine AND neural, false"
    })
    void explicitAndRequiresBothWords(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "machine, machine OR neural, true",
            "neural, machine OR neural, true",
            "unrelated, machine OR neural, false"
    })
    void explicitOrWorks(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "machine, NOT machine, false",
            "learning, NOT machine, true"
    })
    void notWorks(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "Machine Learning, machine, true",
            "machine learning, MACHINE, true"
    })
    void caseInsensitiveMatch(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "Deep Learning, (deep OR neural) NOT vision, true",
            "Neural Networks, (deep OR neural) NOT vision, true",
            "Computer Vision, (deep OR neural) NOT vision, false",
            "Machine Learning, (deep OR neural) NOT vision, false"
    })
    void parenthesesWithNotWork(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }

    @ParameterizedTest
    @CsvSource({
            "Computer Vision, (machine OR computer) NOT learning, true",
            "Machine Learning, (machine OR computer) NOT learning, false",
            "Neural Networks, (machine OR computer) NOT learning, false"
    })
    void complexExpressionWithNotLearning(String groupName, String query, boolean expected) {
        assertEquals(expected, GroupNameFilterVisitor.matches(groupName, query));
    }
}

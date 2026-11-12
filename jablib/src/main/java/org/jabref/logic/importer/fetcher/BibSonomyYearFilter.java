package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.NotNode;
import org.jabref.model.search.query.OperatorNode;
import org.jabref.model.search.query.SearchQueryNode;

import org.jspecify.annotations.NullMarked;

/// Applies the year terms of a search query to the entries returned by BibSonomy.
/// The query tree is evaluated, because reducing all year terms to one range would lose the meaning of `OR` and `NOT`.
@NullMarked
final class BibSonomyYearFilter {
    private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("\\d{4}");

    private final Optional<Predicate<Optional<Integer>>> yearCondition;

    BibSonomyYearFilter(BaseQueryNode query) {
        yearCondition = toYearCondition(query);
    }

    /// A year term never matches an entry without a four-digit year, because the year of that entry is unknown
    boolean accepts(BibEntry entry) {
        Optional<Integer> year = entry.getField(StandardField.YEAR)
                                      .filter(value -> FOUR_DIGIT_YEAR.matcher(value).matches())
                                      .map(Integer::parseInt);
        return yearCondition.map(condition -> condition.test(year)).orElse(true);
    }

    /// Tells whether an entry has to satisfy every year term of the query.
    /// Only then, the years may be sent to BibSonomy: otherwise, BibSonomy would drop entries matching another branch of the query.
    static boolean allYearTermsAreRequired(BaseQueryNode query) {
        return switch (query) {
            case OperatorNode node when node.op() == OperatorNode.Operator.OR ->
                    node.children().stream().noneMatch(BibSonomyYearFilter::containsYearTerm);
            case OperatorNode node ->
                    node.children().stream().allMatch(BibSonomyYearFilter::allYearTermsAreRequired);
            case NotNode node ->
                    !containsYearTerm(node.negatedNode());
            default ->
                    true;
        };
    }

    private static boolean containsYearTerm(BaseQueryNode query) {
        return switch (query) {
            case OperatorNode node ->
                    node.children().stream().anyMatch(BibSonomyYearFilter::containsYearTerm);
            case NotNode node ->
                    containsYearTerm(node.negatedNode());
            case SearchQueryNode node ->
                    isYearField(node) || isYearRangeField(node);
            default ->
                    false;
        };
    }

    /// @return the condition on the year of an entry. Empty if the query does not restrict the year.
    private static Optional<Predicate<Optional<Integer>>> toYearCondition(BaseQueryNode query) {
        return switch (query) {
            case OperatorNode node ->
                    combine(node);
            case NotNode node ->
                    toYearCondition(node.negatedNode()).map(Predicate::not);
            case SearchQueryNode node when isYearField(node) ->
                    Optional.of(yearBetween(node.term(), node.term()));
            case SearchQueryNode node when isYearRangeField(node) ->
                    Optional.of(toYearRangeCondition(node.term()));
            default ->
                    Optional.empty();
        };
    }

    private static Optional<Predicate<Optional<Integer>>> combine(OperatorNode node) {
        List<Optional<Predicate<Optional<Integer>>>> childConditions = node.children().stream()
                                                                           .map(BibSonomyYearFilter::toYearCondition)
                                                                           .toList();
        if (node.op() == OperatorNode.Operator.OR) {
            // A branch without a year term may match an entry of any year
            return childConditions.stream().allMatch(Optional::isPresent)
                   ? childConditions.stream().map(Optional::orElseThrow).reduce(Predicate::or)
                   : Optional.empty();
        }
        return childConditions.stream().flatMap(Optional::stream).reduce(Predicate::and);
    }

    private static Predicate<Optional<Integer>> toYearRangeCondition(String yearRange) {
        String[] years = yearRange.split("-");
        return yearBetween(years[0], years[1]);
    }

    private static Predicate<Optional<Integer>> yearBetween(String firstYear, String secondYear) {
        int start = Math.min(Integer.parseInt(firstYear), Integer.parseInt(secondYear));
        int end = Math.max(Integer.parseInt(firstYear), Integer.parseInt(secondYear));
        return year -> year.filter(value -> (value >= start) && (value <= end)).isPresent();
    }

    private static boolean isYearField(SearchQueryNode node) {
        return hasFieldName(node, "year");
    }

    private static boolean isYearRangeField(SearchQueryNode node) {
        return hasFieldName(node, "year-range");
    }

    private static boolean hasFieldName(SearchQueryNode node, String name) {
        return node.field().filter(field -> name.equalsIgnoreCase(field.getName())).isPresent();
    }
}

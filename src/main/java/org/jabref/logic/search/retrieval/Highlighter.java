package org.jabref.logic.search.retrieval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.model.util.Range;

import com.airhacks.afterburner.injection.Injector;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Highlighter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Highlighter.class);

    /**
     * Functions defined in {@link PostgreConstants#POSTGRES_FUNCTIONS}
     */
    private static final String REGEXP_MARK = "SELECT regexp_mark(?, ?)";
    private static final String REGEXP_POSITIONS = "SELECT * FROM regexp_positions(?, ?)";
    private static Connection connection;

    public static String highlightHtml(String htmlText, SearchQuery searchQuery) {
        Optional<String> searchTermsPattern = buildSearchPattern(searchQuery);
        if (searchTermsPattern.isEmpty()) {
            return htmlText;
        }

        Document document = Jsoup.parse(htmlText);
        try {
            highlightTextNodes(document.body(), searchTermsPattern.get());
            return document.outerHtml();
        } catch (InvalidTokenOffsetsException e) {
            LOGGER.debug("Error highlighting search terms in HTML", e);
            return htmlText;
        }
    }

    private static void highlightTextNodes(Element element, String searchPattern) throws InvalidTokenOffsetsException {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                String highlightedText = highlightNode(textNode.text(), searchPattern);
                textNode.text("");
                textNode.after(highlightedText);
            } else if (node instanceof Element element1) {
                highlightTextNodes(element1, searchPattern);
            }
        }
    }

    private static String highlightNode(String text, String searchPattern) {
        if (connection == null) {
            connection = Injector.instantiateModelOrService(PostgreServer.class).getConnection();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(REGEXP_MARK)) {
            preparedStatement.setString(1, text);
            preparedStatement.setString(2, searchPattern);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error highlighting search terms in text", e);
        }
        return text;
    }

    public static List<Range> findMatchPositions(String text, String pattern) {
        if (connection == null) {
            connection = Injector.instantiateModelOrService(PostgreServer.class).getConnection();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(REGEXP_POSITIONS)) {
            preparedStatement.setString(1, text);
            preparedStatement.setString(2, pattern);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Range> positions = new ArrayList<>();
                while (resultSet.next()) {
                    positions.add(new Range(resultSet.getInt(1), resultSet.getInt(2)));
                }
                return positions;
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting match positions in text", e);
        }
        return List.of();
    }

    public static Map<Optional<Field>, List<String>> groupTermsByField(SearchQuery searchQuery) {
        if (!searchQuery.isValid()) {
            return Map.of();
        }

        List<SearchQueryNode> queryNodes = getSearchQueryNodes(searchQuery);
        Map<Optional<Field>, List<String>> searchTermsMap = new HashMap<>();
        for (SearchQueryNode searchTerm : queryNodes) {
            searchTermsMap.computeIfAbsent(searchTerm.field(), k -> new ArrayList<>()).add(searchTerm.term());
        }
        return searchTermsMap;
    }

    private static Optional<String> buildSearchPattern(SearchQuery searchQuery) {
        if (!searchQuery.isValid()) {
            return Optional.empty();
        }

        List<String> terms = getSearchQueryNodes(searchQuery).stream()
                                                             .map(SearchQueryNode::term)
                                                             .toList();
        return buildSearchPattern(terms);
    }

    private static List<SearchQueryNode> getSearchQueryNodes(SearchQuery searchQuery) {
        return searchQuery.isValid() ? SearchQueryConversion.extractSearchTerms(searchQuery) : List.of();
    }

    public static Optional<String> buildSearchPattern(List<String> terms) {
        return terms.isEmpty() ? Optional.empty() : Optional.of(String.join("|", terms));
    }
}

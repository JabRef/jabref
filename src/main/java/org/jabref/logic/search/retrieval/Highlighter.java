package org.jabref.logic.search.retrieval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.query.SearchQuery;
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
        Optional<String> searchTermsPattern = getSearchTermsPattern(searchQuery);
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

    public static List<Range> getMatchPositions(String text, String pattern) {
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

    public static Optional<String> getSearchTermsPattern(SearchQuery searchQuery) {
        if (!searchQuery.isValid()) {
            return Optional.empty();
        }

        Set<String> terms = SearchQueryConversion.extractSearchTerms(searchQuery);
        if (terms.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(String.join("|", terms));
    }
}

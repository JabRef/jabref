package org.jabref.logic.search.retrieval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.query.SearchQueryConversion;
import org.jabref.model.search.query.SearchQuery;

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
    private final static String HIGHLIGHT_QUERY = """
            SELECT
            regexp_replace(
                ?,
                ?,
                '<mark style="background: orange">\\1</mark>',
                'gi'
            )
            """;
    private static Connection connection;

    public static String highlightHtml(String htmlText, SearchQuery searchQuery) {
        if (!searchQuery.isValid()) {
            return htmlText;
        }

        LOGGER.debug("Highlighting search terms in text: {}", searchQuery);
        Set<String> terms = SearchQueryConversion.extractSearchTerms(searchQuery);
        if (terms.isEmpty()) {
            return htmlText;
        }

        String joinedTerms = String.join("|", terms);
        Document document = Jsoup.parse(htmlText);
        try {
            highlightTextNodes(document.body(), joinedTerms);
            String highlightedHtml = document.outerHtml();
            LOGGER.debug("Highlighted HTML: {}", highlightedHtml);
            return highlightedHtml;
        } catch (InvalidTokenOffsetsException e) {
            LOGGER.debug("Error highlighting search terms in HTML", e);
            return htmlText;
        }
    }

    private static void highlightTextNodes(Element element, String searchTerms) throws InvalidTokenOffsetsException {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                String highlightedText = highlightNode(textNode.text(), searchTerms);
                textNode.text("");
                textNode.after(highlightedText);
            } else if (node instanceof Element element1) {
                highlightTextNodes(element1, searchTerms);
            }
        }
    }

    private static String highlightNode(String text, String searchTerms) {
        if (connection == null) {
            connection = Injector.instantiateModelOrService(PostgreServer.class).getConnection();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(HIGHLIGHT_QUERY)) {
            preparedStatement.setString(1, text);
            preparedStatement.setString(2, '(' + searchTerms + ')');

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
}

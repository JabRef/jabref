package org.jabref.logic.search;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.model.search.SearchFieldConstants;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneHighlighter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneHighlighter.class);
    private static final Formatter FORMATTER = new SimpleHTMLFormatter("<mark>", "</mark>");
    private static final Fragmenter FRAGMENTER = new NullFragmenter();
    private static final Pattern HIGHLIGHTED_TERM_PATTERN = Pattern.compile("<mark>(.*?)</mark>");

    public static String highlightPreviewViewer(String htmlText, Query query) {
        Highlighter highlighter = new Highlighter(
                new SimpleHTMLFormatter("<mark>", "</mark>"),
                new QueryScorer(query));
        highlighter.setTextFragmenter(new NullFragmenter());

        try {
            Document doc = Jsoup.parse(htmlText);
            highlightTextNodes(doc.body(), highlighter);
            return doc.outerHtml();
        } catch (InvalidTokenOffsetsException | IOException e) {
            return htmlText;
        }
    }

    private static void highlightTextNodes(Element element, Highlighter highlighter) throws InvalidTokenOffsetsException, IOException {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                String originalText = textNode.text();
                String highlightedText = highlighter.getBestFragment(SearchFieldConstants.NGram_Analyzer_For_HIGHLIGHING, "", originalText);
                if (highlightedText != null) {
                    textNode.text("");
                    textNode.after(highlightedText);
                }
            } else if (node instanceof Element) {
                highlightTextNodes((Element) node, highlighter);
            }
        }
    }

    public static Optional<Pattern> getHighlightingPattern(String content, Query query) {
        try {
            Highlighter highlighter = new Highlighter(FORMATTER, new QueryScorer(query));
            highlighter.setTextFragmenter(FRAGMENTER);
            String highlightedText = highlighter.getBestFragment(SearchFieldConstants.NGram_Analyzer_For_HIGHLIGHING, null, content);
            if (highlightedText == null) {
                return Optional.empty();
            }
            LOGGER.debug("Highlighted text: {}", highlightedText);
            Set<String> matchedTerms = getMatchedTerms(highlightedText);
            return Optional.of(Pattern.compile(
                    matchedTerms.stream()
                                .sorted(Comparator.comparing(String::length))
                                .collect(Collectors.joining("|")),
                    Pattern.CASE_INSENSITIVE));
        } catch (InvalidTokenOffsetsException | IOException e) {
            return Optional.empty();
        }
    }

    private static Set<String> getMatchedTerms(String highlightedText) {
        Set<String> matchedTerms = new HashSet<>();
        Matcher matcher = HIGHLIGHTED_TERM_PATTERN.matcher(highlightedText);
        while (matcher.find()) {
            matchedTerms.add(matcher.group(1));
        }
        return matchedTerms;
    }
}

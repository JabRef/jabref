package org.jabref.logic.search;

import java.io.IOException;

import org.jabref.model.search.SearchFieldConstants;

import org.apache.lucene.search.Query;
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

public class LuceneHighlighter {

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
                String highlightedText = highlighter.getBestFragment(SearchFieldConstants.NGram_ANALYZER, "", originalText);
                if (highlightedText != null) {
                    textNode.text("");
                    textNode.after(highlightedText);
                }
            } else if (node instanceof Element) {
                highlightTextNodes((Element) node, highlighter);
            }
        }
    }
}

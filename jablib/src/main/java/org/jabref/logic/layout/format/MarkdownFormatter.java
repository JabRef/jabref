package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jspecify.annotations.NonNull;

public class MarkdownFormatter implements LayoutFormatter {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownFormatter() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public String format(@NonNull final String fieldText) {
        Node document = parser.parse(fieldText);
        String html = renderer.render(document);

        // workaround HTMLChars transforming "\n" into <br> by returning a one liner
        return html.replaceAll("\\r\\n|\\r|\\n", " ").trim();
    }
}

package org.jabref.logic.layout.format;

import java.io.File;
import java.util.Objects;

import org.jabref.gui.fieldeditors.MarkdownEditor;
import org.jabref.logic.layout.LayoutFormatter;


import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jabref.model.database.BibDatabaseContext;

public class MarkdownFormatter implements LayoutFormatter {

    private final Parser parser;
    private final HtmlRenderer renderer;


    public MarkdownFormatter() {
        MutableDataSet options = new MutableDataSet();
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();


    }

    @Override
    public String format(final String fieldText) {
        Objects.requireNonNull(fieldText, "Field Text should not be null, when handed to formatter");

        Node document = parser.parse(fieldText);
        String html = renderer.render(document);

        // workaround HTMLChars transforming "\n" into <br> by returning a one liner
        return html.replaceAll("\\r\\n|\\r|\\n", " ").trim();
    }



}

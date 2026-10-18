package org.jabref.gui.bibtexhighlighter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;

import org.jabref.gui.util.ColorUtil;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jsoup.nodes.Entities;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Exports BibTeX source as HTML for rich-text applications such as LibreOffice Writer, Microsoft Word, and Google Docs.
///
/// The default `HtmlExportFormatHandler` writes one `<p/>` per line and no font, so pasted source loses its indentation and alignment.
/// This handler writes the source as one monospace `<pre>` block and keeps the syntax colors of the current theme.
@NullMarked
public class BibTeXHtmlExportFormatHandler extends DataFormatHandler {

    public BibTeXHtmlExportFormatHandler() {
        super(DataFormat.HTML);
    }

    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attributes) {
        throw new UnsupportedOperationException("BibTeX source cannot be imported from HTML");
    }

    // [impl->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    @Override
    public Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        StringWriter writer = new StringWriter();
        export(model, resolver, start, end, writer);
        return writer.toString();
    }

    @Override
    public void save(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        export(model, resolver, start, end, writer);
        writer.flush();
    }

    private static void export(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, Writer writer) throws IOException {
        writer.write("<html><head><meta charset=\"utf-8\"></head><body><pre style=\"font-family: monospace;\">");
        model.export(start, end, new PreformattedOutput(resolver, writer));
        writer.write("</pre></body></html>");
    }

    private record PreformattedOutput(StyleResolver resolver, Writer writer) implements StyledOutput {

        @Override
        public void consume(StyledSegment segment) throws IOException {
            switch (segment.getType()) {
                case TEXT ->
                        writeText(segment.getText(), segment.getStyleAttributeMap(resolver));
                case LINE_BREAK ->
                        writer.write('\n');
                default -> {
                    // Code areas contain no images, paragraph attributes, or document properties
                }
            }
        }

        private void writeText(String text, @Nullable StyleAttributeMap attributes) throws IOException {
            String css = attributes == null ? "" : toCss(attributes);
            if (css.isEmpty()) {
                writer.write(Entities.escape(text));
                return;
            }
            writer.write("<span style=\"");
            writer.write(css);
            writer.write("\">");
            writer.write(Entities.escape(text));
            writer.write("</span>");
        }

        private static String toCss(StyleAttributeMap attributes) {
            StringJoiner css = new StringJoiner(" ");
            Color color = attributes.getTextColor();
            if (color != null) {
                css.add("color: " + ColorUtil.toRGBCode(color) + ";");
            }
            if (attributes.isBold()) {
                css.add("font-weight: bold;");
            }
            if (attributes.isItalic()) {
                css.add("font-style: italic;");
            }
            StringJoiner decoration = new StringJoiner(" ");
            if (attributes.isUnderline()) {
                decoration.add("underline");
            }
            if (attributes.isStrikeThrough()) {
                decoration.add("line-through");
            }
            if (decoration.length() > 0) {
                css.add("text-decoration: " + decoration + ";");
            }
            return css.toString();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        /// The writer belongs to the caller, so it stays open.
        @Override
        public void close() throws IOException {
            writer.flush();
        }
    }
}

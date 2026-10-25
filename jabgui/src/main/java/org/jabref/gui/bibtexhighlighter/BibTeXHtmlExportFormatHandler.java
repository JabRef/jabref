package org.jabref.gui.bibtexhighlighter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javafx.scene.input.DataFormat;

import org.jabref.gui.util.ColorUtil;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jsoup.nodes.Entities;
import org.jspecify.annotations.NullMarked;

/// Exports BibTeX source as HTML for rich-text applications such as LibreOffice Writer, Microsoft Word, and Google Docs.
///
/// The default `HtmlExportFormatHandler` writes one `<p/>` per line and no font, so pasted source loses its indentation and alignment.
/// This handler writes the source as one monospace `<pre>` block and keeps the syntax colors of the current theme.
@NullMarked
public class BibTeXHtmlExportFormatHandler extends DataFormatHandler {

    /// Same font as [BibTeXRtfExportFormatHandler].
    /// HTML drops a line break directly after `<pre>`, so the one written here keeps a selection's leading line break.
    private static final String PROLOGUE = "<html><head><meta charset=\"utf-8\"></head><body><pre style=\"font-family: 'Courier New', monospace;\">\n";

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
        writer.write(PROLOGUE);
        for (TextRun run : TextRun.collect(model, resolver, start, end)) {
            String text = Entities.escape(run.text());
            writer.write(run.color().map(color -> "<span style=\"color: " + ColorUtil.toRGBCode(color) + ";\">" + text + "</span>").orElse(text));
        }
        writer.write("</pre></body></html>");
    }
}

package org.jabref.gui.bibtexhighlighter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Exports BibTeX source as RTF for rich-text applications such as Pages, Microsoft Word, and LibreOffice Writer.
///
/// Many of these applications prefer RTF over HTML when both are on the clipboard.
/// The default `RtfFormatHandler` names a proportional font, so pasted source loses its alignment.
/// This handler uses a monospace font and keeps the syntax colors of the current theme.
@NullMarked
public class BibTeXRtfExportFormatHandler extends DataFormatHandler {

    /// `\fmodern` marks the font as fixed-pitch, so applications without Courier New pick another monospace font
    private static final String HEADER = "{\\rtf1\\ansi\\uc1\\deff0{\\fonttbl{\\f0\\fmodern Courier New;}}";

    public BibTeXRtfExportFormatHandler() {
        super(DataFormat.RTF);
    }

    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attributes) {
        throw new UnsupportedOperationException("BibTeX source cannot be imported from RTF");
    }

    // [impl->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    @Override
    public Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        return export(model, resolver, start, end);
    }

    @Override
    public void save(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException {
        out.write(export(model, resolver, start, end).getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static String export(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        // The color table comes before the text, so collect the text first
        List<Run> runs = new ArrayList<>();
        model.export(start, end, new RunCollector(resolver, runs));
        List<Color> colors = runs.stream()
                                 .map(Run::color)
                                 .filter(Objects::nonNull)
                                 .distinct()
                                 .toList();

        StringBuilder rtf = new StringBuilder(HEADER);
        rtf.append("{\\colortbl;");
        for (Color color : colors) {
            rtf.append("\\red").append(toByte(color.getRed()))
               .append("\\green").append(toByte(color.getGreen()))
               .append("\\blue").append(toByte(color.getBlue()))
               .append(';');
        }
        rtf.append("}\n\\f0 ");
        for (Run run : runs) {
            appendRun(rtf, run, colors);
        }
        return rtf.append('}').toString();
    }

    private static void appendRun(StringBuilder rtf, Run run, List<Color> colors) {
        StringBuilder controls = new StringBuilder();
        Color color = run.color();
        if (color != null) {
            // Index 0 of the color table is the default color
            controls.append("\\cf").append(colors.indexOf(color) + 1);
        }
        StyleAttributeMap attributes = run.attributes();
        if (attributes != null) {
            if (attributes.isBold()) {
                controls.append("\\b");
            }
            if (attributes.isItalic()) {
                controls.append("\\i");
            }
            if (attributes.isUnderline()) {
                controls.append("\\ul");
            }
            if (attributes.isStrikeThrough()) {
                controls.append("\\strike");
            }
        }
        if (controls.isEmpty()) {
            appendEscaped(rtf, run.text());
            return;
        }
        rtf.append('{').append(controls).append(' ');
        appendEscaped(rtf, run.text());
        rtf.append('}');
    }

    private static void appendEscaped(StringBuilder rtf, String text) {
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\', '{', '}' ->
                        rtf.append('\\').append(c);
                case '\t' ->
                        rtf.append("\\tab ");
                case '\n' ->
                        rtf.append("\\line\n");
                default -> {
                    if (c < 0x80) {
                        rtf.append(c);
                    } else {
                        // RTF takes signed 16-bit code units; "?" is the fallback for readers without Unicode support
                        rtf.append("\\u").append((short) c).append('?');
                    }
                }
            }
        }
    }

    private static int toByte(double component) {
        return (int) Math.round(component * 255);
    }

    /// A piece of text with its style; a line break is a run of `"\n"`.
    private record Run(String text, @Nullable StyleAttributeMap attributes) {
        @Nullable Color color() {
            return attributes == null ? null : attributes.getTextColor();
        }
    }

    private record RunCollector(StyleResolver resolver, List<Run> runs) implements StyledOutput {

        @Override
        public void consume(StyledSegment segment) {
            switch (segment.getType()) {
                case TEXT ->
                        runs.add(new Run(segment.getText(), segment.getStyleAttributeMap(resolver)));
                case LINE_BREAK ->
                        runs.add(new Run("\n", null));
                default -> {
                    // Code areas contain no images, paragraph attributes, or document properties
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}

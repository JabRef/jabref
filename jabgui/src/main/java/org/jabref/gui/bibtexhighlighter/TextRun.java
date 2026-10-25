package org.jabref.gui.bibtexhighlighter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jspecify.annotations.NullMarked;

/// A piece of BibTeX source with its text color, as [BibTeXHtmlExportFormatHandler] and [BibTeXRtfExportFormatHandler] export it.
///
/// A line break is a run of `"\n"`.
/// The BibTeX stylesheets only set the fill, so the color is the only style the exports keep.
@NullMarked
record TextRun(String text, Optional<Color> color) {
    /// Collects the runs between `start` and `end`, with the colors `resolver` computes from the current theme.
    static List<TextRun> collect(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        List<TextRun> runs = new ArrayList<>();
        model.export(start, end, new StyledOutput() {
            @Override
            public void consume(StyledSegment segment) {
                switch (segment.getType()) {
                    case TEXT -> {
                        Optional<Color> color = Optional.ofNullable(segment.getStyleAttributeMap(resolver)).map(StyleAttributeMap::getTextColor);
                        runs.add(new TextRun(segment.getText(), color));
                    }
                    case LINE_BREAK ->
                            runs.add(new TextRun("\n", Optional.empty()));
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
        });
        return runs;
    }
}

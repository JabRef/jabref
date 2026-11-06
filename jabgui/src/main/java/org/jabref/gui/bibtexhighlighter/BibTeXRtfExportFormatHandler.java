package org.jabref.gui.bibtexhighlighter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DataFormat;

import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.DataFormatHandler;
import jfx.incubator.scene.control.richtext.model.RtfFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.jspecify.annotations.NullMarked;

/// Exports BibTeX source using JavaFX's RTF writer with a monospace font.
@NullMarked
public class BibTeXRtfExportFormatHandler extends DataFormatHandler {
    private static final StyleAttributeMap MONOSPACE = StyleAttributeMap.builder().setFontFamily("monospaced").build();

    public BibTeXRtfExportFormatHandler() {
        super(DataFormat.RTF);
    }

    @Override
    public StyledInput createStyledInput(Object input, StyleAttributeMap attributes) {
        throw new UnsupportedOperationException("BibTeX source cannot be imported from RTF");
    }

    // [impl->req~entry-editor.source-tab.copy-layout~1,req~entry-editor.source-tab.copy-syntax-colors~1]
    @Override
    public Object copy(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end) throws IOException {
        return RtfFormatHandler.getInstance().copy(model, monospaceResolver(resolver), start, end);
    }

    @Override
    public void save(StyledTextModel model, StyleResolver resolver, TextPos start, TextPos end, OutputStream out) throws IOException {
        String rtf = (String) copy(model, resolver, start, end);
        out.write(rtf.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static StyleResolver monospaceResolver(StyleResolver resolver) {
        return new StyleResolver() {
            @Override
            public StyleAttributeMap resolveStyles(StyleAttributeMap attributes) {
                return resolver.resolveStyles(attributes).combine(MONOSPACE);
            }

            @Override
            public WritableImage snapshot(Node node) {
                return resolver.snapshot(node);
            }
        };
    }
}

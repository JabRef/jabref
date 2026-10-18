package org.jabref.gui.bibtexhighlighter;

import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import org.jspecify.annotations.NullMarked;

/// [CodeTextModel] whose HTML export keeps BibTeX source in a monospace `<pre>` block.
///
/// The export handlers live in the model, so every copy route of the code area uses them:
/// Ctrl+C, Ctrl+Insert, cut, and the context menu.
@NullMarked
public class BibTeXCodeTextModel extends CodeTextModel {

    public BibTeXCodeTextModel() {
        // Same priority as the default HTML handler, which this replaces
        registerDataFormatHandler(new BibTeXHtmlExportFormatHandler(), true, false, 100);
    }
}

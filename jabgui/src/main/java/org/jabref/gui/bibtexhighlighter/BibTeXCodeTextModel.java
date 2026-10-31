package org.jabref.gui.bibtexhighlighter;

import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import org.jspecify.annotations.NullMarked;

/// [CodeTextModel] whose HTML and RTF exports keep BibTeX source monospace, so that pasted source stays aligned.
///
/// The export handlers live in the model, so every copy route of the code area uses them:
/// Ctrl+C, Ctrl+Insert, cut, and the context menu.
@NullMarked
public class BibTeXCodeTextModel extends CodeTextModel {

    public BibTeXCodeTextModel() {
        // Same priorities as the default handlers, which these replace
        registerDataFormatHandler(new BibTeXRtfExportFormatHandler(), true, false, 1000);
        registerDataFormatHandler(new BibTeXHtmlExportFormatHandler(), true, false, 100);
    }
}

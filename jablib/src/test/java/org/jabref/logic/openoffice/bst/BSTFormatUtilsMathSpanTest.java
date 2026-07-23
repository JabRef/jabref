package org.jabref.logic.openoffice.bst;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BSTFormatUtilsMathSpanTest {

    @Test
    void stripsEmphasisInsideInlineMathButKeepsOutside() {
        String html = "<p><em>foo</em> <span><span class=\"math inline\"><em>Σ</em></span></span> bar</p>";
        String oo = BSTFormatUtils.convertPandocHtmlToOOText(html);
        // Outside <em> becomes <i>
        assertTrue(oo.contains("<i>foo</i>"));
        // Math content should not be italicized; Σ should exist without <i> wrapper
        assertTrue(oo.contains("Σ"));
        assertFalse(oo.contains("<i>Σ</i>"));
    }
}

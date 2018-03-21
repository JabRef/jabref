package org.jabref.gui;

import de.jensd.fx.glyphs.GlyphIcons;

/**
 * Provides the same true-type font interface as MaterialDesignIcon itself, but uses a font we created ourselves that
 * contains icons that are not available in MaterialDesignIcons.
 *
 * @implNote The glyphs of the ttf (speak: the icons) were created with Illustrator and a template from the material design icons
 * web-page. The art boards for each icon was exported as SVG and then converted with glypher.com. The final TTF font is
 * located in the resource folder.
 *
 * {@see /jabref/src/main/resources/fonts/JabRefMaterialDesign.ttf}
 * {@see https://materialdesignicons.com/custom}
 */
public enum JabRefMaterialDesignIcon implements GlyphIcons {

    TEX_STUDIO("\ue900"),
    TEX_MAKER("\ue901"),
    EMACS("\ue902"),
    OPEN_OFFICE("\ue903"),
    VIM("\ue904"),
    LYX("\ue905"),
    WINEDT("\ue906"),
    ARXIV("\ue907");


    private final String unicode;

    JabRefMaterialDesignIcon(String unicode) {
        this.unicode = unicode;
    }

    @Override
    public String unicode() {
        return unicode;
    }

    @Override
    public String fontFamily() {
        return "\'JabRefMaterialDesign\'";
    }
}

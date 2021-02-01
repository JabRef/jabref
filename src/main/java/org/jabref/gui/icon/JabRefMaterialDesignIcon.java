package org.jabref.gui.icon;

import de.jensd.fx.glyphs.GlyphIcons;

/**
 * Provides the same true-type font interface as MaterialDesignIcon itself, but uses a font we created ourselves that
 * contains icons that are not available in MaterialDesignIcons.
 *
 * @implNote The glyphs of the ttf (speak: the icons) were created with Illustrator and a template from the material design icons
 * web-page. The art boards for each icon was exported as SVG and then converted with <a href="https://icomoon.io/app">
 * IcoMoon</a>. The final TTF font is located in the resource folder.
 * @see <a href="https://github.com/JabRef/jabref/wiki/Custom-SVG-Icons-for-JabRef">Tutorial on our Wiki</a>
 * @see <a href="https://materialdesignicons.com/custom">Material Design Icon custom page</a>
 */
public enum JabRefMaterialDesignIcon implements GlyphIcons {

    TEX_STUDIO("\ue900"),
    TEX_MAKER("\ue901"),
    EMACS("\ue902"),
    OPEN_OFFICE("\ue903"),
    VIM("\ue904"),
    VIM2("\ue905"),
    LYX("\ue906"),
    WINEDT("\ue907"),
    ARXIV("\ue908"),
    COPY("\ue909"),
    PASTE("\ue90a"),
    SET_CENTER("\ue90b"),
    SET_ALL("\ue90c"),
    VSCODE("\ue90d"),
    CANCEL("\ue90e");

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

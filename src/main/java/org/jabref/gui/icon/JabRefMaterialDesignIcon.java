package org.jabref.gui.icon;

import org.kordamp.ikonli.Ikon;

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
public enum JabRefMaterialDesignIcon implements Ikon {

    TEX_STUDIO("jab-texstudio", '\ue900'),
    TEX_MAKER("jab-textmaker", '\ue901'),
    EMACS("jab-emacs", '\ue902'),
    OPEN_OFFICE("jab-oo", '\ue903'),
    VIM("jab-vim", '\ue904'),
    VIM2("jab-vim2", '\ue905'),
    LYX("jab-lyx", '\ue906'),
    WINEDT("jab-winedt", '\ue907'),
    ARXIV("jab-arxiv", '\ue908'),
    COPY("jab-copy", '\ue909'),
    PASTE("jab-paste", '\ue90a'),
    SET_CENTER("jab-setcenter", '\ue90b'),
    SET_ALL("jab-setall", '\ue90c'),
    VSCODE("jab-vsvode", '\ue90d'),
    CANCEL("jab-cancel", '\ue90e');

    private String description;
    private int code;

    JabRefMaterialDesignIcon(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public static JabRefMaterialDesignIcon findByDescription(String description) {
        for (JabRefMaterialDesignIcon font : values()) {
            if (font.getDescription().equals(description)) {
                return font;
            }
        }
        throw new IllegalArgumentException("Icon description '" + description + "' is invalid!");
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getCode() {
        return code;
    }
}

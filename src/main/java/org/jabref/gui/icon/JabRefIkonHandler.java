package org.jabref.gui.icon;

import java.io.InputStream;
import java.net.URL;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

public class JabRefIkonHandler extends AbstractIkonHandler {

    private static String FONT_RESOURCE = "/fonts/JabRefMaterialDesign.ttf";

    @Override
    public boolean supports(String description) {
        return (description != null) && description.startsWith("jab-");
    }

    @Override
    public Ikon resolve(String description) {
        return JabRefMaterialDesignIcon.findByDescription(description);
    }

    @Override
    public URL getFontResource() {
        return getClass().getResource(FONT_RESOURCE);
    }

    @Override
    public InputStream getFontResourceAsStream() {
        return getClass().getResourceAsStream(FONT_RESOURCE);
    }

    @Override
    public String getFontFamily() {
        return "JabRefMaterialDesign";
    }

}

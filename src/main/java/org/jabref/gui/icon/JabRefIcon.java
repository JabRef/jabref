package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import de.jensd.fx.glyphs.GlyphIcons;

public interface JabRefIcon extends GlyphIcons {

    Node getGraphicNode();

    @Override
    String name();

    JabRefIcon withColor(Color color);

    JabRefIcon disabled();
}

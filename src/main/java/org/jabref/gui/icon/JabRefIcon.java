package org.jabref.gui.icon;

import javax.swing.Icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import de.jensd.fx.glyphs.GlyphIcons;

public interface JabRefIcon extends GlyphIcons {
    Icon getIcon();

    Icon getSmallIcon();

    Node getGraphicNode();

    JabRefIcon disabled();

    JabRefIcon withColor(Color color);

    String name();
}

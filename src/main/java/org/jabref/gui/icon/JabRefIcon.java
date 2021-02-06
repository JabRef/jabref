package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public interface JabRefIcon {

    Node getGraphicNode();

    String name();

    JabRefIcon withColor(Color color);

    JabRefIcon disabled();

    String fontFamily();

    String unicode();
}

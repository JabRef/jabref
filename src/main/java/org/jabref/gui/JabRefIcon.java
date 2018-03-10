package org.jabref.gui;

import javax.swing.Icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

public interface JabRefIcon {
    Icon getIcon();

    Icon getSmallIcon();

    Node getGraphicNode();

    JabRefIcon disabled();

    JabRefIcon withColor(Color color);
}

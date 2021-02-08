package org.jabref.gui.icon;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.kordamp.ikonli.Ikon;

public interface JabRefIcon {

    Node getGraphicNode();

    String name();

    JabRefIcon withColor(Color color);

    JabRefIcon disabled();

    Ikon getIkon();
}

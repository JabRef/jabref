package org.jabref.gui;

import javax.swing.Icon;

import javafx.scene.Node;

public interface JabRefIcon {
    Icon getIcon();

    Icon getSmallIcon();

    Node getGraphicNode();

    JabRefIcon disabled();
}

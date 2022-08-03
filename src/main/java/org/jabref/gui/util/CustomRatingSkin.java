package org.jabref.gui.util;

import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;

import impl.org.controlsfx.skin.RatingSkin;
import org.controlsfx.control.Rating;

public class CustomRatingSkin extends RatingSkin {
    public CustomRatingSkin(Rating control) {
        super(control);
    }

    @Override
    protected Node createButtonNode() {
        return IconTheme.JabRefIcons.RANKING.getGraphicNode();
    }
}

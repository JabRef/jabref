package org.jabref.gui.util;

import impl.org.controlsfx.skin.RatingSkin;
import javafx.scene.Node;
import org.controlsfx.control.Rating;
import org.jabref.gui.icon.IconTheme;

public class CustomRatingSkin extends RatingSkin {

    public CustomRatingSkin(Rating control) {
        super(control);
        consumeMouseEvents(false);
    }

    @Override
    protected Node createButtonNode() {
        return IconTheme.JabRefIcons.RANKING.getGraphicNode();
    }
}

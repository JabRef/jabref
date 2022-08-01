package org.jabref.gui.util;

import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;

import impl.org.controlsfx.skin.RatingSkin;
import org.controlsfx.control.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRatingSkin extends RatingSkin {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomRatingSkin.class);

    public CustomRatingSkin(Rating control) {
        super(control);

        LOGGER.error("works");
    }

    @Override
    protected Node createButtonNode() {
        return IconTheme.JabRefIcons.RANKING.getGraphicNode();
    }
}

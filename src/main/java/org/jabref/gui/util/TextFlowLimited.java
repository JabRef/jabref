package org.jabref.gui.util;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import org.jabref.logic.l10n.Localization;

public class TextFlowLimited extends TextFlow {
    private boolean isCollapsed = true;
    private Hyperlink moreLink = new Hyperlink(Localization.lang("(more)"));
    private Rectangle clip = new Rectangle();

    public TextFlowLimited(Text... texts) {
        super(texts);

        this.setPrefWidth(Region.USE_PREF_SIZE);

        moreLink.setOnAction(event -> expand());
        moreLink.setStyle("-fx-background-color: white");

        this.setOnMouseClicked(event -> expand());
    }

    private void expand() {
        double newPrefHeight = super.computePrefHeight(getWidth());
        final Animation expandPanel = new Transition() {
            {
                setCycleDuration(Duration.millis(200));
            }

            @Override
            protected void interpolate(double fraction) {
                setPrefHeight(newPrefHeight * fraction);
            }
        };

        expandPanel.setOnFinished(event -> {
            isCollapsed = false;
            requestLayout();
        });
        expandPanel.play();
    }

    @Override
    protected double computePrefHeight(double width) {
        if (isCollapsed) {
            return 38;
        } else {
            return super.computePrefHeight(width);
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (isCollapsed) {
            // Display link to expand text
            if (!this.getChildren().contains(moreLink)) {
                this.getChildren().add(moreLink);
            }
            layoutInArea(moreLink, 0, 0, getWidth(), getHeight(), getBaselineOffset(), HPos.RIGHT, VPos.BOTTOM);

            // Clip content if it expands above pref height (no idea why this is needed, but otherwise sometimes the text is still visible)
            clip.setHeight(computePrefHeight(this.getWidth()));
            clip.setWidth(this.getWidth());
            this.setClip(clip);
        } else {
            this.getChildren().remove(moreLink);
            this.setClip(null);
        }
    }
}

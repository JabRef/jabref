/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * A UI delegate for JLabel that rotates the label 90°
 * <P>
 * Extends {@link BasicLabelUI}.
 * <P>
 * The only difference between the appearance of labels in the Basic and Metal L&Fs is the manner in which diabled text
 * is painted. As VerticalLabelUI does not override the method paintDisabledText, this class can be adapted for Metal
 * L&F by extending MetalLabelUI instead of BasicLabelUI.
 * <P>
 * No other changes are required.
 *
 * @author Darryl
 */
public class VerticalLabelUI extends BasicLabelUI {

    private boolean clockwise;
    // see comment in BasicLabelUI
    private Rectangle verticalViewR = new Rectangle();
    private Rectangle verticalIconR = new Rectangle();
    private Rectangle verticalTextR = new Rectangle();
    private static final VerticalLabelUI verticalLabelUI =
            new VerticalLabelUI();
    private static final VerticalLabelUI SAFE_VERTICAL_LABEL_UI =
            new VerticalLabelUI();


    /**
     * Constructs a <code>VerticalLabelUI</code> with the default anticlockwise
     * rotation
     */
    private VerticalLabelUI() {
    }

    /**
     * Constructs a <code>VerticalLabelUI</code> with the desired rotation.
     * <P>
     * @param clockwise true to rotate clockwise, false for anticlockwise
     */
    public VerticalLabelUI(boolean clockwise) {
        this.clockwise = clockwise;
    }

    /**
     * @see ComponentUI#createUI(javax.swing.JComponent)
     */
    public static ComponentUI createUI(JComponent c) {
        if (System.getSecurityManager() != null) {
            return VerticalLabelUI.SAFE_VERTICAL_LABEL_UI;
        } else {
            return VerticalLabelUI.verticalLabelUI;
        }
    }

    /**
     * Overridden to always return -1, since a vertical label does not have a
     * meaningful baseline.
     *
     * @see ComponentUI#getBaseline(JComponent, int, int)
     */
    @Override
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        return -1;
    }

    /**
     * Overridden to always return Component.BaselineResizeBehavior.OTHER,
     * since a vertical label does not have a meaningful baseline
     *
     * @see ComponentUI#getBaselineResizeBehavior(javax.swing.JComponent)
     */
    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.OTHER;
    }

    /**
     * Transposes the view rectangles as appropriate for a vertical view
     * before invoking the super method and copies them after they have been
     * altered by {@link SwingUtilities#layoutCompoundLabel(FontMetrics, String,
     * Icon, int, int, int, int, Rectangle, Rectangle, Rectangle, int)}
     */
    @Override
    protected String layoutCL(JLabel label, FontMetrics fontMetrics,
            String text, Icon icon, Rectangle viewR, Rectangle iconR,
            Rectangle textR) {

        verticalViewR = transposeRectangle(viewR, verticalViewR);
        verticalIconR = transposeRectangle(iconR, verticalIconR);
        verticalTextR = transposeRectangle(textR, verticalTextR);

        text = super.layoutCL(label, fontMetrics, text, icon,
                verticalViewR, verticalIconR, verticalTextR);

        copyRectangle(verticalViewR, viewR);
        copyRectangle(verticalIconR, iconR);
        copyRectangle(verticalTextR, textR);
        return text;
    }

    /**
     * Transforms the Graphics for vertical rendering and invokes the
     * super method.
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (clockwise) {
            g2.rotate(Math.PI / 2, c.getSize().width / 2, c.getSize().width / 2);
        } else {
            g2.rotate(-Math.PI / 2, c.getSize().height / 2, c.getSize().height / 2);
        }
        super.paint(g2, c);
    }

    /**
     * Returns a Dimension appropriate for vertical rendering
     *
     * @see ComponentUI#getPreferredSize(javax.swing.JComponent)
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        return transposeDimension(super.getPreferredSize(c));
    }

    /**
     * Returns a Dimension appropriate for vertical rendering
     *
     * @see ComponentUI#getMaximumSize(javax.swing.JComponent)
     */
    @Override
    public Dimension getMaximumSize(JComponent c) {
        return transposeDimension(super.getMaximumSize(c));
    }

    /**
     * Returns a Dimension appropriate for vertical rendering
     *
     * @see ComponentUI#getMinimumSize(javax.swing.JComponent)
     */
    @Override
    public Dimension getMinimumSize(JComponent c) {
        return transposeDimension(super.getMinimumSize(c));
    }

    private static Dimension transposeDimension(Dimension from) {
        return new Dimension(from.height, from.width + 2);
    }

    private static Rectangle transposeRectangle(Rectangle from, Rectangle to) {
        if (to == null) {
            to = new Rectangle();
        }
        to.x = from.y;
        to.y = from.x;
        to.width = from.height;
        to.height = from.width;
        return to;
    }

    private static void copyRectangle(Rectangle from, Rectangle to) {
        if (to == null) {
            return;
        }
        to.x = from.x;
        to.y = from.y;
        to.width = from.width;
        to.height = from.height;
    }
}

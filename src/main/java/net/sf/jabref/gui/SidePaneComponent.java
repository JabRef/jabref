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

import org.jdesktop.swingx.JXTitledPanel;
import org.jdesktop.swingx.painter.MattePainter;

import javax.swing.*;
import java.awt.*;

public abstract class SidePaneComponent extends JXTitledPanel {

    protected final JButton close = new JButton(IconTheme.JabRefIcon.CLOSE.getSmallIcon());

    private final SidePaneManager manager;

    protected BasePanel panel;


    public SidePaneComponent(SidePaneManager manager, Icon icon, String title) {
        super(title);
        this.manager = manager;

        this.add(new JLabel(icon));

        setTitleForeground(new Color(79, 95, 143));
        setBorder(BorderFactory.createEmptyBorder());

        close.setMargin(new Insets(0, 0, 0, 0));
        close.setBorder(null);
        close.addActionListener(e -> hideAway());

        JButton up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());
        up.setMargin(new Insets(0, 0, 0, 0));
        up.setBorder(null);
        up.addActionListener(e -> moveUp());

        JButton down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
        down.setMargin(new Insets(0, 0, 0, 0));
        down.setBorder(null);
        down.addActionListener(e -> moveDown());

        JToolBar tlb = new OSXCompatibleToolbar();
        tlb.add(up);
        tlb.add(down);
        tlb.add(close);
        tlb.setOpaque(false);
        tlb.setFloatable(false);
        this.getUI().getTitleBar().add(tlb);
        setTitlePainter(new MattePainter(Color.lightGray));

    }

    private void hideAway() {
        manager.hideComponent(this);
    }

    private void moveUp() {
        manager.moveUp(this);
    }

    private void moveDown() {
        manager.moveDown(this);
    }

    public void setActiveBasePanel(BasePanel panel) {
        this.panel = panel;
    }

    public BasePanel getActiveBasePanel() {
        return panel;
    }

    /**
     * Override this method if the component needs to make any changes before it can close.
     */
    public void componentClosing() {
        // Nothing right now
    }

    /**
     * Override this method if the component needs to do any actions when opening.
     */
    public void componentOpening() {
        // Nothing right now
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}

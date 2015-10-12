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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.*;

import org.jdesktop.swingx.JXTitledPanel;

public abstract class SidePaneComponent extends JXTitledPanel {

    private static final long serialVersionUID = 1L;

    protected final JButton close = new JButton(IconTheme.getImage("close"));

    private boolean visible;

    private final SidePaneManager manager;

    protected BasePanel panel;


    public SidePaneComponent(SidePaneManager manager, Icon icon, String title) {
        super(title);
        this.add(new JLabel(icon));
        this.manager = manager;
        JToolBar tlb = new JToolBar();
        close.setMargin(new Insets(0, 0, 0, 0));
        close.setBorder(null);
        JButton up = new JButton(IconTheme.getImage("up"));
        up.setMargin(new Insets(0, 0, 0, 0));
        JButton down = new JButton(IconTheme.getImage("down"));
        down.setMargin(new Insets(0, 0, 0, 0));
        up.setBorder(null);
        down.setBorder(null);
        up.addActionListener(new UpButtonListener());
        down.addActionListener(new DownButtonListener());
        tlb.setFloatable(false);
        tlb.add(up);
        tlb.add(down);
        tlb.add(close);
        close.addActionListener(new CloseButtonListener());
        this.getUI().getTitleBar().add(tlb);
        setBorder(BorderFactory.createEmptyBorder());
    }

    void hideAway() {
        manager.hideComponent(this);
    }

    private void moveUp() {
        manager.moveUp(this);
    }

    private void moveDown() {
        manager.moveDown(this);
    }

    /**
     * Used by SidePaneManager only, to keep track of visibility.
     * 
     */
    void setVisibility(boolean vis) {
        visible = vis;
    }

    /**
     * Used by SidePaneManager only, to keep track of visibility.
     * 
     */
    boolean hasVisibility() {
        return visible;
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

    }

    /**
     * Override this method if the component needs to do any actions when opening.
     */
    public void componentOpening() {

    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }


    private class CloseButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            hideAway();
        }
    }

    private class UpButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            moveUp();
        }
    }

    private class DownButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            moveDown();
        }
    }
}

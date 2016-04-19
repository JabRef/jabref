/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The side pane is displayed at the left side of JabRef and shows instances of
 * SidePaneComponents, for instance the GroupSelector, or the SearchManager.
 */
public class SidePane extends JPanel {

    private final Dimension PREFERRED_SIZE = new Dimension(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION, 100);

    private final GridBagLayout gridBagLayout = new GridBagLayout();

    private final GridBagConstraints constraint = new GridBagConstraints();

    private final JPanel mainPanel = new JPanel();


    public SidePane() {
        // For debugging the border:
        // setBorder(BorderFactory.createLineBorder(Color.BLUE));

        setLayout(new BorderLayout());
        mainPanel.setLayout(gridBagLayout);

        // Initialize constraint
        constraint.anchor = GridBagConstraints.NORTH;
        constraint.fill = GridBagConstraints.BOTH;
        constraint.gridwidth = GridBagConstraints.REMAINDER;
        constraint.insets = new Insets(1, 1, 1, 1);
        constraint.gridheight = 1;
        constraint.weightx = 1;

        /*
         * Added Scrollpane to fix: 
         */
        JScrollPane sp = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(null);

        // To remove the scroll panel just change sp to mainPanel and comment
        // the JScrollPane declaration
        super.add(sp);
    }

    public void setComponents(Collection<SidePaneComponent> comps) {
        mainPanel.removeAll();

        int totalWeights = 0;
        for (SidePaneComponent c : comps) {
            constraint.weighty = c.getRescalingWeight();
            totalWeights += c.getRescalingWeight();
            gridBagLayout.setConstraints(c, constraint);
            mainPanel.add(c);
        }
        if(totalWeights <= 0) {
            // Fill vertical space so that components start at top
            constraint.weighty = 1;
            Component bx = Box.createVerticalGlue();
            gridBagLayout.setConstraints(bx, constraint);
            mainPanel.add(bx);
        }

        revalidate();
        repaint();
    }

    @Override
    public void remove(Component c) {
        mainPanel.remove(c);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return PREFERRED_SIZE;
    }
}

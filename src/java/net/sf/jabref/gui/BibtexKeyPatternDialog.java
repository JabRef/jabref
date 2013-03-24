/*  Copyright (C) 2012 JabRef contributors.
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.labelPattern.LabelPattern;
import net.sf.jabref.labelPattern.LabelPatternPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class BibtexKeyPatternDialog extends JDialog {
	private MetaData metaData;
	private BasePanel panel;
	private LabelPatternPanel labelPatternPanel;

	public BibtexKeyPatternDialog(JabRefFrame parent, BasePanel panel) {
		super(parent, Globals.lang("Bibtex key patterns"), true);
        this.labelPatternPanel= new LabelPatternPanel(parent.helpDiag);
		setPanel(panel);
		init();
	}

	/**
	 * Used for updating an existing Dialog
	 * 
	 * @param panel the panel to read the data from
	 */
	public void setPanel(BasePanel panel) {
		this.panel = panel;
		this.metaData = panel.metaData();
        LabelPattern keypatterns = metaData.getLabelPattern();
        labelPatternPanel.setValues(keypatterns);
	}

    private final void init() {
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(labelPatternPanel, BorderLayout.CENTER);

		JButton ok = new JButton(Globals.lang("Ok"));
        JButton cancel = new JButton(); // label of "cancel" is set later as the label is overwritten by assigning an action to the button
		
		JPanel lower = new JPanel();
		lower.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		ButtonBarBuilder bb = new ButtonBarBuilder(lower);
		bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
		bb.addGlue();
		
		getContentPane().add(lower, BorderLayout.SOUTH);
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getContentPane().setPreferredSize(new Dimension(500, 600));
		pack();

		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    metaData.setLabelPattern(labelPatternPanel.getLabelPattern());
			    panel.markNonUndoableBaseChanged();
				dispose();
			}
		});

		final JDialog dialog = this;
		
        Action cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            }
        };
        cancel.setAction(cancelAction);
        cancel.setText(Globals.lang("Cancel"));
        
        Util.bindCloseDialogKeyToCancelAction(this.getRootPane(), cancelAction);
	}

	public void setVisible(boolean visible) {
		if (visible)
			super.setVisible(visible);
	}

}

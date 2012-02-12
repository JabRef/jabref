package net.sf.jabref.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

/**
 * Creates a DropDown Button for the toolbar to set EntryPriority 
 */
public class PriorityDropDownButton implements ActionListener {

	private JButton button;

	/**
	 * Creates a Button and adds a DropdownMenu
	 * @param frame Parent Frame
	 */
	public PriorityDropDownButton(JabRefFrame frame) {
		Dimension buttonDim = new Dimension(23,23);
		JPopupMenu popup = new JPopupMenu();
		popup = null;
		JButton button = new JButton();
		button.addActionListener(new MenuButtonActionListener(frame, popup,
				buttonDim));
		button.setIcon(new ImageIcon(GUIGlobals.getIconUrl("priority")));
		button.setToolTipText(Globals.lang("Priority"));
		button.setPreferredSize(buttonDim);
		if (!Globals.ON_MAC)
			button.setMargin(new Insets(1, 0, 2, 0));
		button.setBorder(null);
		button.setBorderPainted(false);
		button.setRolloverEnabled(true);
		button.setOpaque(false);
		button.setBounds(0, 0, buttonDim.width, buttonDim.height);
		button.setSize(buttonDim);
		button.setMinimumSize(buttonDim);
		button.setMaximumSize(buttonDim);
		button.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
		this.button = button;
	}

	/**
	 * Returns the Button-Component
	 * @return Button-Component
	 */
	public Component getComponent() {
		return button;
	}

	/**
	 * Popup Menu Item Class
	 */
	private class MenuButtonActionListener implements ActionListener {

		JPopupMenu popup;
		Dimension dim;
		JabRefFrame frame;
		
		/**
		 * Creates a PopupMenuItem
		 * @param frame Parent Frame
		 * @param popup PopupMenu
		 * @param dim Dimension of Button
		 */
		public MenuButtonActionListener(JabRefFrame frame, JPopupMenu popup,
				Dimension dim) {
			this.popup = popup;
			this.dim = dim;
			this.frame = frame;
		}

		public void actionPerformed(ActionEvent e) {
			if (popup == null) {
				popup = new JPopupMenu();
				// Add Reset Button
				JMenuItem item = new JMenuItem();
				item.setText(Globals.lang("Reset Priority"));
				item.setToolTipText(Globals.lang("Reset Priority"));
				item.addActionListener(new PopupitemActionListener(frame, 0));
				item.setMargin(new Insets(0,0,0,0));
				popup.add(item);
				// Add Priority Set Buttons
				for (int i = 1; i <= 3; i++) {
					item = new JMenuItem(new ImageIcon(GUIGlobals.getIconUrl(GUIGlobals.getIconString(i))));
					item.setText(Globals.lang("Set Priority to") + " " + GUIGlobals.getPrioString(i));
					item.setToolTipText(Globals.lang("Set Priority to") + " " + GUIGlobals.getPrioString(i));
					item.addActionListener(new PopupitemActionListener(frame, i));
					item.setMargin(new Insets(0,0,0,0));
					popup.add(item);
				}
			}
			popup.show(button, 0, dim.height);
		}

		/**
		 * ActionListener for PopupMenuItems
		 */
		private class PopupitemActionListener implements ActionListener {

			private int i;
			private JabRefFrame frame;

			/**
			 * Create a Listener for a PopupMenuItem
			 * @param frame Parent Frame
			 * @param i Priority Number
			 */
			public PopupitemActionListener(JabRefFrame frame, int i) {
				this.i = i;
				this.frame = frame;
			}
			
			/**
			 * Process Action when clicked
			 */
			public void actionPerformed(ActionEvent e) {
				// Set Ranking
				frame.basePanel().runCommand("setPriority" + i);
				popup.show(false);
				popup = null;
			}

		}

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
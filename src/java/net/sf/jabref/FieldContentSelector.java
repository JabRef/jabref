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
package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.*;

import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.looks.Options;

/**
 * A combo-box and a manage button that will add selected strings to an
 * associated entry editor.
 * 
 * Used to manage keywords and authors for instance.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FieldContentSelector extends JComponent {

	JComboBox comboBox;

	FieldEditor editor;

	MetaData metaData;

	JabRefFrame frame;

	Window owner;

	BasePanel panel;

    private AbstractAction action;
    String delimiter;

	/**
	 * 
	 * Create a new FieldContentSelector.
	 * 
	 * @param frame
	 *            The one JabRef-Frame.
	 * @param panel
	 *            The basepanel the entry-editor is on.
	 * @param owner
	 *            The window/frame/dialog which should be the owner of the
	 *            content selector dialog.
	 * @param editor
	 *            The entry editor which will be appended by the text selected
	 *            by the user from the combobox.
	 * @param metaData
	 *            The metadata that contains the list of items to display in the
	 *            combobox under the key Globals.SELECTOR_META_PREFIX +
	 *            editor.getFieldName().
	 * @param action
	 *            The action that will be performed to after an item from the
	 *            combobox has been appended to the text in the entryeditor.
	 * @param horizontalLayout
	 *            Whether to put a 2 pixel horizontal strut between combobox and
	 *            button.
	 */
	public FieldContentSelector(JabRefFrame frame, final BasePanel panel,
		Window owner, final FieldEditor editor, final MetaData metaData,
		final AbstractAction action, boolean horizontalLayout, String delimiter) {

		this.frame = frame;
		this.editor = editor;
		this.metaData = metaData;
		this.panel = panel;
		this.owner = owner;
        this.action = action;
        this.delimiter = delimiter;

		comboBox = new JComboBox() {
			public Dimension getPreferredSize() {
				Dimension parents = super.getPreferredSize();
				if (parents.width > GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH)
					parents.width = GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH;
				return parents;
			}
		};

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints con = new GridBagConstraints();

		setLayout(gbl);

		// comboBox.setEditable(true);

		comboBox.setMaximumRowCount(35);

		// Set the width of the popup independent of the size of th box itself:
		comboBox.putClientProperty(Options.COMBO_POPUP_PROTOTYPE_DISPLAY_VALUE_KEY,
			"The longest text in the combo popup menu. And even longer.");

		rebuildComboBox();

		con.gridwidth = horizontalLayout ? 3 : GridBagConstraints.REMAINDER;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		gbl.setConstraints(comboBox, con);

		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/*
				 * These conditions signify arrow key navigation in the dropdown
				 * list, so we should not react to it. I'm not sure if this is
				 * well defined enough to be guaranteed to work everywhere.
				 */
				if (e.getActionCommand().equals("comboBoxChanged") && (e.getModifiers() == 0))
					return;

				selectionMade();
			}
		});
        // Add an action for the Enter key that signals a selection:
        comboBox.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
        comboBox.getActionMap().put("enter", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                selectionMade();
                comboBox.setPopupVisible(false);
            }
        });

        add(comboBox);

		if (horizontalLayout)
			add(Box.createHorizontalStrut(Sizes.dialogUnitXAsPixel(2, this)));

		JButton manage = new JButton(Globals.lang("Manage"));
		gbl.setConstraints(manage, con);
		add(manage);

		manage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ContentSelectorDialog2 csd = new ContentSelectorDialog2(FieldContentSelector.this.owner, FieldContentSelector.this.frame, panel, true, metaData, editor.getFieldName());
				Util.placeDialog(csd, FieldContentSelector.this.frame);

				// Calling setVisible(true) will open the modal dialog and block
				// for the dialog to close.
				csd.setVisible(true);

				// So we need to rebuild the ComboBox afterwards
				rebuildComboBox();
			}
		});
	}

    private void selectionMade() {
        // The first element is only for show.
        // CO: Why?
        if (comboBox.getSelectedIndex() == 0)
            return;

        String chosen = (String) comboBox.getSelectedItem();
        if (chosen == null || chosen.equals(""))
            return;

        // The following is not possible at the moment since the
        // combobox cannot be edited!

        // User edited in a new word. Add it.
        // if (comboBox.getSelectedIndex() == -1)
        // addWord(chosen);

        // TODO: could improve checking as not do add the same item twice
        if (!editor.getText().equals(""))
            editor.append(FieldContentSelector.this.delimiter);

        editor.append(chosen);

        comboBox.setSelectedIndex(0);

        // Fire event that we changed the editor
        if (action != null)
            action.actionPerformed(new ActionEvent(editor, 0, ""));

        // Transfer focus to the editor.
        editor.requestFocus();
    }

    void rebuildComboBox() {
		comboBox.removeAllItems();

		// TODO: CO - What for?
		comboBox.addItem("");
		Vector<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + editor.getFieldName());
		if (items != null) {
			Iterator<String> i = items.iterator();
			while (i.hasNext())
				comboBox.addItem(i.next());
		}
	}

	// Not used since the comboBox is not editable

	//	/**
	//	 * Adds a word to the selector (to the JList and to the MetaData), unless it
	//	 * is already there
	//	 * 
	//	 * @param newWord
	//	 *            String Word to add
	//	 */
	//	public void addWord(String newWord) {
	//
	//		Vector items = metaData.getData(Globals.SELECTOR_META_PREFIX + editor.getFieldName());
	//		boolean exists = false;
	//		int pos = -1;
	//		for (int i = 0; i < items.size(); i++) {
	//			String s = (String) items.elementAt(i);
	//			if (s.equals(newWord)) {
	//				exists = true;
	//				break;
	//			}
	//			if (s.toLowerCase().compareTo(newWord.toLowerCase()) < 0)
	//				pos = i + 1;
	//		}
	//		if (!exists) {
	//			items.add(Math.max(0, pos), newWord);
	//			// TODO CO: Why is this non-undoable?
	//			panel.markNonUndoableBaseChanged();
	//			panel.updateAllContentSelectors();
	//		}
	//	}
}

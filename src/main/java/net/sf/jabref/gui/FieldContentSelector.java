/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.looks.Options;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.l10n.Localization;

/**
 * A combo-box and a manage button that will add selected strings to an
 * associated entry editor.
 *
 * Used to manage keywords and authors for instance.
 */
public class FieldContentSelector extends JComponent {

    private final JComboBox<String> comboBox;

    private final FieldEditor editor;

    private final MetaData metaData;

    private final AbstractAction action;
    private final String delimiter;


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


        this.editor = editor;
        this.metaData = metaData;
        this.action = action;
        this.delimiter = delimiter;

        comboBox = new JComboBox<String>() {

            @Override
            public Dimension getPreferredSize() {
                Dimension parents = super.getPreferredSize();
                if (parents.width > GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH) {
                    parents.width = GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH;
                }
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

        comboBox.addActionListener(e -> {
            /*
             * These conditions signify arrow key navigation in the dropdown
             * list, so we should not react to it. I'm not sure if this is
             * well defined enough to be guaranteed to work everywhere.
             */
            if ("comboBoxChanged".equals(e.getActionCommand()) && (e.getModifiers() == 0)) {
                return;
            }

            selectionMade();
        });
        // Add an action for the Enter key that signals a selection:
        comboBox.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
        comboBox.getActionMap().put("enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                selectionMade();
                comboBox.setPopupVisible(false);
            }
        });

        add(comboBox);

        if (horizontalLayout) {
            add(Box.createHorizontalStrut(Sizes.dialogUnitXAsPixel(2, this)));
        }

        JButton manage = new JButton(Localization.lang("Manage"));
        gbl.setConstraints(manage, con);
        add(manage);

        manage.addActionListener(e -> {
            ContentSelectorDialog2 csd = new ContentSelectorDialog2(owner, frame, panel, true, metaData,
                    editor.getFieldName());
            csd.setLocationRelativeTo(frame);

            // Calling setVisible(true) will open the modal dialog and block
            // for the dialog to close.
            csd.setVisible(true);

            // So we need to rebuild the ComboBox afterwards
            rebuildComboBox();
        });
    }

    private void selectionMade() {
        // The first element is only for show.
        // CO: Why?
        if (comboBox.getSelectedIndex() == 0) {
            return;
        }

        String chosen = (String) comboBox.getSelectedItem();
        if ((chosen == null) || chosen.isEmpty()) {
            return;
        }

        // The following is not possible at the moment since the
        // combobox cannot be edited!

        // User edited in a new word. Add it.
        // if (comboBox.getSelectedIndex() == -1)
        // addWord(chosen);

        // TODO: could improve checking as not do add the same item twice
        if (!"".equals(editor.getText())) {
            editor.append(FieldContentSelector.this.delimiter);
        }

        editor.append(chosen);

        comboBox.setSelectedIndex(0);

        // Fire event that we changed the editor
        if (action != null) {
            action.actionPerformed(new ActionEvent(editor, 0, ""));
        }

        // Transfer focus to the editor.
        editor.requestFocus();
    }

    public void rebuildComboBox() {
        comboBox.removeAllItems();

        // TODO: CO - What for?
        comboBox.addItem("");
        List<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + editor.getFieldName());
        if (items != null) {
            for (String item : items) {
                comboBox.addItem(item);
            }
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

/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import com.jgoodies.forms.layout.Sizes;

public class FieldContentSelector extends JComponent implements ActionListener {

    protected final String DELIMITER = " ", DELIMITER_2 = "";
    protected final FieldEditor m_editor;
    JComboBox list = new JComboBox() {
        public Dimension getPreferredSize() {
            Dimension parents = super.getPreferredSize();
            if (parents.width > GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH)
                parents.width = GUIGlobals.MAX_CONTENT_SELECTOR_WIDTH;
            return parents;
        }
    };
    JButton manage;
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    protected final MetaData m_metaData;
    protected final JabRefFrame m_frame;
    protected final Window m_owner; 
    protected final BasePanel m_panel;
    protected final AbstractAction m_action;
    protected final boolean m_horizontalLayout;

    /**
     * @param action
     *            The action that will be performed to conclude content
     *            insertion.
     */
    public FieldContentSelector(JabRefFrame frame, BasePanel panel,
            Dialog owner, FieldEditor editor, MetaData data,
            AbstractAction action, boolean horizontalLayout) {
        m_editor = editor;
        m_metaData = data;
        m_action = action;
        m_frame = frame;
        m_panel = panel;
        m_owner = owner;
        m_horizontalLayout = horizontalLayout;
        doInit();
    }

    /**
     * @param action
     *            The action that will be performed to conclude content
     *            insertion.
     */
    public FieldContentSelector(JabRefFrame frame, BasePanel panel,
            Frame owner, FieldEditor editor, MetaData data,
            AbstractAction action, boolean horizontalLayout) {
        m_editor = editor;
        m_metaData = data;
        m_action = action;
        m_frame = frame;
        m_panel = panel;
        m_owner = owner;
        m_horizontalLayout = horizontalLayout;
        doInit();
    }

    private void doInit() {
        setLayout(gbl);
        list.setEditable(true);
        list.setMaximumRowCount(35);

        /*
         * list.getInputMap().put(Globals.prefs.getKey("Select value"),
         * "enter"); list.getActionMap().put("enter", new EnterAction());
         * System.out.println(Globals.prefs.getKey("Select value"));
         */
        updateList();
        // else
        // list = new JComboBox(items.toArray());
        con.gridwidth = m_horizontalLayout ? 3 : GridBagConstraints.REMAINDER;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1;
        // list.setPreferredSize(new Dimension(140,
        // list.getPreferredSize().height));
        gbl.setConstraints(list, con);
        list.addActionListener(this);

        add(list);
        
        if (m_horizontalLayout)
            add(Box.createHorizontalStrut(Sizes.dialogUnitXAsPixel(2,this)));

        manage = new JButton(Globals.lang("Manage"));
        gbl.setConstraints(manage, con);
        add(manage);

        manage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // m_owner is either a Frame or a Dialog
                ContentSelectorDialog2 csd = m_owner instanceof Frame ? 
                        new ContentSelectorDialog2(
                        (Frame) m_owner, m_frame, m_panel, true, m_metaData,
                        m_editor.getFieldName())
                        : new ContentSelectorDialog2((Dialog) m_owner, m_frame,
                                m_panel, true, m_metaData, m_editor
                                        .getFieldName());
                Util.placeDialog(csd, m_frame);
                csd.show();
                updateList();
            }
        });
    }

    public void updateList() {
        list.removeAllItems();
        list.addItem(""); // (Globals.lang(""));
        Vector items = m_metaData.getData(Globals.SELECTOR_META_PREFIX
                + m_editor.getFieldName());
        if ((items != null) && (items.size() > 0)) {
            for (int i = 0; i < items.size(); i++)
                list.addItem(items.elementAt(i));
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("comboBoxChanged")
                && (e.getModifiers() == 0))
            // These conditions signify arrow key navigation in the dropdown
            // list, so we should
            // not react to it. I'm not sure if this is well defined enough to
            // be guaranteed to work
            // everywhere.
            return;

        if (list.getSelectedIndex() == 0)
            return; // The first element is only for show.

        String chosen = (String) list.getSelectedItem();
        // System.out.println(list.getSelectedIndex()+"\t"+chosen);
        if (chosen == null)
            return;
        if (list.getSelectedIndex() == -1) { // User edited in a new word.
            // Add this.
            addWord(chosen);
            /*
             * Vector items = metaData.getData(Globals.SELECTOR_META_PREFIX+
             * editor.getFieldName()); boolean exists = false; int pos = -1; for
             * (int i=0; i<items.size(); i++) { String s =
             * (String)items.elementAt(i); if (s.equals(chosen)) { exists =
             * true; break; } if
             * (s.toLowerCase().compareTo(chosen.toLowerCase()) < 0) pos = i+1; }
             * if (!exists) { items.add(Math.max(0, pos), chosen);
             * parent.panel.markNonUndoableBaseChanged(); updateList(); }
             */
        }
        if (!m_editor.getText().equals(""))
            m_editor.append(DELIMITER);
        m_editor.append(chosen + DELIMITER_2);
        list.setSelectedIndex(0);
        if (m_action != null)
            m_action.actionPerformed(new ActionEvent(m_editor, 0, ""));

        // Transfer focus to the editor.
        m_editor.requestFocus();
        // new FocusRequester(editor.getTextComponent());
    }

    /**
     * Adds a word to the selector (to the JList and to the MetaData), unless it
     * is already there
     * 
     * @param newWord
     *            String Word to add
     */
    public void addWord(String newWord) {

        Vector items = m_metaData.getData(Globals.SELECTOR_META_PREFIX
                + m_editor.getFieldName());
        boolean exists = false;
        int pos = -1;
        for (int i = 0; i < items.size(); i++) {
            String s = (String) items.elementAt(i);
            if (s.equals(newWord)) {
                exists = true;
                break;
            }
            if (s.toLowerCase().compareTo(newWord.toLowerCase()) < 0)
                pos = i + 1;
        }
        if (!exists) {
            items.add(Math.max(0, pos), newWord);
            m_panel.markNonUndoableBaseChanged();
            m_panel.updateAllContentSelectors();
            // updateList();
        }

    }

    /*
     * class EnterAction extends AbstractAction { public void
     * actionPerformed(ActionEvent e) { System.out.println("enter");
     * ths.actionPerformed(e); } }
     */

    /*
     * public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
     * System.out.println("visible"); } public void
     * popupMenuWillBecomeInvisible(PopupMenuEvent e) {
     * System.out.println("invisible"); } public void
     * popupMenuCanceled(PopupMenuEvent e) { System.out.println("canceled"); }
     */
}

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

import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;


class FieldContentSelector extends JComponent implements ActionListener {

    final String DELIMITER = " ", DELIMITER_2 = "";
    FieldEditor editor;
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
    EntryEditor parent;
    MetaData metaData;
    FieldContentSelector ths = this;
    
    public FieldContentSelector(EntryEditor parent,
				FieldEditor editor_, MetaData data) {
	setLayout(gbl);
	this.editor = editor_;
	this.parent = parent;
	metaData = data;
	list.setEditable(true);
	list.setMaximumRowCount(35);
        
        /*list.getInputMap().put(Globals.prefs.getKey("Select value"), "enter");
        list.getActionMap().put("enter", new EnterAction());
        System.out.println(Globals.prefs.getKey("Select value"));*/
	final MetaData metaData = data;
        final JabRefFrame frame = parent.frame;
	final BasePanel panel = parent.panel;
	updateList();
	//else
	//    list = new JComboBox(items.toArray());
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1;
        //list.setPreferredSize(new Dimension(140, list.getPreferredSize().height));
	gbl.setConstraints(list, con);
	list.addActionListener(this);
        
	add(list);

	manage = new JButton(Globals.lang("Manage"));
	gbl.setConstraints(manage, con);
	add(manage);

        manage.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
	      ContentSelectorDialog2 csd = new ContentSelectorDialog2
		  (frame, panel, false, metaData, editor.getFieldName());
	      Util.placeDialog(csd, frame);
	      csd.show();
	      //updateList();
          }
        });
    }

    public void updateList() {
	list.removeAllItems();
	list.addItem(""); //(Globals.lang(""));
	Vector items = metaData.getData(Globals.SELECTOR_META_PREFIX+
				    editor.getFieldName());
	if ((items != null) && (items.size() > 0)) {
	    for (int i=0; i<items.size(); i++)
		list.addItem(items.elementAt(i));
	    //list = new JComboBox();
	}
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("comboBoxChanged") && (e.getModifiers() == 0))
                // These conditions signify arrow key navigation in the dropdown list, so we should
                // not react to it. I'm not sure if this is well defined enough to be guaranteed to work
                // everywhere.
                return;
        
	if (list.getSelectedIndex() == 0)
	    return; // The first element is only for show.
        
	String chosen = (String)list.getSelectedItem();
        //System.out.println(list.getSelectedIndex()+"\t"+chosen);
        if (chosen == null)
            return;
	if (list.getSelectedIndex() == -1) {  // User edited in a new word. Add this.
          addWord(chosen);
	 /*   Vector items = metaData.getData(Globals.SELECTOR_META_PREFIX+
					    editor.getFieldName());
	    boolean exists = false;
	    int pos = -1;
	    for (int i=0; i<items.size(); i++) {
		String s = (String)items.elementAt(i);
		if (s.equals(chosen)) {
		    exists = true;
		    break;
		}
		if (s.toLowerCase().compareTo(chosen.toLowerCase()) < 0)
		    pos = i+1;
	    }
	    if (!exists) {
		items.add(Math.max(0, pos), chosen);
		parent.panel.markNonUndoableBaseChanged();
		updateList();
	    }*/
	}
	if (!editor.getText().equals(""))
	    editor.append(DELIMITER);
	editor.append(chosen+DELIMITER_2);
	list.setSelectedIndex(0);
	parent.storeFieldAction.actionPerformed
	    (new ActionEvent(editor, 0, ""));

	// Transfer focus to the editor.
	editor.requestFocus();
	//new FocusRequester(editor.getTextComponent());
    }

  /**
   * Adds a word to the selector (to the JList and to the MetaData), unless it
   * is already there
   *
   * @param newWord String Word to add
   */
  public void addWord(String newWord) {

      Vector items = metaData.getData(Globals.SELECTOR_META_PREFIX+
                                      editor.getFieldName());
      boolean exists = false;
      int pos = -1;
      for (int i=0; i<items.size(); i++) {
        String s = (String)items.elementAt(i);
        if (s.equals(newWord)) {
          exists = true;
          break;
        }
        if (s.toLowerCase().compareTo(newWord.toLowerCase()) < 0)
          pos = i+1;
      }
      if (!exists) {
        items.add(Math.max(0, pos), newWord);
        parent.panel.markNonUndoableBaseChanged();
	parent.panel.updateAllContentSelectors();
        //updateList();
      }

    }
  
  /*class EnterAction extends AbstractAction {
      public void actionPerformed(ActionEvent e) {
          System.out.println("enter");
          ths.actionPerformed(e);
      }
  }*/
  
  /*public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      System.out.println("visible");
  }
  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      System.out.println("invisible");
  }
  public void popupMenuCanceled(PopupMenuEvent e) {
      System.out.println("canceled");
  }*/
}

/*
Copyright (C) 2003 Morten O.Alver & Nizar N. Batada

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

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
// 
class KeyBindingsDialog extends JDialog
{
    JList list=new JList();
    JTextField keyTF=new JTextField();
    JButton ok,cancel,grabB,defB;
    HashMap bindHM, defBinds;
    boolean clickedSave=false;
    boolean getAction(){return clickedSave;}
    HashMap getNewKeyBindings(){
	return bindHM;
    }
    public KeyBindingsDialog(HashMap name2binding, HashMap defBinds)
    {
	super();
	this.defBinds = defBinds;
	setTitle("JabRef Key Binding:");
	setModal(true);//this needs to be modal so that client knows when ok or cancel was clicked
	getContentPane().setLayout(new BorderLayout());
	bindHM=name2binding;
	JScrollPane listScroller = new JScrollPane(list);
	listScroller.setPreferredSize(new Dimension(250, 400));
	getContentPane().add( listScroller, BorderLayout.CENTER);

	Box buttonBox=new Box(BoxLayout.X_AXIS);
	ok=new JButton(Globals.lang("Ok"));
	cancel=new JButton(Globals.lang("Cancel"));
	grabB=new JButton(Globals.lang("Grab"));	
	defB=new JButton(Globals.lang("Default"));	
	grabB.addKeyListener(new JBM_CustomKeyBindingsListener());
	buttonBox.add(grabB);
	buttonBox.add(defB);
	buttonBox.add(ok);
	buttonBox.add(cancel);

	getContentPane().add( buttonBox, BorderLayout.SOUTH);
	setTop();
	setButtons();
	setList();
	keyTF.setEditable(false);
    }
    void setTop(){
	Box topBox=new Box(BoxLayout.X_AXIS);

	topBox.add(new JLabel(Globals.lang("Binding")+":",JLabel.RIGHT));
	topBox.add(keyTF);
	getContentPane().add(topBox,BorderLayout.NORTH);

    }
    //##################################################
    // respond to grabKey and display the key binding
    //##################################################
    public class JBM_CustomKeyBindingsListener extends KeyAdapter {
        public void keyPressed(KeyEvent evt) {
	    // first check if anything is selected if not the return
	    Object[] selected = list.getSelectedValues();
	    if(selected.length==0)
		return;
 	    String code=KeyEvent.getKeyText(evt.getKeyCode());
 	    String mod=KeyEvent.getKeyModifiersText(evt.getModifiers());
	    // all key bindings must have a modifier: ctrl alt etc 

	    if(mod.equals("")) {
		int kc = evt.getKeyCode();
		if ((kc < KeyEvent.VK_F1) && (kc > KeyEvent.VK_F12) &&
		    (kc != KeyEvent.VK_ESCAPE) && (kc != KeyEvent.VK_DELETE))
		    return; // need a modifier except for function keys
	    }
	    // second key cannot be a modifiers 
	    //if ( evt.isActionKey()) {
	    //Util.pr(code);
	    if (//code.equals("Escape") 
		    code.equals("Tab") 
		    || code.equals("Backspace") 
		    || code.equals("Enter") 
		    //|| code.equals("Delete")
		    || code.equals("Space") 
		    || code.equals("Ctrl") 
		    || code.equals("Shift") 
		    || code.equals("Alt") )    	
		    return;
		//}
	    String newKey;
	    if (!mod.equals(""))
		newKey = mod.toLowerCase() + " " + code; 
	    else
		newKey = code;
	    keyTF.setText(newKey);
	    //find which key is selected and set its value int the bindHM
	    String selectedFunction=(String)list.getSelectedValue();
	    // log print
	    // System.out.println("selectedfunction " + selectedFunction + " new key: " + newKey);
	    bindHM.put(selectedFunction,newKey);
	}
    }
    //##################################################
    // put the corresponding key binding into keyTF
    //##################################################
    class MyListSelectionListener implements ListSelectionListener {
        // This method is called each time the user changes the set of selected items
        public void valueChanged(ListSelectionEvent evt) {
            // When the user release the mouse button and completes the selection,
            // getValueIsAdjusting() becomes false
            if (!evt.getValueIsAdjusting()) {
                JList list = (JList)evt.getSource();
		
                // Get all selected items
                Object[] selected = list.getSelectedValues();
		
                // Iterate all selected items
                for (int i=0; i<selected.length; i++) {
                    Object sel = selected[i];
		    keyTF.setText( (String)bindHM.get( sel ));
                }
            }
        }
    }
      class MyListDataListener implements ListDataListener {
	  // This method is called when new items have been added to the list
	  public void intervalAdded(ListDataEvent evt) {
	      DefaultListModel model = (DefaultListModel)evt.getSource();
	      
	      // Get range of new  items
	      int start = evt.getIndex0();
	      int end = evt.getIndex1();
	      int count = end-start+1;
	      
	      // Get new items
	      for (int i=start; i<=end; i++) {
                Object item = model.getElementAt(i);
            }
        }
    
        // This method is called when items have been removed from the list
        public void intervalRemoved(ListDataEvent evt) {
            // Get range of removed items
            int start = evt.getIndex0();
            int end = evt.getIndex1();
            int count = end-start+1;
    
            // The removed items are not available
        }
    
    
        // This method is called when items in the list are replaced
        public void contentsChanged(ListDataEvent evt) {
            DefaultListModel model = (DefaultListModel)evt.getSource();
    
            // Get range of changed items
            int start = evt.getIndex0();
            int end = evt.getIndex1();
            int count = end-start+1;
    
            // Get changed items
            for (int i=start; i<=end; i++) {
                Object item = model.getElementAt(i);
            }
        }
    }  
    //setup so that clicking on list will display the current binding
    void setList(){
	list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	list.getModel().addListDataListener(new MyListDataListener());

	// This method is called each time the user changes the set of selected items
	list.addListSelectionListener(new MyListSelectionListener());
	DefaultListModel listModel = new DefaultListModel();
	TreeMap sorted = new TreeMap(bindHM);
  	Iterator it = sorted.keySet().iterator();
	while(it.hasNext()){
	    listModel.addElement(it.next());
	}
	list.setModel(listModel);
	list.setSelectionInterval(0,0);//select the first entry
    }
    // listners
    void setButtons(){
	ok.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    // save all the key bindings
		    dispose();
		    clickedSave=true;
		    // message: key bindings will take into effect next time you start JBM
		}});
	cancel.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		    clickedSave=false;
		    //System.exit(-1);//get rid of this
		}});
	defB.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Object[] selected = list.getSelectedValues();
		    if(selected.length==0)
			return;
		    keyTF.setText(setToDefault((String)list.getSelectedValue()));
		}});
	
    }

    String setToDefault(String name) 
    {
	String defKey = (String)defBinds.get(name);
	bindHM.put(name, defKey);
	return defKey;
    }

    /*
    public static void main(String args[])
    {
	HashMap h=new HashMap();
	h.put("new-bibtex","ctrl N");
	h.put("edit-bibtex","ctrl E");
	h.put("exit-bibtex","ctrl Q");	
	KeyBindingsDialog d= new KeyBindingsDialog(h);
	d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	d.setSize(200,300);
	d.setVisible(true);
	
	}*/
}

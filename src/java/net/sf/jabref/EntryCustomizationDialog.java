/*
Copyright (C) 2002-2003 Nizar N. Batada nbatada@stanford.edu
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
import java.awt.event.*;
import java.awt.*;

class EntryCustomizationDialog extends JDialog
{
    BibtexEntryType type;

    JButton ok, cancel;
    JPanel typePanel=new JPanel();
    JPanel fieldPanel = new JPanel();
    int width=10;
    JLabel messageLabel=new JLabel("");    

    JTextField name = new JTextField("", width);
    JTextArea req_ta=new JTextArea("",17,width),//10 row, 20 columns
	opt_ta=new JTextArea("",17,width);//10 row, 20 columns
    // need to get FIeld name from somewhere
	
    //JComboBox types_cb = new JComboBox(Globals.typeNames);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
	
    JPanel buttonPanel = new JPanel();
    public EntryCustomizationDialog(JFrame parent)
    {
	//Type=Article, Book etc
	// templateName will be used to put on the dialog frame
	// create 10 default entries
	// return an array
	super(parent,"Customize Form fields",true);
	setSize(230,400);
	initialize();
	makeButtons();
	typePanel.add( new JLabel("TYPE",JLabel.RIGHT));
	//typePanel.add( types_cb);
	fieldPanel.add(name);
	fieldPanel.add(new JLabel("FIELDS",JLabel.RIGHT));
	fieldPanel.add(new JScrollPane
		       (req_ta,			
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

	fieldPanel.add(new JScrollPane
		       (opt_ta,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

	fieldPanel.add(messageLabel);
    }

    public EntryCustomizationDialog(JFrame parent, BibtexEntryType type_) {
	this(parent);
	type = type_;
	
    }

    void initialize(){
	setModal(true);
	setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
	getContentPane().setLayout(new BorderLayout());
	getContentPane().add( buttonPanel, BorderLayout.SOUTH);
	getContentPane().add( typePanel, BorderLayout.NORTH);
	getContentPane().add( fieldPanel, BorderLayout.CENTER);		

	messageLabel.setForeground(Color.black);
	messageLabel.setText("Field names delimiter is semicolon.\n Ex: author;title;journal;");
		
    }

    void save()
    {
			
	String 
	    reqStr = req_ta.getText().replaceAll("\\s+","")
	    .replaceAll("\\n+","").trim(),
	    optStr = opt_ta.getText().replaceAll("\\s+","")
	    .replaceAll("\\n+","").trim();

									 
	String typeName = name.getText().trim();

	if(! typeName.equals("")) {
	    Util.pr("Save entry definition.");
	    CustomEntryType typ = new CustomEntryType
		(typeName, reqStr, optStr);
	    BibtexEntryType.ALL_TYPES.put(typeName, typ);
	}
	else{ 				
	    Util.pr("No name.");
	}
		
    }
    void makeButtons(){
	ok = new JButton(Globals.lang("Ok"));
	cancel=new JButton(Globals.lang("Cancel"));
	buttonPanel.add( ok );
	buttonPanel.add( cancel);
	ok.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    save();
		}
	    });
	cancel.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	    });
		
    }

}

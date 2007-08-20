package net.sf.jabref.wizard.text.gui;

import java.util.Vector;

import javax.swing.DefaultListModel;

import net.sf.jabref.wizard.integrity.IntegrityMessage;

public class HintListModel extends DefaultListModel {
	
	public void setData(Vector<IntegrityMessage> newData) {
		clear();
		if (newData != null) {
			for (IntegrityMessage message : newData){
				addElement(message);
			}
		}
	}

	public void valueUpdated(int index) {
		super.fireContentsChanged(this, index, index);
	}
}

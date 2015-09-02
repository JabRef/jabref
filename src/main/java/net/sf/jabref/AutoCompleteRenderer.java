package net.sf.jabref;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.Action;

public interface AutoCompleteRenderer<E> { 
    boolean updateListData(E[] strings);

	Component init();

	 /**
     * This method will attempt to locate a reasonable autocomplete item
     * from all combo box items and select it. It will also populate the
     * combo box editor with the remaining text which matches the
     * autocomplete item and select it. If the selection changes and the
     * JComboBox is not a Table Cell Editor, an ActionEvent will be
     * broadcast from the combo box.
     */
	void selectAutoCompleteTerm(String text);

	void selectNewItem(int offset);

	E getSelectedItem();

	void registerAcceptAction(ActionListener acceptAction);
}
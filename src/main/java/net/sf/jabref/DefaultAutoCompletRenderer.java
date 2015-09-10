package net.sf.jabref;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class DefaultAutoCompletRenderer<E> implements AutoCompleteRenderer<E> {
	JList<E> list = new JList<E>();
	ActionListener acceptAction;
	Boolean interpretSelectionChangeAsAccept = true;
	
	public boolean updateListData(E[] autoCompletions) {
		if(autoCompletions != null)
        {
        	list.setListData(autoCompletions);
        	int size = list.getModel().getSize();
			//list.setVisibleRowCount(size < 10 ? size : 10);
			if(size == 0)
				return false;
			else
				return true;
        }
		return false;
    }
	
	public Component init() {
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFocusable(false);
		list.setRequestFocusEnabled(false);
		list.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		list.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				if(interpretSelectionChangeAsAccept && acceptAction != null)
					acceptAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setFocusable(false);
		scrollPane.setRequestFocusEnabled(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
		return scrollPane;
	}

	@Override
	public void selectAutoCompleteTerm(String text) {
		interpretSelectionChangeAsAccept = false;
		list.setSelectedIndex(0);	
		list.ensureIndexIsVisible(0);
		interpretSelectionChangeAsAccept = true;
	}

	@Override
	public void selectNewItem(int offset) {
		interpretSelectionChangeAsAccept = false;
		int newIndex = list.getSelectedIndex() + offset;
		
		// Set new index if valid
		if(newIndex >= 0 && newIndex < list.getModel().getSize())
		{
			list.setSelectedIndex(newIndex);
			list.ensureIndexIsVisible(newIndex);
		}		
		interpretSelectionChangeAsAccept = true;
	}



	@Override
	public E getSelectedItem() {
		return list.getSelectedValue();
	}

	@Override
	public void registerAcceptAction(ActionListener acceptAction) {
		this.acceptAction = acceptAction;		
	}
}
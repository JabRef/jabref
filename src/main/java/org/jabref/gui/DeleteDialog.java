package org.jabref.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jabref.model.groups.GroupTreeNode;
import javafx.collections.ObservableList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Set;
import java.awt.event.ActionEvent;

public class DeleteDialog extends JDialog {

	public enum DeleteResult {
		DELETE_FROM_DATABASE,
		REMOVE_FROM_GROUPS,
		CANCEL
	}

	protected DeleteResult result = DeleteResult.CANCEL;


	/**
	 * Create the dialog.
	 * @param selectedGroups 
	 * @param allEnriesGroups 
	 */
	public DeleteDialog(ObservableList<GroupTreeNode> selectedGroups, Set<String> allEnriesGroups) {
		setTitle("Delete");
		setModal(true);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		JPanel panel = new JPanel();
		getContentPane().add(panel);

		String message = "";
		if (selectedGroups.size() > 0) {
			message += "Currently there are selected group(s): <br/><ul>";
			for(int i = 0; i < selectedGroups.size(); i++){
				message += "<li>"+selectedGroups.get(i).getName() + "</li>"; 
			}
			message += "</ul>If you would like to remove selected entities from selected groups <br/>press button \"Remove from groups\" <br/><br/>";
		}
		
		if (!allEnriesGroups.isEmpty()) {
			message += "Selected entries belongs to group(s): <br/><ul>";
			Iterator<String> iterator = allEnriesGroups.iterator();
			while(iterator.hasNext()){
				message += "<li>"+iterator.next()+ "</li>"; 
			}
			message += "</ul>If you delete articles it will be removed from all listed groups <br>";
		}

		JLabel lblThisEntitiesAre = new JLabel("<html> "+ message + " </html>");
		panel.add(lblThisEntitiesAre);

		JPanel buttonPane = new JPanel();
		getContentPane().add(buttonPane);
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

		JButton btnDeleteFromDatabase = new JButton("Delete from database");
		btnDeleteFromDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = DeleteResult.DELETE_FROM_DATABASE;
				dispose();
			}
		});
		buttonPane.add(btnDeleteFromDatabase);

		if (selectedGroups.size() > 0) {
			JButton btnRemoveFromAll = new JButton("Remove from groups");
			btnRemoveFromAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					result = DeleteResult.REMOVE_FROM_GROUPS;
					dispose();
				}
			});
			buttonPane.add(btnRemoveFromAll);
		}

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = DeleteResult.CANCEL;
				dispose();
			}
		});
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
		

		JPanel panel2 = new JPanel();
		getContentPane().add(panel2);
		panel2.setLayout(new BoxLayout(panel2, 0));
		{
			JCheckBox chckbxRememberChoice = new JCheckBox("Remember choice");
			panel2.add(chckbxRememberChoice);
		}
		
		
		// set size
		pack();
		
		// center of screen
		this.setLocationRelativeTo(null);
		
		// dispose on ESC
		getRootPane().registerKeyboardAction(e -> {
		    this.dispose();
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	DeleteResult getResult(){
		return result;
	}

}

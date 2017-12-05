package org.jabref.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;
import javafx.collections.ObservableList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
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
			message += Localization.lang("Currently_there_are_selected_group(s)")+": <br/><ul>";
			for(int i = 0; i < selectedGroups.size(); i++){
				message += "<li>"+selectedGroups.get(i).getName() + "</li>"; 
			}
			message += "</ul>" + Localization.lang("If_you_would_like_to_remove_selected_entities_from_selected_groups_<br/>press_button") 
			+ "\" " 
			+ Localization.lang("Remove_from_groups") 
			+ "\"<br/><br/>";
		}
		
		if (!allEnriesGroups.isEmpty()) {
			message += Localization.lang("Selected_entries_belongs_to_group(s)") + ": <br/><ul>";
			Iterator<String> iterator = allEnriesGroups.iterator();
			while (iterator.hasNext()) {
				message += "<li>" + iterator.next() + "</li>"; 
			}
			message += "</ul>" + Localization.lang("If_you_delete_articles_it_will_be_removed_from_all_listed_groups") + "<br>";
		}

		JLabel lblThisEntitiesAre = new JLabel("<html> "+ message + " </html>");
		panel.add(lblThisEntitiesAre);

		JPanel buttonPane = new JPanel();
		getContentPane().add(buttonPane);
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));

		JButton btnDeleteFromDatabase = new JButton(Localization.lang("Delete_from_database"));
		btnDeleteFromDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = DeleteResult.DELETE_FROM_DATABASE;
				dispose();
			}
		});
		buttonPane.add(btnDeleteFromDatabase);

		if (selectedGroups.size() > 0) {
			JButton btnRemoveFromAll = new JButton(Localization.lang("Remove_from_group(s)"));
			btnRemoveFromAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					result = DeleteResult.REMOVE_FROM_GROUPS;
					dispose();
				}
			});
			buttonPane.add(btnRemoveFromAll);
		}

		JButton cancelButton = new JButton(Localization.lang("Cancel"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = DeleteResult.CANCEL;
				dispose();
			}
		});
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
		

//		JPanel panel2 = new JPanel();
//		getContentPane().add(panel2);
//		panel2.setLayout(new BoxLayout(panel2, 0));
//		{
//			JCheckBox chckbxRememberChoice = new JCheckBox(Localization.lang("Remember_choice"));
//			panel2.add(chckbxRememberChoice);
//		}
		
		
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

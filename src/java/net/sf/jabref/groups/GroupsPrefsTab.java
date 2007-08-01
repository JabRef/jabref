package net.sf.jabref.groups;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.PrefsTab;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

// JZTODO lyrics
public class GroupsPrefsTab extends JPanel implements PrefsTab {
	private final JCheckBox showIcons = new JCheckBox(Globals.lang("Show icons for groups"));
	private final JCheckBox showDynamic = new JCheckBox(
			"<html>"+Globals.lang("Show dynamic groups in <i>italics</i>")+"</html>");
	private final JCheckBox expandTree = new JCheckBox(
			Globals.lang("Initially show groups tree expanded"));
	private final JCheckBox autoShow = new JCheckBox(
			Globals.lang("Automatically show groups interface when switching to a database that contains groups"));
	private final JCheckBox autoHide = new JCheckBox(
			Globals.lang("Automatically hide groups interface when switching to a database that contains no groups"));
	private JTextField groupingField = new JTextField(20);
	private JTextField keywordSeparator = new JTextField(2);

	private final JabRefPreferences prefs;

	public GroupsPrefsTab(JabRefPreferences prefs) {
		this.prefs = prefs;
		
		keywordSeparator.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				keywordSeparator.selectAll();
			}
			public void focusLost(FocusEvent e) {
				// deselection is automatic
			}
		});
		
		FormLayout layout = new FormLayout("9dlu, pref", //500px",
				"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, " +
				"p, 3dlu, p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.appendSeparator(Globals.lang("View"));
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
		builder.append(showIcons);
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
		builder.append(showDynamic);
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
		builder.append(expandTree);
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
		builder.append(autoShow);
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
		builder.append(autoHide);
		builder.nextLine();
		builder.nextLine();
		builder.appendSeparator(Globals.lang("Dynamic groups"));
		builder.nextLine();
		builder.nextLine();
		builder.nextColumn();
			// build subcomponent
			FormLayout layout2 = new FormLayout("left:pref, 2dlu, left:pref", 
					"p, 3dlu, p");
			DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
			builder2.append(new JLabel(Globals.lang("Default grouping field") + ":"));
			builder2.append(groupingField);
			builder2.nextLine();
			builder2.nextLine();
			builder2.append(new JLabel(Globals.lang("When adding/removing keywords, separate them by")+":"));
			builder2.append(keywordSeparator);
		builder.append(builder2.getPanel());

		setLayout(new BorderLayout());
		JPanel panel = builder.getPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.CENTER);
	}

	public void setValues() {
		showIcons.setSelected(prefs.getBoolean("groupShowIcons"));
		showDynamic.setSelected(prefs.getBoolean("groupShowDynamic"));
		expandTree.setSelected(prefs.getBoolean("groupExpandTree"));
		groupingField.setText(prefs.get("groupsDefaultField"));
		autoShow.setSelected(prefs.getBoolean("groupAutoShow"));
		autoHide.setSelected(prefs.getBoolean("groupAutoHide"));
		keywordSeparator.setText(prefs.get("groupKeywordSeparator"));
	}

	public void storeSettings() {
		prefs.putBoolean("groupShowIcons", showIcons.isSelected());
		prefs.putBoolean("groupShowDynamic", showDynamic.isSelected());
		prefs.putBoolean("groupExpandTree", expandTree.isSelected());
		prefs.put("groupsDefaultField", groupingField.getText().trim());
		prefs.putBoolean("groupAutoShow", autoShow.isSelected());
		prefs.putBoolean("groupAutoHide", autoHide.isSelected());
		prefs.put("groupKeywordSeparator", keywordSeparator.getText());
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("Groups");
	}

}

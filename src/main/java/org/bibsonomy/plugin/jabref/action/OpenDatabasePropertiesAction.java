package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.DatabasePropertiesDialog;

public class OpenDatabasePropertiesAction extends AbstractPluginAction{
	
	private static final long serialVersionUID = -5243052886812863636L;
	
	DatabasePropertiesDialog databasePropertiesDialog = null;
	
	public OpenDatabasePropertiesAction(JabRefFrame jabRefFrame) {
		super(jabRefFrame);
	}

	public void actionPerformed(ActionEvent e) {
		if (databasePropertiesDialog == null) {
			databasePropertiesDialog = new DatabasePropertiesDialog(getJabRefFrame());
			databasePropertiesDialog.setPanel(getJabRefFrame().basePanel());
		}
		databasePropertiesDialog.setVisible(true);
	}

}

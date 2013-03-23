package net.sf.jabref.export;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

public class OpenFolder extends AbstractWorker {
	String message = null;
	private JabRefFrame frame;

	public OpenFolder(JabRefFrame frame) {
		this.frame = frame;
	}

	@Override		
	
	public void run() {
		BasePanel panel = frame.basePanel();
		if (panel == null) {
			return;
		}

		if (panel.getSelectedEntries().length == 0) {
			message = Globals.lang("No entries selected") + ".";
			getCallBack().update();
			return;
		}

		for (BibtexEntry entry : panel.getSelectedEntries()) {
			try{
				FileListTableModel tm = new FileListTableModel();
				tm.setContent(entry.getField("file"));
				for (int i = 0; i < tm.getRowCount(); i++) {
					FileListEntry flEntry = tm.getEntry(i);
					FileListEntry ent = flEntry;
					System.out.println(ent.getLink());
					Util.openFolder(ent.getLink());
					break;
				}
			}catch(Exception ex){
				ex.printStackTrace();
				message = ex.getMessage();
			}
		}
	}
}

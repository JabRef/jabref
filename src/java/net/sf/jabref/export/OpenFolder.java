package net.sf.jabref.export;

import java.util.logging.Logger;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

public class OpenFolder extends AbstractWorker {
    private static final Logger logger = Logger.getLogger(OpenFolder.class.getName());

    String message;
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
		
		Integer selectedFilesCount = 0;
		message = null;

		if (panel.getSelectedEntries().length == 0) {
			message = Globals.lang("No entries selected.");
			return;
		} else {
			for (BibtexEntry entry : panel.getSelectedEntries()) {
				try {
					FileListTableModel tm = new FileListTableModel();
					tm.setContent(entry.getField("file"));
					for (int i = 0; i < tm.getRowCount(); i++) {
						FileListEntry flEntry = tm.getEntry(i);
						FileListEntry ent = flEntry;
						logger.finer(ent.getLink());
						Util.openFolderAndSelectFile(ent.getLink());
						selectedFilesCount++;
						break;
					}
				} catch (Exception ex) {
					logger.warning(ex.getMessage());
					message = ex.getMessage();
				}
			}
		}
		if (message == null) {
			// no error occurred, just output the number of opened folders
			message = Globals.lang("Opened %0 folder(s).", selectedFilesCount.toString());
		}
	}
	
	public void update() {
		frame.output(message);
	}
}

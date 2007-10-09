package net.sf.jabref.export;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.HashSet;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import net.sf.jabref.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Dec 12, 2006
 * Time: 6:22:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExportToClipboardAction extends AbstractWorker {
    String message = null;
    private JabRefFrame frame;
    private BibtexDatabase database;

    public ExportToClipboardAction(JabRefFrame frame, BibtexDatabase database) {
        this.frame = frame;
        this.database = database;
    }
    public void run() {
        BasePanel panel = frame.basePanel();
        if (panel == null)
            return;
        if (panel.getSelectedEntries().length == 0) {
            message = Globals.lang("No entries selected") + ".";
            getCallBack().update();
            return;
        }

        Map<String, IExportFormat> m = ExportFormats.getExportFormats();
        IExportFormat[] formats = new ExportFormat[m.size()];
        String[] array = new String[formats.length];
        
        int piv = 0;
		for (IExportFormat format : m.values()) {
			formats[piv] = format;
			array[piv] = format.getDisplayName();
			piv++;
		}
        
        JList list = new JList(array);
        list.setBorder(BorderFactory.createEtchedBorder());
        list.setSelectionInterval(0, 0);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int answer = JOptionPane.showOptionDialog(frame, list, Globals.lang("Select format"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null,
            new String[]{Globals.lang("Ok"), Globals.lang("Cancel")},
            Globals.lang("Ok"));

        if (answer == JOptionPane.NO_OPTION)
            return;

        IExportFormat format = formats[list.getSelectedIndex()];

        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = frame.basePanel().metaData()
                .getFileDirectory(GUIGlobals.FILE_FIELD);
        
        /*final boolean custom = (list.getSelectedIndex() >= Globals.STANDARD_EXPORT_COUNT);
        String dir = null;
        if (custom) {
            int index = list.getSelectedIndex() - Globals.STANDARD_EXPORT_COUNT;
            dir = (String) (Globals.prefs.customExports.getElementAt(index)[1]);
            File f = new File(dir);
            lfName = f.getName();
            lfName = lfName.substring(0, lfName.indexOf("."));
            // Remove file name - we want the directory only.
            dir = f.getParent() + System.getProperty("file.separator");
        }
        final String format = lfName,
                directory = dir;
        */
        File tmp = null;
        Reader reader = null;
        try {
            // To simplify the exporter API we simply do a normal export to a temporary
            // file, and read the contents afterwards:
            tmp = File.createTempFile("jabrefCb", ".tmp");
            tmp.deleteOnExit();
            BibtexEntry[] bes = panel.getSelectedEntries();
            HashSet<String> entries = new HashSet<String>(bes.length);
            for (BibtexEntry be : bes)
                entries.add(be.getId());
            
            // Write to file:
            format.performExport(database, panel.metaData(),
                    tmp.getPath(), panel.getEncoding(), entries);
            // Read the file and put the contents on the clipboard:
            StringBuffer sb = new StringBuffer();
            reader = new InputStreamReader(new FileInputStream(tmp), panel.getEncoding());
            int s;
            while ((s = reader.read()) != -1) {
                sb.append((char)s);
            }
            ClipboardOwner owner = new ClipboardOwner() {
                public void lostOwnership(Clipboard clipboard, Transferable content) {
                }
            };
            //StringSelection ss = new StringSelection(sw.toString());
            RtfSelection rs = new RtfSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(rs, owner);
            message = Globals.lang("Entries exported to clipboard") + ": " + bes.length;

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            message = Globals.lang("Error exporting to clipboard");
            return;
        } finally {
            // Clean up:
            if (tmp != null)
                tmp.delete();
            if (reader != null)
                try { reader.close(); } catch (IOException ex) { ex.printStackTrace(); }
        }

    }

    public void update() {
        frame.output(message);
    }

}

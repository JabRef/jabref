package net.sf.jabref.imports;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 22, 2006
 * Time: 12:06:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImportFormats {

    public static JFileChooser createImportFileChooser(String currentDir) {

        SortedSet<ImportFormat> importers = Globals.importFormatReader.getImportFormats();
        
        String lastUsedFormat = Globals.prefs.get("lastUsedImport");
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        TreeSet<ImportFileFilter> filters = new TreeSet<ImportFileFilter>();
        for (ImportFormat format : importers){
            ImportFileFilter filter = new ImportFileFilter(format);
            filters.add(filter);
            if (format.getFormatName().equals(lastUsedFormat))
                defaultFilter = filter;
        }
        for (ImportFileFilter filter : filters){
            fc.addChoosableFileFilter(filter);
        }

        if (defaultFilter != null)
            fc.setFileFilter(defaultFilter);
        else
            fc.setFileFilter(fc.getAcceptAllFileFilter());
        return fc;
    }

    /**
     * Create an AbstractAction for performing an Import operation.
     * @param frame The JabRefFrame of this JabRef instance.
     * @param openInNew Indicate whether the action should open into a new database or
     *  into the currently open one.
     * @return The action.
     */
    public static AbstractAction getImportAction(JabRefFrame frame, boolean openInNew) {

        class ImportAction extends MnemonicAwareAction {
            private JabRefFrame frame;
            private boolean openInNew;


            public ImportAction(JabRefFrame frame, boolean openInNew) {
                this.frame = frame;
                this.openInNew = openInNew;

                putValue(NAME, openInNew ? "Import into new database" :
                        "Import into current database");
                putValue(ACCELERATOR_KEY, openInNew ? Globals.prefs.getKey("Import into new database") :
                        Globals.prefs.getKey("Import into current database"));
            }

            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = ImportFormats.createImportFileChooser
                        (Globals.prefs.get("importWorkingDirectory"));
                fc.showOpenDialog(frame);
                File file = fc.getSelectedFile();
                if (file == null)
                    return;
                FileFilter ff = fc.getFileFilter();
                ImportFormat format = null;
                if (ff instanceof ImportFileFilter)
                    format = ((ImportFileFilter)ff).getImportFormat();

                try {
                    if (!file.exists()) {
                        // Warn that the file doesn't exists:
                        JOptionPane.showMessageDialog(frame,
                                Globals.lang("File not found")+
                                ": '"+file.getName()+"'.",
                                Globals.lang("Import"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ImportMenuItem imi = new ImportMenuItem(frame,
                            openInNew, format);
                    imi.automatedImport(new String[] {file.getAbsolutePath()});


                    // Make sure we remember which filter was used, to set the default
                    // for next time:
                    if (format != null)
                        Globals.prefs.put("lastUsedImport", format.getFormatName());
                    else
                        Globals.prefs.put("lastUsedImport", "__all");
                    Globals.prefs.put("importWorkingDirectory", file.getParent());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }

        return new ImportAction(frame, openInNew);
    }
}

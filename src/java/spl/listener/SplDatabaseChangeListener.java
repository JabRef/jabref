package spl.listener;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.gui.MainTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 10.09.2010
 * Time: 14:02:55
 * To change this template use File | Settings | File Templates.
 */
public class SplDatabaseChangeListener implements DatabaseChangeListener {

    private JabRefFrame frame;
    private BasePanel panel;
    private MainTable entryTable;
    private String fileName;

    public SplDatabaseChangeListener(JabRefFrame frame, BasePanel panel, MainTable entryTable, String fileName) {
        this.frame = frame;
        this.panel = panel;
        this.entryTable = entryTable;
        this.fileName = fileName;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        if(e.getType() == DatabaseChangeEvent.ChangeType.ADDED_ENTRY){
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, entryTable, e.getEntry());
            panel.database().removeDatabaseChangeListener(this);
        }
    }
}

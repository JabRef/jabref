package org.jabref.gui.entryeditor;

import javax.swing.JTabbedPane;

public class EntryEditorTabbedPane extends JTabbedPane {

    private FileAnnotationTab pdfTab;

    public EntryEditorTabbedPane() {
        super();
    }

    public void hidePdfTab(FileAnnotationTab pdfTab){
        this.pdfTab = pdfTab;
        this.remove(pdfTab);
    }

}

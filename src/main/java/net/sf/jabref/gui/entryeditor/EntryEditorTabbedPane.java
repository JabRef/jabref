package net.sf.jabref.gui.entryeditor;

import javax.swing.JTabbedPane;

public class EntryEditorTabbedPane extends JTabbedPane {

    private PdfCommentsTab pdfTab;

    public EntryEditorTabbedPane() {
        super();
    }

    public void hidePdfTab(PdfCommentsTab pdfTab){
        this.pdfTab = pdfTab;
        this.remove(pdfTab);
    }

}

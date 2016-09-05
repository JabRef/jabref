package net.sf.jabref.gui.entryeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.pdf.PdfCommentImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class PdfCommentsTab extends JPanel {

    private JPanel informationPanel = new JPanel();

    private JList<String> commentList = new JList<>();

    private final JScrollPane commentScrollPane = new JScrollPane(commentList);

    private JLabel authorLabel = new JLabel("author",JLabel.CENTER);

    private JTextArea authorArea = new JTextArea("author",2,25);

    private JScrollPane authorScrollPane = new JScrollPane(authorArea);

    private JLabel dateLabel = new JLabel ("date",JLabel.CENTER);

    private JTextArea dateArea = new JTextArea("date",2,25);

    private JScrollPane dateScrollPane = new JScrollPane(dateArea);

    private JLabel siteLabel = new JLabel("site",JLabel.CENTER);

    private JTextArea siteArea = new JTextArea("site",2,25);

    private JScrollPane siteScrollPane = new JScrollPane(siteArea);

    DefaultListModel<String> listModel;

    private final EntryEditor parent;

    private final String tabTitle;

    private final JabRefFrame frame;

    private final BasePanel basePanel;

    public PdfCommentsTab(EntryEditor parent, JabRefFrame frame, BasePanel basePanel) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;
        this.tabTitle = "PDF comments";
        this.setUpInformationPanel();
        listModel  = new DefaultListModel<>();
        try {
            this.setUpPdfCommentsTab();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpPdfCommentsTab() throws IOException {
        Optional<String> field = parent.getEntry().getField(FieldName.FILE);
        if (field.isPresent()) {

            commentList.setModel(listModel);
            PdfCommentImporter commentImporter = new PdfCommentImporter();
            ArrayList<BibEntry> entries = new ArrayList<>();
            entries.add(parent.getEntry());
            commentImporter.importPdfFile(entries, basePanel.getBibDatabaseContext());

            HashMap<String, String> importedNotes = commentImporter.importNotes(commentImporter.importPdfFile(entries,
                    basePanel.getBibDatabaseContext()).get(0));
            updateShownComments(importedNotes);
        }

        commentScrollPane.setPreferredSize(new Dimension(450,200));

        this.add(commentScrollPane, BorderLayout.EAST);
        this.add(informationPanel,BorderLayout.WEST);
    }

    private void updateShownComments(HashMap<String, String> importedNotes){
        importedNotes.values().stream().forEach((note) -> listModel.addElement(note));
    }


    private void setUpInformationPanel(){
        authorArea.setEditable(false);
        dateArea.setEditable(false);
        siteArea.setEditable(false);

        informationPanel.add(authorLabel, BorderLayout.NORTH);
        informationPanel.add(authorScrollPane,BorderLayout.NORTH);
        informationPanel.add(dateLabel, BorderLayout.CENTER);
        informationPanel.add(dateScrollPane, BorderLayout.CENTER);
        informationPanel.add(siteLabel, BorderLayout.SOUTH);
        informationPanel.add(siteScrollPane, BorderLayout.SOUTH);
    }


}

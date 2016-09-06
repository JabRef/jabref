package net.sf.jabref.gui.entryeditor;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.pdf.PdfCommentImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.pdf.PdfComment;

import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfCommentsTab extends JPanel {

    private final JPanel informationPanel = new JPanel();
    private final JList<String> commentList = new JList<>();
    private final JScrollPane commentScrollPane = new JScrollPane();
    private final JLabel authorLabel = new JLabel(Localization.lang("Author"), JLabel.CENTER);
    private final JTextArea authorArea = new JTextArea("author", 2, 25);
    private final JScrollPane authorScrollPane = new JScrollPane();
    private final JLabel dateLabel = new JLabel(Localization.lang("Date"), JLabel.CENTER);
    private final JTextArea dateArea = new JTextArea("date", 2, 25);
    private final JScrollPane dateScrollPane = new JScrollPane();
    private final JLabel pageLabel = new JLabel(Localization.lang("Page"), JLabel.CENTER);
    private final JTextArea pageArea = new JTextArea("page", 2, 25);
    private final JScrollPane pageScrollPane = new JScrollPane();
    private final JLabel commentTxtLabel = new JLabel(Localization.lang("Content"),JLabel.CENTER);
    private final JTextArea commentTxtArea = new JTextArea("comment content", 10, 25);
    private final JScrollPane commentTxtScrollPane = new JScrollPane();
    DefaultListModel<String> listModel;

    private final EntryEditor parent;
    private final String tabTitle;
    private final JabRefFrame frame;
    private final BasePanel basePanel;

    public PdfCommentsTab(EntryEditor parent, JabRefFrame frame, BasePanel basePanel) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;
        this.tabTitle = Localization.lang("PDF comments");
        this.setUpInformationPanel();
        listModel  = new DefaultListModel<>();
        this.setUpPdfCommentsTab();
    }

    private void setUpPdfCommentsTab() {

        setLayout(null);
        commentScrollPane.setBounds(26, 16, 450, 389);

        commentScrollPane.setPreferredSize(new Dimension(450,200));

        this.add(commentScrollPane);
        commentScrollPane.setViewportView(commentList);
        informationPanel.setBounds(583, 16, 650, 389);
        this.add(informationPanel);
    }

    public void addComments() throws IOException {
        Optional<String> field = parent.getEntry().getField(FieldName.FILE);
        if (field.isPresent()) {
            commentList.setModel(listModel);
            PdfCommentImporter commentImporter = new PdfCommentImporter();
            ArrayList<BibEntry> entries = new ArrayList<>();
            entries.add(parent.getEntry());

            List<PDDocument> documents = commentImporter.importPdfFile(entries,
                    basePanel.getBibDatabaseContext());
            if (documents.isEmpty()) {
                listModel.clear();
                listModel.addElement(Localization.lang("Attached_file_has_no_valid_path"));
            } else {
                HashMap<String, PdfComment> importedNotes = commentImporter.importNotes(documents.get(0));
                updateShownComments(importedNotes);
            }
        }
    }

    private void updateShownComments(HashMap<String, PdfComment> importedNotes){
        listModel.clear();
        importedNotes.values().stream().forEach((note) -> listModel.addElement(note.getContent()));
    }

    private void setUpInformationPanel(){
        informationPanel.setLayout(null);

        authorLabel.setBounds(15, 25, 46, 20);
        informationPanel.add(authorLabel);
        authorScrollPane.setBounds(76, 25, 547, 42);
        informationPanel.add(authorScrollPane);
        authorScrollPane.setViewportView(authorArea);
        authorArea.setEditable(false);

        dateLabel.setBounds(15, 80, 30, 20);
        informationPanel.add(dateLabel);
        dateScrollPane.setBounds(76, 83, 547, 42);
        informationPanel.add(dateScrollPane);
        dateScrollPane.setViewportView(dateArea);
        dateArea.setEditable(false);
        
        pageLabel.setBounds(15, 128, 34, 20);
        informationPanel.add(pageLabel);
        pageScrollPane.setBounds(76, 141, 547, 42);
        informationPanel.add(pageScrollPane);
        pageScrollPane.setViewportView(pageArea);
        pageArea.setEditable(false);

        commentTxtLabel.setBounds(15, 200, 69, 20);
        informationPanel.add(commentTxtLabel);
        commentTxtScrollPane.setBounds(76, 200, 547, 173);
        informationPanel.add(commentTxtScrollPane);
        commentTxtScrollPane.setViewportView(commentTxtArea);
        commentTxtArea.setEditable(false);

    }
}
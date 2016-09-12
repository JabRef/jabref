package net.sf.jabref.gui.entryeditor;

import java.awt.Dimension;
import java.awt.SystemColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
    private final JList<PdfComment> commentList = new JList<>();
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
    DefaultListModel<PdfComment> listModel;

    private final EntryEditor parent;
    private final String tabTitle;
    private final JabRefFrame frame;
    private final BasePanel basePanel;
    private int commentListSelectedIndex;

    HashMap<String, PdfComment> importedNotes = new HashMap<>();

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
        informationPanel.setBounds(491, 16, 742, 389);
        this.add(informationPanel);
    }

    public void addComments() throws IOException {
        Optional<String> field = parent.getEntry().getField(FieldName.FILE);
        if (field.isPresent()) {
            if (!commentList.getModel().equals(listModel)) {
                commentList.setModel(listModel);
                commentList.addListSelectionListener(new CommentListSelectionListener());
            }
            PdfCommentImporter commentImporter = new PdfCommentImporter();
            ArrayList<BibEntry> entries = new ArrayList<>();
            entries.add(parent.getEntry());

            List<PDDocument> documents = commentImporter.importPdfFile(entries,
                    basePanel.getBibDatabaseContext());
            if (documents.isEmpty()) {
                listModel.clear();
                listModel.addElement(new PdfComment("", "", "", 0, Localization.lang("Attached_file_has_no_valid_path"), ""));
            } else {
                importedNotes = commentImporter.importNotes(documents.get(0));
                updateShownComments(importedNotes);
                commentList.setSelectedIndex(commentListSelectedIndex);
            }
        }
    }

    /**
     * Updates the list model to show the given notes exclusive those with no content
     * @param importedNotes value is the comments name and the value is a pdfComment object to add to the list model
     */
    private void updateShownComments(HashMap<String, PdfComment> importedNotes){
        listModel.clear();
        importedNotes.values().stream().filter(comment -> !(null == comment.getContent())).collect(Collectors.toList())
                .forEach((comment) -> listModel.addElement(comment));
    }

    private void updateTextFields(PdfComment comment) {
        authorArea.setText(comment.getAuthor());
        dateArea.setText(comment.getDate());
        pageArea.setText(String.valueOf(comment.getPage()));
        commentTxtArea.setText(comment.getContent());
    }

    private void setUpInformationPanel(){
        informationPanel.setLayout(null);

        authorLabel.setBounds(0, 44, 61, 20);
        informationPanel.add(authorLabel);
        authorScrollPane.setBounds(116, 43, 547, 20);
        informationPanel.add(authorScrollPane);
        authorArea.setBackground(SystemColor.control);
        authorScrollPane.setViewportView(authorArea);
        authorArea.setEditable(false);

        dateLabel.setBounds(0, 70, 61, 20);
        informationPanel.add(dateLabel);
        dateScrollPane.setBounds(116, 69, 547, 20);
        informationPanel.add(dateScrollPane);
        dateArea.setBackground(SystemColor.control);
        dateScrollPane.setViewportView(dateArea);
        dateArea.setEditable(false);

        pageLabel.setBounds(10, 95, 34, 20);
        informationPanel.add(pageLabel);
        pageScrollPane.setBounds(116, 94, 547, 20);
        informationPanel.add(pageScrollPane);
        pageArea.setBackground(SystemColor.control);
        pageScrollPane.setViewportView(pageArea);
        pageArea.setEditable(false);

        commentTxtLabel.setBounds(0, 138, 69, 20);
        informationPanel.add(commentTxtLabel);
        commentTxtScrollPane.setBounds(116, 143, 547, 173);
        informationPanel.add(commentTxtScrollPane);
        commentTxtScrollPane.setViewportView(commentTxtArea);
        commentTxtArea.setEditable(false);

        JScrollPane pdfNamePane = new JScrollPane();
        pdfNamePane.setBounds(116, 16, 506, 20);
        informationPanel.add(pdfNamePane);

        JTextArea pdfNameArea = new JTextArea();
        pdfNameArea.setBackground(SystemColor.control);
        pdfNameArea.setEditable(false);
        pdfNamePane.setViewportView(pdfNameArea);
        pdfNameArea.setText("pdfName");

        JLabel lblPdfName = new JLabel("PDF Name");
        lblPdfName.setBounds(0, 17, 81, 20);
        informationPanel.add(lblPdfName);

        JButton btnOpenPdf = new JButton("Open PDF");
        btnOpenPdf.setBounds(624, 16, 118, 21);
        informationPanel.add(btnOpenPdf);

    }

    private class CommentListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index;
            if (commentList.getSelectedIndex() >= 0) {
                index = commentList.getSelectedIndex();
                updateTextFields(listModel.get(index));
                commentListSelectedIndex = index;
            }
            commentList.setSelectedIndex(commentListSelectedIndex);
        }
    }
}
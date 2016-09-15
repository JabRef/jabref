package net.sf.jabref.gui.entryeditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.swing.BoxLayout;
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
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.pdf.PdfCommentImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.pdf.PdfComment;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfCommentsTab extends JPanel {

    private final JList<PdfComment> commentList = new JList<>();
    private final JScrollPane commentScrollPane = new JScrollPane();
    private final JLabel commentLabel = new JLabel(Localization.lang("Comments and highlighted text"), JLabel.LEFT);
    private final JLabel fileNameLabel = new JLabel(Localization.lang("Filename"),JLabel.CENTER);
    private final JTextArea filenameArea = new JTextArea("filename");
    private final JScrollPane fileNameScrollPane = new JScrollPane();
    private final JLabel authorLabel = new JLabel(Localization.lang("Author"), JLabel.CENTER);
    private final JTextArea authorArea = new JTextArea("author");
    private final JScrollPane authorScrollPane = new JScrollPane();
    private final JLabel dateLabel = new JLabel(Localization.lang("Date"), JLabel.CENTER);
    private final JTextArea dateArea = new JTextArea("date");
    private final JScrollPane dateScrollPane = new JScrollPane();
    private final JLabel pageLabel = new JLabel(Localization.lang("Page"), JLabel.CENTER);
    private final JTextArea pageArea = new JTextArea("page");
    private final JScrollPane pageScrollPane = new JScrollPane();
    private final JLabel commentTxtLabel = new JLabel(Localization.lang("Content"),JLabel.CENTER);
    private final JTextArea commentTxtArea = new JTextArea("comment content");
    private final JScrollPane commentTxtScrollPane = new JScrollPane();
    private final JButton copyToClipboardButton = new JButton();
    private final JButton openPdfButton = new JButton();
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
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        listModel  = new DefaultListModel<>();
        this.setUpPdfCommentsTab();
        this.setUpInformationPanel();
    }

    private void setUpPdfCommentsTab() {
        JPanel commentListPanel = FormBuilder.create()
                .columns("fill:pref:grow")
                .rows("pref, $lg, fill:pref:grow")
                .padding(Paddings.DIALOG)
                .add(commentLabel).xy(1,1)
                .add(commentScrollPane).xy(1,3)
                .build();
        commentScrollPane.setViewportView(commentList);

        this.add(commentListPanel);
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
        Comparator<PdfComment> byPage = (comment1, comment2) -> Integer.compare(comment1.getPage(), comment2.getPage());
        importedNotes.values().stream()
                .filter(comment -> !(null == comment.getContent()))
                .sorted(byPage)
                .forEach(listModel::addElement);
    }

    private void updateTextFields(PdfComment comment) {
        authorArea.setText(comment.getAuthor());
        dateArea.setText(comment.getDate());
        pageArea.setText(String.valueOf(comment.getPage()));
        commentTxtArea.setText(comment.getContent());
        filenameArea.setText(FileField.parse(parent.getEntry().getField(FieldName.FILE).get()).get(0).getLink());
    }

    private void setUpInformationPanel(){
        JPanel informationPanel  = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref")
                .padding(Paddings.DIALOG)
                .add(fileNameLabel).xy(1,1)
                .add(fileNameScrollPane).xy(3,1)
                .add(authorLabel).xy(1,3)
                .add(authorScrollPane).xy(3,3)
                .add(dateLabel).xy(1,5)
                .add(dateScrollPane).xy(3,5)
                .add(pageLabel).xy(1,7)
                .add(pageScrollPane).xy(3,7)
                .add(commentTxtLabel).xy(1,9)
                .add(commentTxtScrollPane).xy(3,9)
                .add(this.setUpButtons()).xy(3,11)
                .build();

        fileNameScrollPane.setViewportView(filenameArea);
        fileNameScrollPane.setBorder(null);
        authorScrollPane.setViewportView(authorArea);
        authorScrollPane.setBorder(null);
        dateScrollPane.setViewportView(dateArea);
        dateScrollPane.setBorder(null);
        pageScrollPane.setViewportView(pageArea);
        pageScrollPane.setBorder(null);
        commentTxtScrollPane.setViewportView(commentTxtArea);
        authorArea.setBackground(SystemColor.control);
        authorArea.setEditable(false);
        dateArea.setBackground(SystemColor.control);
        dateArea.setEditable(false);
        pageArea.setBackground(SystemColor.control);
        pageArea.setEditable(false);
        commentTxtArea.setEditable(false);
        filenameArea.setEditable(false);
        filenameArea.setBackground(SystemColor.control);

        this.add(informationPanel);
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

    private JPanel setUpButtons(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonConstraints = new GridBagConstraints();

        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonConstraints.gridy = 10;
        buttonConstraints.gridx = 10;
        openPdfButton.setText(Localization.lang("Open PDF"));
        openPdfButton.addActionListener(e -> openPdf());
        copyToClipboardButton.setText(Localization.lang("Copy to clipboard"));
        copyToClipboardButton.addActionListener(e -> copyToClipboard());

        buttonPanel.add(copyToClipboardButton, buttonConstraints);

        buttonConstraints.gridx = 1;
        buttonPanel.add(openPdfButton, buttonConstraints);

        return buttonPanel;
    }

    private void copyToClipboard(){
        new ClipBoardManager().setClipboardContents(commentTxtArea.getText());
    }

    private void openPdf() {

    }
}
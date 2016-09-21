package net.sf.jabref.gui.entryeditor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.pdf.PdfCommentImporter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.pdf.PdfComment;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotationHighlight;

public class PdfCommentsTab extends JPanel {

    private final JList<PdfComment> commentList = new JList<>();
    private final JScrollPane commentScrollPane = new JScrollPane();
    private final JLabel fileNameLabel = new JLabel(Localization.lang("Filename"),JLabel.CENTER);
    private final JComboBox<String> fileNameComboBox = new JComboBox<>();
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
    private final JTabbedPane tabbed;
    private int commentListSelectedIndex = 0;

    private ArrayList<PdfComment> importedNotes = new  ArrayList<PdfComment>();
    private ArrayList<ArrayList<PdfComment>> allNotes = new ArrayList<>();


    public PdfCommentsTab(EntryEditor parent, JabRefFrame frame, BasePanel basePanel, JTabbedPane tabbed) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;
        this.tabTitle = Localization.lang("PDF comments");
        this.tabbed = tabbed;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        listModel  = new DefaultListModel<>();
        try {
            addComments();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setUpPdfCommentsTab();
        this.setUpInformationPanel();
    }

    public void addComments() throws IOException {
        Optional<String> field = parent.getEntry().getField(FieldName.FILE);
        if (field.isPresent()) {
            if (!commentList.getModel().equals(listModel)) {
                commentList.setModel(listModel);
                commentList.addListSelectionListener(new CommentListSelectionListener());
                commentList.setCellRenderer(new CommentsListCellRenderer());
            }
            PdfCommentImporter commentImporter = new PdfCommentImporter();
            ArrayList<BibEntry> entries = new ArrayList<>();
            entries.add(parent.getEntry());

            int indexSelectedByComboBox;
            if (fileNameComboBox.getItemCount() == 0) {
                indexSelectedByComboBox = 0;
            } else {
                indexSelectedByComboBox = fileNameComboBox.getSelectedIndex();
            }
            FileField.parse(parent.getEntry().getField(FieldName.FILE).get()).stream()
                    .filter(parsedFileField -> parsedFileField.getLink().toLowerCase().endsWith(".pdf"))
                    .forEach(parsedFileField ->
                    allNotes.add(commentImporter.importNotes(parsedFileField.getLink())));

            updateShownComments(allNotes.get(indexSelectedByComboBox));
            if(listModel.isEmpty()) {
                tabbed.remove(this);
            } else if(commentList.isSelectionEmpty()){
                commentList.setSelectedIndex(0);
            }
            //set up the comboBox for representing the selected file
            fileNameComboBox.removeAllItems();
            FileField.parse(parent.getEntry().getField(FieldName.FILE).get()).stream()
                    .filter(parsedFileField -> parsedFileField.getLink().toLowerCase().endsWith(".pdf") )
                    .forEach(((parsedField) -> fileNameComboBox.addItem(parsedField.getLink())));
        }
    }

    /**
     * Updates the list model to show the given notes without those with no content
     * @param importedNotes value is the comments name and the value is a pdfComment object to add to the list model
     */
    private void updateShownComments(ArrayList<PdfComment> importedNotes){
        listModel.clear();
        if(importedNotes.isEmpty()){
            listModel.addElement(new PdfComment("", "", "", 0, Localization.lang("Attached_file_has_no_valid_path"), ""));
        } else {
            Comparator<PdfComment> byPage = (comment1, comment2) -> Integer.compare(comment1.getPage(), comment2.getPage());
            importedNotes.stream()
                    .filter(comment -> !(null == comment.getContent()))
                    .sorted(byPage)
                    .forEach(listModel::addElement);
        }
    }

    private void updateTextFields(PdfComment comment) {
        authorArea.setText(comment.getAuthor());
        dateArea.setText(comment.getDate());
        pageArea.setText(String.valueOf(comment.getPage()));
        commentTxtArea.setText(comment.getContent());

    }

    private void updateFileNameComboBox() {
        int indexSelectedByComboBox;
        if (fileNameComboBox.getItemCount() == 0) {
            indexSelectedByComboBox = 0;
        } else {
            indexSelectedByComboBox = fileNameComboBox.getSelectedIndex();
        }
        fileNameComboBox.removeAllItems();
        FileField.parse(parent.getEntry().getField(FieldName.FILE).get()).stream()
                .filter(parsedFileField -> parsedFileField.getLink().toLowerCase().endsWith(".pdf") )
                .forEach(((parsedField) -> fileNameComboBox.addItem(parsedField.getLink())));
        fileNameComboBox.setSelectedIndex(indexSelectedByComboBox);
        updateShownComments(allNotes.get(indexSelectedByComboBox));
    }

    private void setUpPdfCommentsTab() {
        JPanel commentListPanel = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, fill:pref:grow, $lg, pref")
                .padding(Paddings.DIALOG)
                .add(fileNameLabel).xy(1,1, "left, top")
                .add(fileNameScrollPane).xyw(2, 1, 2)
                .add(commentScrollPane).xyw(1, 3, 3)
                .build();
        commentScrollPane.setViewportView(commentList);

        this.add(commentListPanel);
    }

    private void setUpInformationPanel(){
        JPanel informationPanel  = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, pref, $lg, pref, $lg, pref, $lg, fill:pref:grow, $lg, pref")
                .padding(Paddings.DIALOG)
                .add(authorLabel).xy(1,3, "left, top")
                .add(authorScrollPane).xy(3,3)
                .add(dateLabel).xy(1,5, "left, top")
                .add(dateScrollPane).xy(3,5)
                .add(pageLabel).xy(1,7, "left, top")
                .add(pageScrollPane).xy(3,7)
                .add(commentTxtLabel).xy(1,9, "left, top")
                .add(commentTxtScrollPane).xy(3,9)
                .add(this.setUpButtons()).xy(3,11)
                .build();

        fileNameScrollPane.setViewportView(fileNameComboBox);
        fileNameLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        authorLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        dateLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        pageLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        commentTxtLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
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
        commentTxtArea.setLineWrap(true);
        fileNameComboBox.setEditable(false);
        fileNameComboBox.setBackground(SystemColor.control);
        fileNameComboBox.addActionListener(e -> updateFileNameComboBox());

        this.add(informationPanel);
    }

    private JPanel setUpButtons(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonConstraints = new GridBagConstraints();

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


    private class CommentListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {

            int index;
            if (commentList.getSelectedIndex() >= 0) {
                index = commentList.getSelectedIndex();
                updateTextFields(listModel.get(index));
                commentListSelectedIndex = index;
            } else {
                commentListSelectedIndex = 0;
            }
            commentList.setSelectedIndex(commentListSelectedIndex);
            //repaint the list to refresh the linked annotation highlighting
            commentList.repaint();
        }
    }

    /**
     * Triggers the cellRenderer to render the given cell
     * @param cellIndex index in the listModel of the comment which is in the cell that has to be rendered
     */
    private void triggerRenderingACell(int cellIndex){
        if(null != (listModel.get(cellIndex).getLinkedPdfComment())) {
            commentList.getCellRenderer().getListCellRendererComponent(commentList,
                    listModel.get(cellIndex).getLinkedPdfComment(),
                    listModel.indexOf(listModel.get(cellIndex).getLinkedPdfComment()), false, false);
        }
    }

    /**
     * Cell renderer that shows different icons dependent on the annotation subtype and highlights annotations that are
     * linked with each other eg.
     */
    class CommentsListCellRenderer extends DefaultListCellRenderer {

        JLabel label;

        public CommentsListCellRenderer() {
            this.label = new JLabel();
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            boolean isLinkedComment = false;
            PdfComment comment = (PdfComment) value;

            if(comment.equals(commentList.getSelectedValue().getLinkedPdfComment())){
                isLinkedComment = true;
            }

            //call the super method so that the cell selection is done as usual
            label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            //If more different comment types should be reflected by icons in the list, add them here
            switch(comment.getAnnotationType()){
                case FDFAnnotationHighlight.SUBTYPE:
                    label.setIcon(IconTheme.JabRefIcon.MARKER.getSmallIcon());
                    break;
                default:
                    label.setIcon(IconTheme.JabRefIcon.COMMENT.getSmallIcon());
                    break;
            }

            label.setToolTipText(comment.getAnnotationType());
            label.setText(comment.toString());

            if(isLinkedComment){
                label.setBackground(GUIGlobals.activeBackgroundColor);
            }

            return label;
        }
    }
}
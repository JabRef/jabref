package net.sf.jabref.gui.entryeditor;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.pdf.EntryAnnotationImporter;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.pdf.FileAnnotation;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotationHighlight;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

class FileAnnotationTab extends JPanel {

    private final JList<FileAnnotation> commentList = new JList<>();
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
    private final JTextArea contentTxtArea = new JTextArea();
    private final JLabel highlightTxtLabel = new JLabel(Localization.lang("Highlight"), JLabel.CENTER);
    private final JTextArea highlightTxtArea = new JTextArea();
    private final JScrollPane commentTxtScrollPane = new JScrollPane();
    private final JScrollPane highlightScrollPane = new JScrollPane();
    private final JButton copyToClipboardButton = new JButton();
    private final JButton reloadAnnotationsButton = new JButton();
    private DefaultListModel<FileAnnotation> listModel;

    private final EntryEditor parent;

    private Map<String, List<FileAnnotation>> annotationsOfFiles;
    private boolean isInitialized;


    FileAnnotationTab(EntryEditor parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        listModel  = new DefaultListModel<>();
        this.isInitialized = false;

    }

    FileAnnotationTab initializeTab(FileAnnotationTab tab) {
        if (tab.isInitialized) {
            return tab;
        }

        tab.setUpGui();
        tab.isInitialized = true;
        tab.parent.repaint();
        return tab;
    }

    FileAnnotationTab initializeTab(FileAnnotationTab tab, Map<String, List<FileAnnotation>> cachedFileAnnotations) {
        this.annotationsOfFiles = cachedFileAnnotations;

        if (tab.isInitialized) {
            return tab;
        }

        tab.addAnnotations();
        tab.setUpGui();
        tab.isInitialized = true;
        tab.parent.repaint();
        return tab;

    }

    /**
     * Adds pdf annotations from all attached pdf files belonging to the entry selected in the main table and
     * shows those from the first file in the file annotations tab
     */
    private void addAnnotations() {
        if (parent.getEntry().getField(FieldName.FILE).isPresent()) {
            if (!commentList.getModel().equals(listModel)) {
                commentList.setModel(listModel);
                commentList.addListSelectionListener(new CommentListSelectionListener());
                commentList.setCellRenderer(new CommentsListCellRenderer());
            }

            //set up the comboBox for representing the selected file
            fileNameComboBox.removeAllItems();
            new EntryAnnotationImporter(parent.getEntry()).getFilteredFileList().
                    forEach(((parsedField) -> fileNameComboBox.addItem(parsedField.getLink())));
            //show the annotationsOfFiles attached to the selected file
            updateShownAnnotations(annotationsOfFiles.get(fileNameComboBox.getSelectedItem() == null ?
                    fileNameComboBox.getItemAt(0) : fileNameComboBox.getSelectedItem().toString()));
            //select the first annotation
            if(commentList.isSelectionEmpty()){
                commentList.setSelectedIndex(0);
            }
        }
    }

    /**
     * Updates the list model to show the given notes without those with no content
     * @param annotations value is the comments name and the value is a pdfComment object to add to the list model
     */
    private void updateShownAnnotations(List<FileAnnotation> annotations) {
        listModel.clear();
        if (annotations.isEmpty()) {
            listModel.addElement(new FileAnnotation("", "", "", 0, Localization.lang("File has no attached annotations"), ""));
        } else {
            Comparator<FileAnnotation> byPage = Comparator.comparingInt(FileAnnotation::getPage);
            annotations.stream()
                    .filter(annotation -> !(null == annotation.getContent()))
                    .filter(annotation -> annotation.getAnnotationType().equals(FDFAnnotationHighlight.SUBTYPE)
                            || (null == annotation.getLinkedFileAnnotation()))
                    .sorted(byPage)
                    .forEach(listModel::addElement);
        }
    }

    /**
     * Updates the text fields showing meta data and the content from the selected comment
     * @param comment pdf comment which data should be shown in the text fields
     */
    private void updateTextFields(FileAnnotation comment) {
        authorArea.setText(comment.getAuthor());
        dateArea.setText(comment.getDate());
        pageArea.setText(String.valueOf(comment.getPage()));
        updateContentAndHighlightTextfields(comment);

    }

    /**
     * Updates the selection of files that are attached to the pdf file
     */
    private void updateFileNameComboBox() {
        int indexSelectedByComboBox;
        if (fileNameComboBox.getItemCount() == 0) {
            indexSelectedByComboBox = 0;
        } else {
            indexSelectedByComboBox = fileNameComboBox.getSelectedIndex();
        }
        fileNameComboBox.removeAllItems();
        new EntryAnnotationImporter(parent.getEntry()).getFilteredFileList().stream().filter(parsedFileField -> parsedFileField.getLink().toLowerCase().endsWith(".pdf"))
                .forEach(((parsedField) -> fileNameComboBox.addItem(parsedField.getLink())));
        fileNameComboBox.setSelectedIndex(indexSelectedByComboBox);
        updateShownAnnotations(annotationsOfFiles.get(fileNameComboBox.getSelectedItem().toString()));
    }

    private void setUpGui() {
        JPanel commentListPanel = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, fill:pref:grow, $lg, pref")
                .padding(Paddings.DIALOG)
                .add(fileNameLabel).xy(1,1, "left, top")
                .add(fileNameScrollPane).xyw(2, 1, 2)
                .add(commentScrollPane).xyw(1, 3, 3)
                .build();
        commentScrollPane.setViewportView(commentList);

        JPanel informationPanel  = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref:grow, $lg, pref:grow, $lg, fill:pref")
                .padding(Paddings.DIALOG)
                .add(authorLabel).xy(1,3, "left, top")
                .add(authorScrollPane).xy(3,3)
                .add(dateLabel).xy(1,5, "left, top")
                .add(dateScrollPane).xy(3,5)
                .add(pageLabel).xy(1,7, "left, top")
                .add(pageScrollPane).xy(3,7)
                .add(commentTxtLabel).xy(1,9, "left, top")
                .add(commentTxtScrollPane).xywh(3,9, 1, 2)
                .add(highlightTxtLabel).xy(1, 11, "left, top")
                .add(highlightScrollPane).xywh(3, 11, 1, 2)
                .add(this.setUpButtons()).xyw(1, 13, 3)
                .build();

        fileNameScrollPane.setViewportView(fileNameComboBox);
        fileNameLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        authorLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        dateLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        pageLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        commentTxtLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        highlightTxtLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        fileNameScrollPane.setBorder(null);
        authorScrollPane.setViewportView(authorArea);
        authorScrollPane.setBorder(null);
        dateScrollPane.setViewportView(dateArea);
        dateScrollPane.setBorder(null);
        pageScrollPane.setViewportView(pageArea);
        pageScrollPane.setBorder(null);
        commentTxtScrollPane.setViewportView(contentTxtArea);
        highlightScrollPane.setViewportView(highlightTxtArea);
        authorArea.setEditable(false);
        dateArea.setEditable(false);
        pageArea.setEditable(false);
        contentTxtArea.setEditable(false);
        contentTxtArea.setLineWrap(true);
        highlightTxtArea.setEditable(false);
        highlightTxtArea.setLineWrap(true);
        fileNameComboBox.setEditable(false);
        fileNameComboBox.addActionListener(e -> updateFileNameComboBox());

        this.add(FormBuilder.create()
                .columns("0:grow, $lcgap, 0:grow")
                .rows("fill:pref:grow")
                .add(commentListPanel).xy(1, 1)
                .add(informationPanel).xy(3, 1)
                .build());
    }

    private JPanel setUpButtons(){
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonConstraints = new GridBagConstraints();

        copyToClipboardButton.setText(Localization.lang("Copy to clipboard"));
        copyToClipboardButton.addActionListener(e -> copyToClipboard());
        reloadAnnotationsButton.setText(Localization.lang("Reload annotations"));
        reloadAnnotationsButton.addActionListener(e -> reloadAnnotations());

        buttonConstraints.gridy = 10;
        buttonConstraints.gridx = 3;

        buttonPanel.add(copyToClipboardButton, buttonConstraints);

        buttonConstraints.gridx = 2;

        buttonConstraints.gridx = 1;
        buttonPanel.add(reloadAnnotationsButton, buttonConstraints);

        return buttonPanel;
    }

    /**
     * Copies the meta and content information of the pdf annotation to the clipboard
     */
    private void copyToClipboard(){
        StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
        sj.add("Author: " + authorArea.getText());
        sj.add("Date: " + dateArea.getText());
        sj.add("Page: " + pageArea.getText());
        sj.add("Content: " + contentTxtArea.getText());
        sj.add("Highlighted: " + highlightTxtArea.getText());

        new ClipBoardManager().setClipboardContents(sj.toString());
    }

    private void reloadAnnotations() {
        isInitialized = false;
        Arrays.stream(this.getComponents()).forEach(this::remove);
        initializeTab(this);
        this.repaint();
    }


    /**
     * Fills the highlight and comment texts and enables/disables the highlight area if there is no highlighted text
     *
     * @param comment either a text comment or a highlighting from a pdf
     */
    private void updateContentAndHighlightTextfields(final FileAnnotation comment){

        if(comment.hasLinkedComment()){
            String textComment = "";
            String highlightedText = "";

            if(comment.getAnnotationType().equals(FDFAnnotationHighlight.SUBTYPE)){
                highlightedText = comment.getContent();
                textComment = comment.getLinkedFileAnnotation().getContent();
            } else {
                highlightedText = comment.getLinkedFileAnnotation().getContent();
                textComment = comment.getContent();
            }
            highlightTxtArea.setEnabled(true);
            contentTxtArea.setText(textComment);
            highlightTxtArea.setText(highlightedText);

        } else {
            contentTxtArea.setText(comment.getContent());
            highlightTxtArea.setText("N/A");
            highlightTxtArea.setEnabled(false);
        }
    }


    private class CommentListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {

            int index;
            int commentListSelectedIndex;
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
     * Cell renderer that shows different icons dependent on the annotation subtype
     */
    class CommentsListCellRenderer extends DefaultListCellRenderer {

        JLabel label;

        CommentsListCellRenderer() {
            this.label = new JLabel();
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            FileAnnotation comment = (FileAnnotation) value;

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

            return label;
        }
    }

    boolean isInitialized() {
        return isInitialized;
    }
}

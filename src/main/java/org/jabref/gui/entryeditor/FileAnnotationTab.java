package org.jabref.gui.entryeditor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.FileAnnotation;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import static org.jabref.model.pdf.FileAnnotationType.NONE;


class FileAnnotationTab extends JPanel {

    private final JList<FileAnnotation> annotationList = new JList<>();
    private final JScrollPane annotationScrollPane = new JScrollPane();
    private final JLabel fileNameLabel = new JLabel(Localization.lang("Filename"), JLabel.CENTER);
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
    private final JLabel annotationTextLabel = new JLabel(Localization.lang("Content"), JLabel.CENTER);
    private final JTextArea contentTxtArea = new JTextArea();
    private final JLabel markedTextLabel = new JLabel(Localization.lang("Marking"), JLabel.CENTER);
    private final JTextArea markedTxtArea = new JTextArea();
    private final JScrollPane annotationTextScrollPane = new JScrollPane();
    private final JScrollPane markedTextScrollPane = new JScrollPane();
    private final JButton copyToClipboardButton = new JButton();
    private final JButton reloadAnnotationsButton = new JButton();
    private final FileAnnotationCache fileAnnotationCache;
    private DefaultListModel<FileAnnotation> listModel;

    private final EntryEditor parent;

    private boolean isInitialized;


    FileAnnotationTab(EntryEditor parent, FileAnnotationCache cache) {
        this.fileAnnotationCache = cache;
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        listModel = new DefaultListModel<>();
        this.isInitialized = false;
    }

    public FileAnnotationTab initializeTab(FileAnnotationTab tab) {
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
            if (!annotationList.getModel().equals(listModel)) {
                annotationList.setModel(listModel);
                annotationList.addListSelectionListener(new AnnotationListSelectionListener());
                annotationList.setCellRenderer(new AnnotationListCellRenderer());
            }

            //set up the comboBox for representing the selected file
            fileNameComboBox.removeAllItems();
            final Map<String, List<FileAnnotation>> fileAnnotations = fileAnnotationCache.getFromCache(parent.getEntry());
            fileAnnotations.keySet().forEach(fileNameComboBox::addItem);

            //show the annotationsOfFiles attached to the selected file
            updateShownAnnotations(fileAnnotations.get(fileNameComboBox.getSelectedItem() == null ?
                    fileNameComboBox.getItemAt(0) : fileNameComboBox.getSelectedItem().toString()));
            //select the first annotation
            if (annotationList.isSelectionEmpty()) {
                annotationList.setSelectedIndex(0);
            }
        }
    }

    /**
     * Updates the list model to show the given notes without those with no content
     *
     * @param annotations value is the annotation name and the value is a pdfAnnotation object to add to the list model
     */
    private void updateShownAnnotations(List<FileAnnotation> annotations) {
        listModel.clear();
        if (annotations.isEmpty()) {
            listModel.addElement(new FileAnnotation("", LocalDateTime.now(), 0, Localization.lang("File has no attached annotations"), NONE, Optional.empty()));
        } else {
            Comparator<FileAnnotation> byPage = Comparator.comparingInt(FileAnnotation::getPage);
            annotations.stream()
                    .filter(annotation -> (null != annotation.getContent()))
                    .sorted(byPage)
                    .forEach(annotation -> listModel.addElement(new FileAnnotationViewModel(annotation)));
        }
    }


    /**
     * Updates the text fields showing meta data and the content from the selected annotation
     *
     * @param annotation pdf annotation which data should be shown in the text fields
     */
    private void updateTextFields(FileAnnotation annotation) {
        authorArea.setText(annotation.getAuthor());
        dateArea.setText(annotation.getTimeModified().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        pageArea.setText(String.valueOf(annotation.getPage()));
        updateContentAndMarkedTextAreas(annotation);
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
        final Map<String, List<FileAnnotation>> fileAnnotations = fileAnnotationCache.getFromCache(parent.getEntry());
        fileAnnotations.keySet().stream().filter(filename -> filename.toLowerCase(Locale.ROOT).endsWith(".pdf")).
                forEach((fileNameComboBox::addItem));
        fileNameComboBox.setSelectedIndex(indexSelectedByComboBox);
        updateShownAnnotations(fileAnnotations.get(fileNameComboBox.getSelectedItem().toString()));
    }

    private void setUpGui() {
        JPanel annotationPanel = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, fill:pref:grow, $lg, pref")
                .padding(Paddings.DIALOG)
                .add(fileNameLabel).xy(1, 1, "left, top")
                .add(fileNameScrollPane).xyw(2, 1, 2)
                .add(annotationScrollPane).xyw(1, 3, 3)
                .build();
        annotationScrollPane.setViewportView(annotationList);

        JPanel informationPanel = FormBuilder.create()
                .columns("pref, $lcgap, pref:grow")
                .rows("pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref:grow, $lg, pref:grow, $lg, fill:pref")
                .padding(Paddings.DIALOG)
                .add(authorLabel).xy(1, 3, "left, top")
                .add(authorScrollPane).xy(3, 3)
                .add(dateLabel).xy(1, 5, "left, top")
                .add(dateScrollPane).xy(3, 5)
                .add(pageLabel).xy(1, 7, "left, top")
                .add(pageScrollPane).xy(3, 7)
                .add(annotationTextLabel).xy(1, 9, "left, top")
                .add(annotationTextScrollPane).xywh(3, 9, 1, 2)
                .add(markedTextLabel).xy(1, 11, "left, top")
                .add(markedTextScrollPane).xywh(3, 11, 1, 2)
                .add(this.setUpButtons()).xyw(1, 13, 3)
                .build();

        fileNameScrollPane.setViewportView(fileNameComboBox);
        fileNameLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        authorLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        dateLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        pageLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        annotationTextLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        markedTextLabel.setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        fileNameScrollPane.setBorder(null);
        authorScrollPane.setViewportView(authorArea);
        authorScrollPane.setBorder(null);
        dateScrollPane.setViewportView(dateArea);
        dateScrollPane.setBorder(null);
        pageScrollPane.setViewportView(pageArea);
        pageScrollPane.setBorder(null);
        annotationTextScrollPane.setViewportView(contentTxtArea);
        markedTextScrollPane.setViewportView(markedTxtArea);
        authorArea.setEditable(false);
        dateArea.setEditable(false);
        pageArea.setEditable(false);
        contentTxtArea.setEditable(false);
        contentTxtArea.setLineWrap(true);
        markedTxtArea.setEditable(false);
        markedTxtArea.setLineWrap(true);
        fileNameComboBox.setEditable(false);
        fileNameComboBox.addActionListener(e -> updateFileNameComboBox());

        this.add(FormBuilder.create()
                .columns("0:grow, $lcgap, 0:grow")
                .rows("fill:pref:grow")
                .add(annotationPanel).xy(1, 1)
                .add(informationPanel).xy(3, 1)
                .build());
    }

    private JPanel setUpButtons() {
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
    private void copyToClipboard() {
        StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
        sj.add(Localization.lang("Author") + ": " + authorArea.getText());
        sj.add(Localization.lang("Date") + ": " + dateArea.getText());
        sj.add(Localization.lang("Page") + ": " + pageArea.getText());
        sj.add(Localization.lang("Content") + ": " + contentTxtArea.getText());
        sj.add(Localization.lang("Marking") + ": " + markedTxtArea.getText());

        new ClipBoardManager().setClipboardContents(sj.toString());
    }

    private void reloadAnnotations() {
        isInitialized = false;
        Arrays.stream(this.getComponents()).forEach(this::remove);
        fileAnnotationCache.remove(parent.getEntry());
        initializeTab(this);
        this.repaint();
    }


    /**
     * Fills the TextAreas of the content and the highlighted or underlined text with the corresponding text and also
     * changes the label accordingly.
     *
     * @param annotation either a text annotation or a marking from a PDF
     */
    private void updateContentAndMarkedTextAreas(final FileAnnotation annotation) {
        updateMarkingType(annotation);

        if (annotation.hasLinkedAnnotation()) {
            // isPresent() of the optional is already checked in annotation.hasLinkedAnnotation()
            if (!annotation.getLinkedFileAnnotation().getContent().isEmpty()) {
                contentTxtArea.setText(annotation.getLinkedFileAnnotation().getContent());
                contentTxtArea.setEnabled(true);
            } else {
                contentTxtArea.setText("N/A");
                contentTxtArea.setEnabled(false);
            }

            if (annotation.getContent().isEmpty()) {
                markedTxtArea.setEnabled(false);
                markedTxtArea.setText(Localization.lang("The marked area does not contain any legible text!"));
            } else {
                markedTxtArea.setEnabled(true);
                markedTxtArea.setText(annotation.getContent());
            }
        } else {
            contentTxtArea.setEnabled(true);
            if ("File has no attached annotations.".equals(annotation.getContent())) {
                authorArea.setText("N/A");
                authorArea.setEnabled(false);
                dateArea.setText("N/A");
                dateArea.setEnabled(false);
                pageArea.setText("N/A");
                pageArea.setEnabled(false);
                contentTxtArea.setEnabled(false);
            }
            contentTxtArea.setText(annotation.getContent());
            markedTxtArea.setText("N/A");
            markedTxtArea.setEnabled(false);
        }
    }

    private void updateMarkingType(FileAnnotation annotation) {
        switch (annotation.getAnnotationType()) {
            case UNDERLINE:
                markedTextLabel.setText(Localization.lang("Underline"));
                break;
            case HIGHLIGHT:
                markedTextLabel.setText(Localization.lang("Highlight"));
                break;
            default:
                markedTextLabel.setText(Localization.lang("Marking"));
                break;
        }
    }


    private class AnnotationListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {

            int index;
            int annotationListSelectedIndex = 0;
            if (annotationList.getSelectedIndex() >= 0) {
                index = annotationList.getSelectedIndex();
                updateTextFields(listModel.get(index));
                annotationListSelectedIndex = index;
            }
            annotationList.setSelectedIndex(annotationListSelectedIndex);
            //repaint the list to refresh the linked annotation
            annotationList.repaint();
        }
    }

    /**
     * Cell renderer that shows different icons dependent on the annotation subtype
     */
    class AnnotationListCellRenderer extends DefaultListCellRenderer {

        JLabel label;

        AnnotationListCellRenderer() {
            this.label = new JLabel();
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            FileAnnotation annotation = (FileAnnotation) value;

            //call the super method so that the cell selection is done as usual
            label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            //If more different annotation types should be reflected by icons in the list, add them here
            switch (annotation.getAnnotationType()) {
                case HIGHLIGHT:
                    label.setIcon(IconTheme.JabRefIcon.MARKER.getSmallIcon());
                    break;
                case UNDERLINE:
                    label.setIcon(IconTheme.JabRefIcon.MARKER.getSmallIcon());
                    break;
                default:
                    label.setIcon(IconTheme.JabRefIcon.OPTIONAL.getSmallIcon());
                    break;
            }

            label.setToolTipText(annotation.getAnnotationType().toString());
            label.setText(annotation.toString());

            return label;
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}

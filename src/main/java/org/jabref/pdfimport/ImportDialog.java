package org.jabref.pdfimport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import org.jabref.Globals;
import org.jabref.gui.JabRefDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportDialog extends JabRefDialog {

    public static final int NOMETA = 0;
    public static final int XMP = 1;
    public static final int CONTENT = 2;
    public static final int ONLYATTACH = 4;

    private final JCheckBox checkBoxDoNotShowAgain;
    private final JCheckBox useDefaultPDFImportStyle;
    private final JRadioButton radioButtonXmp;
    private final JRadioButton radioButtonPDFcontent;
    private final JRadioButton radioButtonNoMeta;
    private final JRadioButton radioButtononlyAttachPDF;
    private int result;


    public ImportDialog(boolean targetIsARow, String fileName) {
        super(ImportDialog.class);

        Boolean targetIsARow1 = targetIsARow;
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        setContentPane(contentPane);
        JPanel panel3 = new JPanel();
        panel3.setBackground(new Color(-1643275));
        JLabel labelHeadline = new JLabel(Localization.lang("Import metadata from:"));
        labelHeadline.setFont(new Font(labelHeadline.getFont().getName(), Font.BOLD, 14));
        JLabel labelSubHeadline = new JLabel(Localization.lang("Choose the source for the metadata import"));
        labelSubHeadline.setFont(new Font(labelSubHeadline.getFont().getName(), labelSubHeadline.getFont().getStyle(), 13));
        JLabel labelFileName = new JLabel();
        labelFileName.setFont(new Font(labelHeadline.getFont().getName(), Font.BOLD, 14));
        JPanel headLinePanel = new JPanel();
        headLinePanel.add(labelHeadline);
        headLinePanel.add(labelFileName);
        headLinePanel.setBackground(new Color(-1643275));
        GridLayout gl = new GridLayout(2, 1);
        gl.setVgap(10);
        gl.setHgap(10);
        panel3.setLayout(gl);
        panel3.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel3.add(headLinePanel);
        panel3.add(labelSubHeadline);
        radioButtonNoMeta = new JRadioButton(Localization.lang("Create blank entry linking the PDF"));
        radioButtonXmp = new JRadioButton(Localization.lang("Create entry based on XMP-metadata"));
        radioButtonPDFcontent = new JRadioButton(Localization.lang("Create entry based on content"));
        radioButtononlyAttachPDF = new JRadioButton(Localization.lang("Only attach PDF"));
        JButton buttonOK = new JButton(Localization.lang("OK"));
        JButton buttonCancel = new JButton(Localization.lang("Cancel"));
        checkBoxDoNotShowAgain = new JCheckBox(Localization.lang("Do not show this box again for this import"));
        useDefaultPDFImportStyle = new JCheckBox(Localization.lang("Always use this PDF import style (and do not ask for each import)"));
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref, 5dlu, left:pref:grow", ""));
        List<BibEntry> foundEntries = getEntriesFromXMP(fileName);
        JPanel entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        foundEntries.forEach(entry -> {
            JTextArea entryArea = new JTextArea(entry.toString());
            entryArea.setEditable(false);
            entriesPanel.add(entryArea);
        });

        b.appendSeparator(Localization.lang("Create new entry"));
        b.append(radioButtonNoMeta, 3);
        b.append(radioButtonXmp, 3);
        b.append(radioButtonPDFcontent, 3);
        b.appendSeparator(Localization.lang("Update existing entry"));
        b.append(radioButtononlyAttachPDF, 3);
        b.nextLine();
        b.append(checkBoxDoNotShowAgain);
        b.append(useDefaultPDFImportStyle);
        if (!foundEntries.isEmpty()) {
            b.appendSeparator(Localization.lang("XMP-metadata"));
            b.append(entriesPanel, 3);
        }
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(buttonOK);
        bb.addButton(buttonCancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        contentPane.add(panel3, BorderLayout.NORTH);
        contentPane.add(b.getPanel(), BorderLayout.CENTER);
        contentPane.add(bb.getPanel(), BorderLayout.SOUTH);

        if (!targetIsARow1) {
            this.radioButtononlyAttachPDF.setEnabled(false);
        }
        String name = new File(fileName).getName();
        labelFileName.setText(StringUtil.limitStringLength(name, 34));
        this.setTitle(Localization.lang("Import metadata from PDF"));

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        // only one of the radio buttons may be selected.
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioButtonNoMeta);
        bg.add(radioButtonXmp);
        bg.add(radioButtonPDFcontent);
        bg.add(radioButtononlyAttachPDF);

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        switch (Globals.prefs.getInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE)) {
        case NOMETA:
            radioButtonNoMeta.setSelected(true);
            break;
        case XMP:
            radioButtonXmp.setSelected(true);
            break;
        case CONTENT:
            radioButtonPDFcontent.setSelected(true);
            break;
        case ONLYATTACH:
            radioButtononlyAttachPDF.setSelected(true);
            break;
        default:
            // fallback
            radioButtonPDFcontent.setSelected(true);
            break;
        }

        this.setSize(555, 371);
    }

    private List<BibEntry> getEntriesFromXMP(String fileName) {
        List<BibEntry> foundEntries = new ArrayList<>();
        try {
            foundEntries = XmpUtilReader.readXmp(fileName, Globals.prefs.getXMPPreferences());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return foundEntries;
    }

    private void onOK() {
        this.result = JOptionPane.OK_OPTION;
        Globals.prefs.putInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE, this.getChoice());
        if (useDefaultPDFImportStyle.isSelected()) {
            Globals.prefs.putBoolean(JabRefPreferences.IMPORT_ALWAYSUSE, true);
        }
        // checkBoxDoNotShowAgain handled by local variable
        dispose();
    }

    private void onCancel() {
        this.result = JOptionPane.CANCEL_OPTION;
        dispose();
    }

    public void showDialog() {
        this.pack();
        this.setVisible(true);
    }

    public int getChoice() {
        if (radioButtonXmp.isSelected()) {
            return ImportDialog.XMP;
        } else if (radioButtonPDFcontent.isSelected()) {
            return ImportDialog.CONTENT;
        } else if (radioButtonNoMeta.isSelected()) {
            return ImportDialog.NOMETA;
        } else if (radioButtononlyAttachPDF.isSelected()) {
            return ImportDialog.ONLYATTACH;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean isDoNotShowAgain() {
        return this.checkBoxDoNotShowAgain.isSelected();
    }

    public int getResult() {
        return result;
    }

    public void disableXMPChoice() {
        this.radioButtonXmp.setEnabled(false);
    }
}

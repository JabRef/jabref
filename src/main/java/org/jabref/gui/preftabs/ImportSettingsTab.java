package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.pdfimport.ImportDialog;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportSettingsTab extends JPanel implements PrefsTab {

    public static final String[] DEFAULT_FILENAMEPATTERNS = new String[] {"[bibtexkey]",
            "[bibtexkey] - [title]"};

    public static final int DEFAULT_STYLE = ImportDialog.CONTENT;
    private static final String[] DEFAULT_FILENAMEPATTERNS_DISPLAY = new String[] {"bibtexkey", "bibtexkey - title",};

    private final JabRefPreferences prefs;
    private final JRadioButton radioButtonXmp;
    private final JRadioButton radioButtonPDFcontent;
    private final JRadioButton radioButtonNoMeta;
    private final JRadioButton radioButtononlyAttachPDF;
    private final JCheckBox useDefaultPDFImportStyle;

    private final JTextField fileNamePattern;
    private final JButton selectFileNamePattern;

    private final JTextField fileDirPattern;

    public ImportSettingsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);

        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu");
        radioButtonNoMeta = new JRadioButton(Localization.lang("Create blank entry linking the PDF"));
        radioButtonXmp = new JRadioButton(Localization.lang("Create entry based on XMP-metadata"));
        radioButtonPDFcontent = new JRadioButton(Localization.lang("Create entry based on content"));
        radioButtononlyAttachPDF = new JRadioButton(Localization.lang("Only attach PDF"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioButtonNoMeta);
        bg.add(radioButtonXmp);
        bg.add(radioButtonPDFcontent);
        bg.add(radioButtononlyAttachPDF);

        useDefaultPDFImportStyle = new JCheckBox(
                Localization.lang("Always use this PDF import style (and do not ask for each import)"));

        fileNamePattern = new JTextField(50);
        fileDirPattern = new JTextField(50);
        selectFileNamePattern = new JButton(Localization.lang("Choose pattern"));
        selectFileNamePattern.addActionListener(e -> openFilePatternMenu());

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();

        builder.appendSeparator(Localization.lang("Default import style for drag and drop of PDFs"));
        builder.nextLine();
        builder.append(pan);
        builder.append(radioButtonNoMeta);
        builder.nextLine();
        builder.append(pan);
        builder.append(radioButtonXmp);
        builder.nextLine();
        builder.append(pan);
        builder.append(radioButtonPDFcontent);
        builder.nextLine();
        builder.append(pan);
        builder.append(radioButtononlyAttachPDF);
        builder.nextLine();
        builder.append(pan);
        builder.append(useDefaultPDFImportStyle);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Default PDF file link action"));
        builder.nextLine();
        builder.append(pan);

        JPanel pan2 = new JPanel();
        JLabel lab = new JLabel(Localization.lang("Filename format pattern").concat(":"));
        pan2.add(lab);
        pan2.add(fileNamePattern);
        pan2.add(selectFileNamePattern);
        builder.append(pan2);

        JPanel pan3 = new JPanel();
        JLabel lbfileDirPattern = new JLabel(Localization.lang("File directory pattern").concat(":"));
        pan3.add(lbfileDirPattern);
        pan3.add(fileDirPattern);

        builder.nextLine();
        builder.append(pan);
        builder.append(pan3);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {

        useDefaultPDFImportStyle.setSelected(prefs.getBoolean(JabRefPreferences.IMPORT_ALWAYSUSE));
        int style = prefs.getInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE);
        switch (style) {
        case ImportDialog.NOMETA:
            radioButtonNoMeta.setSelected(true);
            break;
        case ImportDialog.XMP:
            radioButtonXmp.setSelected(true);
            break;
        case ImportDialog.CONTENT:
            radioButtonPDFcontent.setSelected(true);
            break;
        case ImportDialog.ONLYATTACH:
            radioButtononlyAttachPDF.setSelected(true);
            break;
        default:
            // fallback
            radioButtonPDFcontent.setSelected(true);
            break;
        }
        fileNamePattern.setText(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));
        fileDirPattern.setText(prefs.get(JabRefPreferences.IMPORT_FILEDIRPATTERN));
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.IMPORT_ALWAYSUSE, useDefaultPDFImportStyle.isSelected());
        int style = ImportSettingsTab.DEFAULT_STYLE;
        if (radioButtonNoMeta.isSelected()) {
            style = ImportDialog.NOMETA;
        } else if (radioButtonXmp.isSelected()) {
            style = ImportDialog.XMP;
        } else if (radioButtonPDFcontent.isSelected()) {
            style = ImportDialog.CONTENT;
        } else if (radioButtononlyAttachPDF.isSelected()) {
            style = ImportDialog.ONLYATTACH;
        }
        prefs.putInt(JabRefPreferences.IMPORT_DEFAULT_PDF_IMPORT_STYLE, style);
        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, fileNamePattern.getText());
        prefs.put(JabRefPreferences.IMPORT_FILEDIRPATTERN, fileDirPattern.getText());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import");
    }

    private void openFilePatternMenu() {
        JPopupMenu popup = new JPopupMenu();
        for (int i = 0; i < ImportSettingsTab.DEFAULT_FILENAMEPATTERNS.length; i++) {
            final JMenuItem item = new JMenuItem(ImportSettingsTab.DEFAULT_FILENAMEPATTERNS_DISPLAY[i]);
            final String toSet = ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[i];
            item.addActionListener(e -> fileNamePattern.setText(toSet));
            popup.add(item);
        }
        popup.show(selectFileNamePattern, 0, selectFileNamePattern.getHeight());
    }
}

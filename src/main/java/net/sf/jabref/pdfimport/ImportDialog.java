/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.pdfimport;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.preftabs.ImportSettingsTab;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ImportDialog extends JDialog {

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
        Boolean targetIsARow1 = targetIsARow;
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        setContentPane(contentPane);
        JPanel panel3 = new JPanel();
        panel3.setBackground(new Color(-1643275));
        JLabel labelHeadline = new JLabel(Localization.lang("Import_metadata_from:"));
        labelHeadline.setFont(new Font(labelHeadline.getFont().getName(), Font.BOLD, 14));
        JLabel labelSubHeadline = new JLabel(Localization.lang("Choose_the_source_for_the_metadata_import"));
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
        radioButtonNoMeta = new JRadioButton(Localization.lang("Create_blank_entry_linking_the_PDF"));
        radioButtonXmp = new JRadioButton(Localization.lang("Create_entry_based_on_XMP_data"));
        radioButtonPDFcontent = new JRadioButton(Localization.lang("Create_entry_based_on_content"));
        radioButtononlyAttachPDF = new JRadioButton(Localization.lang("Only_attach_PDF"));
        JButton buttonOK = new JButton(Localization.lang("OK"));
        JButton buttonCancel = new JButton(Localization.lang("Cancel"));
        checkBoxDoNotShowAgain = new JCheckBox(Localization.lang("Do not show this box again for this import"));
        useDefaultPDFImportStyle = new JCheckBox(Localization.lang("Always use this PDF import style (and do not ask for each import)"));
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref, 5dlu, left:pref:grow", ""));
        b.appendSeparator(Localization.lang("Create New Entry"));
        b.append(radioButtonNoMeta, 3);
        b.append(radioButtonXmp, 3);
        b.append(radioButtonPDFcontent, 3);
        b.appendSeparator(Localization.lang("Update_Existing_Entry"));
        b.append(radioButtononlyAttachPDF, 3);
        b.nextLine();
        b.append(checkBoxDoNotShowAgain);
        b.append(useDefaultPDFImportStyle);
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
        this.setTitle(Localization.lang("Import_Metadata_From_PDF"));

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

        switch (Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE)) {
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

    private void onOK() {
        this.result = JOptionPane.OK_OPTION;
        Globals.prefs.putInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, this.getChoice());
        if (useDefaultPDFImportStyle.isSelected()) {
            Globals.prefs.putBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE, true);
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

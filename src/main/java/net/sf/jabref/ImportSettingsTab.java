/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import spl.gui.ImportDialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportSettingsTab extends JPanel implements PrefsTab {

    public final static String PREF_IMPORT_ALWAYSUSE = "importAlwaysUsePDFImportStyle";
    public final static String PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE = "importDefaultPDFimportStyle";
    public final static int DEFAULT_STYLE = ImportDialog.CONTENT;

    public final static String PREF_IMPORT_FILENAMEPATTERN = "importFileNamePattern";
    private final static String[] DEFAULT_FILENAMEPATTERNS_DISPLAY = new String[] {
            "bibtexkey",
            "bibtexkey - title",
    };
    public final static String[] DEFAULT_FILENAMEPATTERNS = new String[] {
            "\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"
    };

    private final JRadioButton radioButtonXmp;
    private final JRadioButton radioButtonPDFcontent;
    private final JRadioButton radioButtonNoMeta;
    private final JRadioButton radioButtononlyAttachPDF;
    private final JCheckBox useDefaultPDFImportStyle;

    private final JTextField fileNamePattern;
    private final JButton selectFileNamePattern;


    public ImportSettingsTab() {
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu");
        radioButtonNoMeta = new JRadioButton(Globals.lang("Create_blank_entry_linking_the_PDF"));
        radioButtonXmp = new JRadioButton(Globals.lang("Create_entry_based_on_XMP_data"));
        radioButtonPDFcontent = new JRadioButton(Globals.lang("Create_entry_based_on_content"));
        radioButtononlyAttachPDF = new JRadioButton(Globals.lang("Only_attach_PDF"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioButtonNoMeta);
        bg.add(radioButtonXmp);
        bg.add(radioButtonPDFcontent);
        bg.add(radioButtononlyAttachPDF);

        useDefaultPDFImportStyle = new JCheckBox(Globals.lang("Always use this PDF import style (and do not ask for each import)"));

        fileNamePattern = new JTextField(50);
        selectFileNamePattern = new JButton(Globals.lang("Choose pattern"));
        selectFileNamePattern.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openFilePatternMenu();
            }
        });

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();

        builder.appendSeparator(Globals.lang("Default import style for drag&drop of PDFs"));
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

        builder.appendSeparator(Globals.lang("Default PDF file link action"));
        builder.nextLine();
        builder.append(pan);
        JPanel pan2 = new JPanel();
        JLabel lab = new JLabel(Globals.lang("File name format pattern").concat(":"));
        pan2.add(lab);
        pan2.add(fileNamePattern);
        pan2.add(selectFileNamePattern);
        builder.append(pan2);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        useDefaultPDFImportStyle.setSelected(Globals.prefs.getBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE));
        int style = Globals.prefs.getInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);
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
        fileNamePattern.setText(Globals.prefs.get(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN));
    }

    @Override
    public void storeSettings() {
        Globals.prefs.putBoolean(ImportSettingsTab.PREF_IMPORT_ALWAYSUSE, useDefaultPDFImportStyle.isSelected());
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
        Globals.prefs.putInt(ImportSettingsTab.PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, style);
        Globals.prefs.put(ImportSettingsTab.PREF_IMPORT_FILENAMEPATTERN, fileNamePattern.getText());
    }

    @Override
    public boolean readyToClose() {
        return true;
    }

    @Override
    public String getTabName() {
        return Globals.lang("Import");
    }

    private void openFilePatternMenu() {
        JPopupMenu popup = new JPopupMenu();
        for (int i = 0; i < ImportSettingsTab.DEFAULT_FILENAMEPATTERNS.length; i++) {
            final JMenuItem item = new JMenuItem(ImportSettingsTab.DEFAULT_FILENAMEPATTERNS_DISPLAY[i]);
            final String toSet = ImportSettingsTab.DEFAULT_FILENAMEPATTERNS[i];
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    fileNamePattern.setText(toSet);
                }
            });
            popup.add(item);
        }
        popup.show(selectFileNamePattern, 0, selectFileNamePattern.getHeight());
    }
}

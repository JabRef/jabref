package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import spl.gui.ImportDialog;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportSettingsTab extends JPanel implements PrefsTab {

	public final static String PREF_IMPORT_ALWAYSUSE = "importAlwaysUsePDFImportStyle";
	public final static String PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE = "importDefaultPDFimportStyle";
	public final static int DEFAULT_STYLE = ImportDialog.CONTENT; 
	
	public final static String PREF_IMPORT_FILENAMEPATTERN = "importFileNamePattern"; 
	public final static String DEFAULT_FILENAMEPATTERN = "\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}";
	
    private JRadioButton radioButtonXmp;
	private JRadioButton radioButtonPDFcontent;
    private JRadioButton radioButtonMrDlib;
    private JRadioButton radioButtonNoMeta;
	private JRadioButton radioButtononlyAttachPDF;
	private JRadioButton radioButtonUpdateEmptyFields;
	private JCheckBox useDefaultPDFImportStyle;
	
	private JTextField fileNamePattern;
	private JButton defaultFileNamePattern;

	public ImportSettingsTab() {
        setLayout(new BorderLayout());
        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu");
        radioButtonNoMeta = new JRadioButton(Globals.lang("Create_blank_entry_linking_the_PDF"));
        radioButtonXmp = new JRadioButton(Globals.lang("Create_entry_based_on_XMP_data"));
        radioButtonPDFcontent = new JRadioButton(Globals.lang("Create_entry_based_on_content"));
        radioButtonMrDlib = new JRadioButton(Globals.lang("Create_entry_based_on_data_fetched_from"));
        radioButtononlyAttachPDF = new JRadioButton(Globals.lang("Only_attach_PDF"));
        radioButtonUpdateEmptyFields = new JRadioButton(Globals.lang("Update_empty_fields_with_data_fetched_from"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioButtonNoMeta);
        bg.add(radioButtonXmp);
        bg.add(radioButtonPDFcontent);
        bg.add(radioButtonMrDlib);
        bg.add(radioButtononlyAttachPDF);
        bg.add(radioButtonUpdateEmptyFields);

        useDefaultPDFImportStyle = new JCheckBox(Globals.lang("Always use this PDF import style (and do not ask for each import)"));
		
        fileNamePattern = new JTextField(50);
        defaultFileNamePattern = new JButton(Globals.lang("Revert to default"));
        defaultFileNamePattern.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileNamePattern.setText(DEFAULT_FILENAMEPATTERN);
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
		builder.append(radioButtonMrDlib);
        builder.nextLine();
        builder.append(pan);
		builder.append(radioButtononlyAttachPDF);
        builder.nextLine();
        builder.append(pan);
		builder.append(radioButtonUpdateEmptyFields);
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
        pan2.add(defaultFileNamePattern);
        builder.append(pan2);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
	}
	
	@Override
	public void setValues() {
		useDefaultPDFImportStyle.setSelected(Globals.prefs.getBoolean(PREF_IMPORT_ALWAYSUSE));
		int style = Globals.prefs.getInt(PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE);
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
		case ImportDialog.MRDLIB:
			radioButtonMrDlib.setSelected(true);
			break;
		case ImportDialog.ONLYATTACH:
			radioButtononlyAttachPDF.setSelected(true);
			break;
		case ImportDialog.UPDATEEMPTYFIELDS:
			radioButtonUpdateEmptyFields.setSelected(true);
			break;
		default:
			// fallback
			radioButtonPDFcontent.setSelected(true);
			break;
		}
		fileNamePattern.setText(Globals.prefs.get(PREF_IMPORT_FILENAMEPATTERN));
	}

	@Override
	public void storeSettings() {
		Globals.prefs.putBoolean(PREF_IMPORT_ALWAYSUSE, useDefaultPDFImportStyle.isSelected());
		int style = DEFAULT_STYLE;
		if (radioButtonNoMeta.isSelected())
			style = ImportDialog.NOMETA;
		else if (radioButtonXmp.isSelected())
			style = ImportDialog.XMP;
		else if (radioButtonPDFcontent.isSelected())
			style = ImportDialog.CONTENT;
		else if (radioButtonMrDlib.isSelected())
			style = ImportDialog.MRDLIB;
		else if (radioButtononlyAttachPDF.isSelected())
			style = ImportDialog.ONLYATTACH;
		else if (radioButtonUpdateEmptyFields.isSelected())
			style = ImportDialog.UPDATEEMPTYFIELDS;
		Globals.prefs.putInt(PREF_IMPORT_DEFAULT_PDF_IMPORT_STYLE, style);
		Globals.prefs.put(PREF_IMPORT_FILENAMEPATTERN, fileNamePattern.getText());
	}

	@Override
	public boolean readyToClose() {
		return true;
	}

	@Override
	public String getTabName() {
		return Globals.lang("Import");
	}

}

package spl.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import net.sf.jabref.ImportSettingsTab;
import net.sf.jabref.JabRefPreferences;
import spl.listener.LabelLinkListener;
import spl.localization.LocalizationSupport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ResourceBundle;

public class ImportDialog extends JDialog {
	public final static int NOMETA = 0;
	public final static int XMP = 1;
	public final static int CONTENT = 2;
	public final static int MRDLIB = 3;
	public final static int ONLYATTACH = 4;
	public final static int UPDATEEMPTYFIELDS = 5;
	
    private JPanel contentPane;
    private JLabel labelSubHeadline;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox checkBoxDoNotShowAgain;
   	private JCheckBox useDefaultPDFImportStyle;
    private JRadioButton radioButtonXmp;
	private JRadioButton radioButtonPDFcontent;
    private JRadioButton radioButtonMrDlib;
    private JRadioButton radioButtonNoMeta;
    private JLabel labelHeadline;
    private JLabel labelFileName;
    private JRadioButton radioButtononlyAttachPDF;
    private JRadioButton radioButtonUpdateEmptyFields;
    private JLabel labelMrDlib1;
    private JLabel labelMrDlib2;
    private int result;
    private int dropRow;
    private String fileName;
    
    public ImportDialog(int dropRow, String fileName) {
        this.dropRow = dropRow;
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        setContentPane(contentPane);
        JPanel panel3 = new JPanel();
        panel3.setBackground(new Color(-1643275));
        labelHeadline = new JLabel(Globals.lang("Import_Metadata_from:"));
        labelHeadline.setFont(new Font(labelHeadline.getFont().getName(), Font.BOLD, 14));
        labelSubHeadline = new JLabel(Globals.lang("Choose_the_source_for_the_metadata_import"));
        labelSubHeadline.setFont(new Font(labelSubHeadline.getFont().getName(), labelSubHeadline.getFont().getStyle(), 13));
        labelFileName = new JLabel();
        labelFileName.setFont(new Font(labelHeadline.getFont().getName(), Font.BOLD, 14));
        JPanel headLinePanel = new JPanel();
        headLinePanel.add(labelHeadline);
        headLinePanel.add(labelFileName);
        headLinePanel.setBackground(new Color(-1643275));
        GridLayout gl = new GridLayout(2,1);
        gl.setVgap(10);
        gl.setHgap(10);
        panel3.setLayout(gl);
        panel3.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel3.add(headLinePanel);
        panel3.add(labelSubHeadline);
        radioButtonNoMeta = new JRadioButton(Globals.lang("Create_blank_entry_linking_the_PDF"));
        radioButtonXmp = new JRadioButton(Globals.lang("Create_entry_based_on_XMP_data"));
        radioButtonPDFcontent = new JRadioButton(Globals.lang("Create_entry_based_on_content"));
        radioButtonMrDlib = new JRadioButton(Globals.lang("Create_entry_based_on_data_fetched_from"));
        radioButtononlyAttachPDF = new JRadioButton(Globals.lang("Only_attach_PDF"));
        radioButtonUpdateEmptyFields = new JRadioButton(Globals.lang("Update_empty_fields_with_data_fetched_from"));
        labelMrDlib1 = new JLabel("Mr._dLib");
        labelMrDlib1.setFont(new Font(labelMrDlib1.getFont().getName(), Font.BOLD, 13));
        labelMrDlib1.setForeground(new Color(-16776961));
        labelMrDlib2 = new JLabel("Mr._dLib");
        labelMrDlib2.setFont(new Font(labelMrDlib1.getFont().getName(), Font.BOLD, 13));
        labelMrDlib2.setForeground(new Color(-16776961));
        buttonOK = new JButton(Globals.lang("Ok"));
        buttonCancel = new JButton(Globals.lang("Cancel"));
        checkBoxDoNotShowAgain = new JCheckBox(Globals.lang("Do not show this box again for this import"));
        useDefaultPDFImportStyle = new JCheckBox(Globals.lang("Always use this PDF import style (and do not ask for each import)"));
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref, 5dlu, left:pref:grow",""));
        b.appendSeparator(Globals.lang("Create New Entry"));
        b.append(radioButtonNoMeta, 3);
        b.append(radioButtonXmp, 3);
        b.append(radioButtonPDFcontent, 3);
        b.append(radioButtonMrDlib);
        b.append(labelMrDlib1);
        b.appendSeparator(Globals.lang("Update_Existing_Entry"));
        b.append(radioButtononlyAttachPDF, 3);
        b.append(radioButtonUpdateEmptyFields);
        b.append(labelMrDlib2);
        b.nextLine();
        b.append(checkBoxDoNotShowAgain);
        b.append(useDefaultPDFImportStyle);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(buttonOK);
        bb.addButton(buttonCancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        
        contentPane.add(panel3, BorderLayout.NORTH);
        contentPane.add(b.getPanel(), BorderLayout.CENTER);
        contentPane.add(bb.getPanel(), BorderLayout.SOUTH);

        //$$$setupUI$$$();
        //this.setText();
        if (this.dropRow < 0) {
            this.radioButtononlyAttachPDF.setEnabled(false);
            this.radioButtonUpdateEmptyFields.setEnabled(false);
            this.labelMrDlib2.setEnabled(false);
        }
        this.fileName = fileName;
        String name = new File(this.fileName).getName();
        if (name.length() < 34) {
            this.labelFileName.setText(name);
        } else {
            this.labelFileName.setText(new File(this.fileName).getName().substring(0, 33) + "...");
        }
        this.labelMrDlib1.addMouseListener(new LabelLinkListener(this.labelMrDlib1, "www.mr-dlib.org/docs/pdf_metadata_extraction.php"));
        this.labelMrDlib2.addMouseListener(new LabelLinkListener(this.labelMrDlib2, "www.mr-dlib.org/docs/pdf_metadata_extraction.php"));
        this.setTitle(LocalizationSupport.message("Import_Metadata_From_PDF"));

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        
        // only one of the radio buttons may be selected.
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioButtonNoMeta);
        bg.add(radioButtonXmp);
        bg.add(radioButtonPDFcontent);
        bg.add(radioButtonMrDlib);
        bg.add(radioButtononlyAttachPDF);
        bg.add(radioButtonUpdateEmptyFields);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

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
		case MRDLIB:
			radioButtonMrDlib.setSelected(true);
			break;
		case ONLYATTACH:
			radioButtononlyAttachPDF.setSelected(true);
			break;
		case UPDATEEMPTYFIELDS:
			radioButtonUpdateEmptyFields.setSelected(true);
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
    	if (radioButtonXmp.isSelected())
    		return XMP;
    	else if (radioButtonPDFcontent.isSelected())
    		return CONTENT;
    	else if (radioButtonMrDlib.isSelected())
    		return MRDLIB;
    	else if (radioButtonNoMeta.isSelected())
    		return NOMETA;
    	else if (radioButtononlyAttachPDF.isSelected())
    		return ONLYATTACH;
    	else if (radioButtonUpdateEmptyFields.isSelected())
    		return UPDATEEMPTYFIELDS;
    	else throw new IllegalStateException();
    }

    public boolean getDoNotShowAgain() {
    	return this.checkBoxDoNotShowAgain.isSelected();
    }

    public int getResult() {
        return result;
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

	public void disableXMPChoice() {
		this.radioButtonXmp.setEnabled(false);
	}
}

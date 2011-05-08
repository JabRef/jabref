package spl.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import spl.listener.LabelLinkListener;
import spl.localization.LocalizationSupport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ResourceBundle;

public class ImportDialog extends JDialog {
    private JPanel contentPane;
    private JLabel labelSubHeadline;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton radioButtonXmp;
    private JRadioButton radioButtonMrDlib;
    private JRadioButton radioButtonNoMeta;
    private JLabel labelHeadline;
    private JLabel labelFileName;
    private JRadioButton radioButtononlyAttachPDF;
    private JRadioButton radioButtonUpdateEmptyFields;
    private JPanel panelUpdateEntry;
    private JLabel labelMrDlib1;
    private JLabel labelMrDlib2;
    private JPanel panelNewEntry;
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
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, left:pref:grow",""));
        b.appendSeparator(Globals.lang("Create New Entry"));
        b.append(radioButtonNoMeta, 3);
        b.append(radioButtonXmp, 3);
        b.append(radioButtonMrDlib);
        b.append(labelMrDlib1);
        b.appendSeparator(Globals.lang("Update_Existing_Entry"));
        b.append(radioButtononlyAttachPDF, 3);
        b.append(radioButtonUpdateEmptyFields);
        b.append(labelMrDlib2);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(buttonOK);
        bb.addGridded(buttonCancel);
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

        radioButtonXmp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onXmp();
            }
        });

        radioButtonMrDlib.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onMrDlib();
            }
        });

        radioButtonNoMeta.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onNoMeta();
            }
        });

        radioButtononlyAttachPDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAttachPDF();
            }
        });

        radioButtonUpdateEmptyFields.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onUpdateEntry();
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

        this.radioButtonMrDlib.setSelected(true);
        this.radioButtonMrDlib.requestFocus();
        this.setSize(555, 371);
    }

    private void onOK() {
        this.result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        this.result = JOptionPane.CANCEL_OPTION;
        dispose();
    }

    private void onXmp() {
        this.setSelection(this.radioButtonXmp);
    }

    private void onAttachPDF() {
        this.setSelection(this.radioButtononlyAttachPDF);
    }

    private void onUpdateEntry() {
        this.setSelection(this.radioButtonUpdateEmptyFields);
    }

    private void onMrDlib() {
        this.setSelection(this.radioButtonMrDlib);
    }

    private void onNoMeta() {
        this.setSelection(this.radioButtonNoMeta);
    }

    private void setSelection(JRadioButton button) {
        if (button != this.radioButtonMrDlib) {
            this.radioButtonMrDlib.setSelected(false);
        }
        if (button != this.radioButtonUpdateEmptyFields) {
            this.radioButtonUpdateEmptyFields.setSelected(false);
        }
        if (button != this.radioButtononlyAttachPDF) {
            this.radioButtononlyAttachPDF.setSelected(false);
        }
        if (button != this.radioButtonXmp) {
            this.radioButtonXmp.setSelected(false);
        }
        if (button != this.radioButtonNoMeta) {
            this.radioButtonNoMeta.setSelected(false);
        }
    }

    public void showDialog() {
        this.pack();
        this.setVisible(true);
    }

    public JRadioButton getRadioButtonXmp() {
        return radioButtonXmp;
    }

    public JRadioButton getRadioButtonMrDlib() {
        return radioButtonMrDlib;
    }

    public JRadioButton getRadioButtonNoMeta() {
        return radioButtonNoMeta;
    }

    public JRadioButton getRadioButtononlyAttachPDF() {
        return radioButtononlyAttachPDF;
    }

    public JRadioButton getRadioButtonUpdateEmptyFields() {
        return radioButtonUpdateEmptyFields;
    }

    public int getResult() {
        return result;
    }

    private void setText() {
        this.labelHeadline.setText(LocalizationSupport.message("Import_Metadata_from:"));
        this.labelSubHeadline.setText(LocalizationSupport.message("Choose_the_source_for_the_metadata_import"));
        this.buttonOK.setText(LocalizationSupport.message("Ok"));
        this.buttonCancel.setText(LocalizationSupport.message("Cancel"));
        this.radioButtonXmp.setText(LocalizationSupport.message("Create_entry_based_on_XMP_data"));
        this.radioButtonUpdateEmptyFields.setText(LocalizationSupport.message("Update_empty_fields_with_data_fetched_from"));
        this.radioButtonMrDlib.setText(LocalizationSupport.message("Create_entry_based_on_data_fetched_from"));
        this.radioButtonNoMeta.setText(LocalizationSupport.message("Create_blank_entry_linking_the_PDF"));
        this.radioButtononlyAttachPDF.setText(LocalizationSupport.message("Only_attach_PDF"));
        this.labelMrDlib1.setText(LocalizationSupport.message("Mr._dLib"));
        this.labelMrDlib2.setText(LocalizationSupport.message("Mr._dLib"));
        this.panelNewEntry.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), LocalizationSupport.message("Create_New_Entry"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(panelNewEntry.getFont().getName(), panelNewEntry.getFont().getStyle(), 12), new Color(-16777216)));
        panelUpdateEntry.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), LocalizationSupport.message("Update_Existing_Entry"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font(panelUpdateEntry.getFont().getName(), panelUpdateEntry.getFont().getStyle(), 12), new Color(-16777216)));
    }


    private void createUIComponents() {
    }




    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}

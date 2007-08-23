package net.sf.jabref.external;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class produces a dialog box for editing an external file type.
 */
public class ExternalFileTypeEntryEditor {

    JFrame fParent = null;
    JDialog dParent = null;
    JDialog diag;
    JTextField extension = new JTextField(),
        name = new JTextField(),
        application = new JTextField();
    String selectedIcon = null;
    JButton icon = new JButton(GUIGlobals.getImage("picture"));
    JButton ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));
    JRadioButton useDefault = new JRadioButton(Globals.lang("Default")),
        other = new JRadioButton("");
    final String emptyMessage = "<"+Globals.lang("Use default viewer")+">";
    boolean applicationFieldEmpty = false;

    private ExternalFileType entry;
    private boolean okPressed = false;

    public ExternalFileTypeEntryEditor(JFrame parent, ExternalFileType entry) {
        fParent = parent;
        init(entry);
    }

    public ExternalFileTypeEntryEditor(JDialog parent, ExternalFileType entry) {
        dParent = parent;
        init(entry);
    }

    private void init(ExternalFileType entry) {
        this.entry = entry;
        icon.setText(null);

        ButtonGroup bg = new ButtonGroup();
        bg.add(useDefault);
        bg.add(other);

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", ""));
        builder.append(Globals.lang("Icon"));
        builder.append(icon);
        builder.nextLine();
        builder.append(Globals.lang("Name"));
        builder.append(name);
        builder.nextLine();
        builder.append(Globals.lang("Extension"));
        builder.append(extension);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        builder.nextLine();
        builder.append(Globals.lang("Application"));
        JButton browseBut = new JButton(Globals.lang("Browse"));
        if (Globals.ON_WIN) {
            builder.append(useDefault);
            builder.nextLine();
            JPanel p1 = new JPanel();
            builder.append(p1);
            JPanel p2 = new JPanel();
            DefaultFormBuilder b2 = new DefaultFormBuilder(
                    new FormLayout("left:pref, 4dlu, fill:pref", ""));
            application.setPreferredSize(new Dimension(300, application.getPreferredSize().height));
            BorderLayout bl = new BorderLayout();
            bl.setHgap(4);
            //b2.append(other);
            //b2.append(application);
            p2.setLayout(bl);
            p2.add(other, BorderLayout.WEST);
            p2.add(application, BorderLayout.CENTER);
            builder.append(p2);
            //builder.append(b2.getPanel());
            builder.append(browseBut);
        } else {
            builder.append(application);
            builder.append(browseBut);
        }
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
                storeSettings(ExternalFileTypeEntryEditor.this.entry);
                okPressed = true;
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });

        icon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String initSel = ExternalFileTypeEntryEditor.this.entry.getIconName();
                if (selectedIcon != null)
                    initSel = selectedIcon;
                IconSelection ic = new IconSelection(diag, initSel);
                ic.setVisible(true);
                if (ic.isOkPressed()) {
                    selectedIcon = ic.getSelectedIconKey();
                    icon.setIcon(GUIGlobals.getImage(selectedIcon));
                }
                //JOptionPane.showMessageDialog(null, "Sorry, the icon can unfortunately not be changed in this version of JabRef");
            }
        });

        if (Globals.ON_WIN) {
            application.getDocument().addDocumentListener(new DocumentListener() {
                private void handle(DocumentEvent e) {
                    if (application.getText().length() == 0) {
                        useDefault.setSelected(true);
                    } else {
                        other.setSelected(true);
                    }
                }
                public void insertUpdate(DocumentEvent e) {
                    handle(e);
                }

                public void removeUpdate(DocumentEvent documentEvent) {
                    handle(documentEvent);
                }

                public void changedUpdate(DocumentEvent documentEvent) {
                    handle(documentEvent);
                }
            });
        }

        if (dParent != null)
            diag = new JDialog(dParent, Globals.lang("Edit file type"), true);
        else
            diag = new JDialog(fParent, Globals.lang("Edit file type"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();

        BrowseListener browse = new BrowseListener(diag, application);
        browseBut.addActionListener(browse);

        if (dParent != null)
            diag.setLocationRelativeTo(dParent);
        else
            diag.setLocationRelativeTo(fParent);
        //Util.placeDialog(diag, parent);

        setValues(entry);
    }

    public void setEntry(ExternalFileType entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible) {
        if (visible)
            okPressed = false;
        diag.setVisible(visible);
    }

    public void setValues(ExternalFileType entry) {
        name.setText(entry.getName());
        extension.setText(entry.getExtension());
        application.setText(entry.getOpenWith());
        icon.setIcon(entry.getIcon());
        if (true && (application.getText().length() == 0))
            useDefault.setSelected(true);
        else
            other.setSelected(true);
        selectedIcon = null;
    }

    public void storeSettings(ExternalFileType entry) {
        entry.setName(name.getText().trim());
        entry.setExtension(extension.getText().trim());
        if (selectedIcon != null)
            entry.setIconName(selectedIcon);
        if (!Globals.ON_WIN) {
            entry.setOpenWith(application.getText().trim());
        } else {
            // On Windows, store application as empty if the "Default" option is selected,
            // or if the application name is empty:
            if (useDefault.isSelected() || (application.getText().trim().length() == 0))
                entry.setOpenWith("");
            else
                entry.setOpenWith(application.getText().trim());
        }
    }

    public boolean okPressed() {
        return okPressed;
    }

    class BrowseListener implements ActionListener {
        private JTextField comp;

        public BrowseListener(JDialog parent, JTextField comp) {
            this.comp = comp;
        }

        public void actionPerformed(ActionEvent e) {
            File initial = new File(comp.getText().trim());
            if (comp.getText().trim().length() == 0) {
                // Nothing in the field. Go to the last file dir used:
                initial = new File(Globals.prefs.get("fileWorkingDirectory"));
            }
            String chosen = Globals.getNewFile(/*parent*/null, initial, Globals.NONE,
                JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                // Store the directory for next time:
                Globals.prefs.put("fileWorkingDirectory", newFile.getParent());
                comp.setText(newFile.getPath());
                comp.requestFocus();
            }
        }
    }
}

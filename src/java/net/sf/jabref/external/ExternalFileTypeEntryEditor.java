package net.sf.jabref.external;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.*;
import java.io.File;

/**
 * This class produces a dialog box for editing an external file type.
 */
public class ExternalFileTypeEntryEditor {

    JDialog diag;
    JTextField extension = new JTextField(),
        name = new JTextField(),
        application = new JTextField();
    JButton icon = new JButton(GUIGlobals.getImage("picture"));
    JButton ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));


    private ExternalFileType entry;
    private boolean okPressed = false;

    public ExternalFileTypeEntryEditor(JDialog parent, ExternalFileType entry) {
        this.entry = entry;

        icon.setText(null);

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
        builder.append(application);
        BrowseListener browse = new BrowseListener(parent, application);
        JButton browseBut = new JButton(Globals.lang("Browse"));
        browseBut.addActionListener(browse);
        builder.append(browseBut);

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

        diag = new JDialog(parent, Globals.lang("Edit file type"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        Util.placeDialog(diag, parent);

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
    }

    public void storeSettings(ExternalFileType entry) {
        entry.setName(name.getText().trim());
        entry.setExtension(extension.getText().trim());
        entry.setOpenWith(application.getText().trim());
    }

    public boolean okPressed() {
        return okPressed;
    }

    class BrowseListener implements ActionListener {
        private JDialog parent;
        private JTextField comp;

        public BrowseListener(JDialog parent, JTextField comp) {
            this.parent = parent;
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

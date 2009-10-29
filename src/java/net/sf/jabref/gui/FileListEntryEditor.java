package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;
import net.sf.jabref.external.ConfirmCloseFileListEntryEditor;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;


/**
 * This class produces a dialog box for editing a single file link from a Bibtex entry.
 *
 * The information to be edited includes the file description, the link itself and the
 * file type. The dialog also includes convenience buttons for quick linking.
 *
 * For use when downloading files, this class also offers a progress bar and a "Downloading..."
 * label that can be hidden when the download is complete.
 */
public class FileListEntryEditor {

    JDialog diag;
    JTextField link = new JTextField(), description = new JTextField();
    JButton ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel")),
            open = new JButton(Globals.lang("Open"));


    JComboBox types;
    JProgressBar prog = new JProgressBar(JProgressBar.HORIZONTAL);
    JLabel downloadLabel = new JLabel(Globals.lang("Downloading..."));
    ConfirmCloseFileListEntryEditor externalConfirm = null;

    private AbstractAction okAction;
    private FileListEntry entry;
    private MetaData metaData;
    private boolean okPressed = false, okDisabledExternally = false,
            openBrowseWhenShown = false, dontOpenBrowseUntilDisposed = false;

    public static Pattern remoteLinkPattern = Pattern.compile("[a-z]+://.*");

    public FileListEntryEditor(JabRefFrame frame, FileListEntry entry, boolean showProgressBar,
                               boolean showOpenButton, MetaData metaData) {
        this.entry = entry;
        this.metaData = metaData;

        okAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    // If OK button is disabled, ignore this event:
                    if (!ok.isEnabled())
                        return;
                    // If necessary, ask the external confirm object whether we are ready to close.
                    if (externalConfirm != null) {
                        // Construct an updated FileListEntry:
                        FileListEntry testEntry = new FileListEntry("", "", null);
                        storeSettings(testEntry);
                        if (!externalConfirm.confirmClose(testEntry))
                            return;
                    }
                    diag.dispose();
                    storeSettings(FileListEntryEditor.this.entry);
                    okPressed = true;
                }
            };
        types = new JComboBox();
        types.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemEvent) {
                if (!okDisabledExternally)
                    ok.setEnabled(types.getSelectedItem() != null);
            }
        });
        
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref, 4dlu, fill:pref", ""));
        builder.append(Globals.lang("Link"));
        builder.append(link);
        final BrowseListener browse = new BrowseListener(frame, link);
        final JButton browseBut = new JButton(Globals.lang("Browse"));
        browseBut.addActionListener(browse);
        builder.append(browseBut);
        if (showOpenButton)
            builder.append(open);
        builder.nextLine();
        builder.append(Globals.lang("Description"));
        builder.append(description, 3);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        builder.nextLine();
        builder.append(Globals.lang("File type"));
        builder.append(types, 3);
        if (showProgressBar) {
            builder.nextLine();
            builder.append(downloadLabel);
            builder.append(prog, 3);
        }
        
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        //bb.addGridded(open);
        //bb.addRelatedGap();
        bb.addRelatedGap();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();


        ok.addActionListener(okAction);
        // Add OK action to the two text fields to simplify entering:
        link.addActionListener(okAction);
        description.addActionListener(okAction);

        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                openFile();
            }
        });

        AbstractAction cancelAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    diag.dispose();
                }
            };
        cancel.addActionListener(cancelAction);


        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        link.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }
            public void removeUpdate(DocumentEvent documentEvent) {
            }
            public void changedUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }

        });


        diag = new JDialog(frame, Globals.lang("Edit file link"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        Util.placeDialog(diag, frame);
        diag.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                if (openBrowseWhenShown && !dontOpenBrowseUntilDisposed) {
                    dontOpenBrowseUntilDisposed = true;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            browse.actionPerformed(new ActionEvent(browseBut, 0, ""));
                        }
                    });
                }
            }

            @Override
            public void windowClosed(WindowEvent event) {
                dontOpenBrowseUntilDisposed = false;
            }
        });
        setValues(entry);
    }

    private void checkExtension() {
        if ((types.getSelectedIndex() == -1) &&
                (link.getText().trim().length() > 0)) {

            // Check if this looks like a remote link:
            if (remoteLinkPattern.matcher(link.getText()).matches()) {
                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("html");
                if (type != null) {
                    types.setSelectedItem(type);
                    return;
                }
            }

            // Try to guess the file type:
            String theLink = link.getText().trim();
            int index = theLink.lastIndexOf('.');
            if ((index >= 0) && (index < theLink.length()-1)) {

                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt
                        (theLink.substring(index+1));
                if (type != null)
                    types.setSelectedItem(type);
            }
        }
    }

    public void openFile() {
        ExternalFileType type = (ExternalFileType)types.getSelectedItem();
        if (type != null)
            try {
                Util.openExternalFileAnyFormat(metaData, link.getText(), type);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void setExternalConfirm(ConfirmCloseFileListEntryEditor eC) {
        this.externalConfirm = eC;
    }

    public void setOkEnabled(boolean enabled) {
        okDisabledExternally = !enabled;
        ok.setEnabled(enabled);
    }

    public JProgressBar getProgressBar() {
        return prog;
    }

    public JLabel getProgressBarLabel() {
        return downloadLabel;
    }

    public void setEntry(FileListEntry entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible, boolean openBrowse) {
        openBrowseWhenShown = openBrowse;
        if (visible)
            okPressed = false;
        diag.setVisible(visible);
    }

    public boolean isVisible() {
        return diag != null && diag.isVisible();
    }

    public void setValues(FileListEntry entry) {
        description.setText(entry.getDescription());
        link.setText(entry.getLink());
        //if (link.getText().length() > 0)
        //    checkExtension();
        types.setModel(new DefaultComboBoxModel(Globals.prefs.getExternalFileTypeSelection()));
        types.setSelectedIndex(-1);
        // See what is a reasonable selection for the type combobox:
        if ((entry.getType() != null) && !(entry.getType() instanceof UnknownExternalFileType))
            types.setSelectedItem(entry.getType());
        // If the entry has a link but not a file type, try to deduce the file type:
        else if ((entry.getLink() != null) && (entry.getLink().length() > 0)) {
            checkExtension();    
        }

    }

    public void storeSettings(FileListEntry entry) {
        entry.setDescription(description.getText().trim());
	// See if we should trim the file link to be relative to the file directory:
	try {
        String fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
        if ((fileDir == null) ||(fileDir.equals(""))) {
            entry.setLink(link.getText().trim());
        } else {
            String canPath = (new File(fileDir)).getCanonicalPath();
            File fl = new File(link.getText().trim());
            if (fl.isAbsolute()) {
                String flPath = fl.getCanonicalPath();
                if ((flPath.length() > canPath.length()) && (flPath.startsWith(canPath))) {
                    String relFileName = fl.getCanonicalPath().substring(canPath.length()+1);
                    entry.setLink(relFileName);
                } else
                    entry.setLink(link.getText().trim());
            }
            else entry.setLink(link.getText().trim());
        }
    } catch (java.io.IOException ex)
	{ 
		ex.printStackTrace();
		// Don't think this should happen, but set the file link directly as a fallback:
		entry.setLink(link.getText().trim());
	}
	
        entry.setType((ExternalFileType)types.getSelectedItem());
    }

    public boolean okPressed() {
        return okPressed;
    }

    class BrowseListener implements ActionListener {
        private JFrame parent;
        private JTextField comp;

        public BrowseListener(JFrame parent, JTextField comp) {
            this.parent = parent;
            this.comp = comp;
        }

        public void actionPerformed(ActionEvent e) {
            File initial = new File(comp.getText().trim());
            if (comp.getText().trim().length() == 0) {
                // Nothing in the field. Go to the last file dir used:
                initial = new File(Globals.prefs.get("fileWorkingDirectory"));
            }
            String chosen = FileDialogs.getNewFile(parent, initial, Globals.NONE,
                JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                // Store the directory for next time:
                Globals.prefs.put("fileWorkingDirectory", newFile.getParent());

                // If the file is below the file directory, make the path relative:
                ArrayList<File> dirs = new ArrayList<File>();
                String fileDir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                if (fileDir != null)
                    dirs.add(new File(fileDir));
                if (dirs.size() > 0) {
                    newFile = FileListEditor.relativizePath(newFile, dirs);
                }

                comp.setText(newFile.getPath());
                comp.requestFocus();
            }
        }
    }


}

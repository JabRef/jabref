package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.*;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 31, 2005
 * Time: 10:46:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabasePropertiesDialog extends JDialog {

    MetaData metaData;
    BasePanel panel = null;
    JComboBox encoding;
    JButton ok, cancel;
    JTextField fileDir = new JTextField(40),
            pdfDir = new JTextField(40), psDir = new JTextField(40);
    String oldFileVal="", oldPdfVal="", oldPsVal=""; // Remember old values to see if they are changed.
    JCheckBox protect = new JCheckBox(Globals.lang("Refuse to save the database before external changes have been reviewed."));
    boolean oldProtectVal = false;

    public DatabasePropertiesDialog(JFrame parent) {
        super(parent, Globals.lang("Database properties"), false);
        encoding = new JComboBox(Globals.ENCODINGS);
        ok = new JButton(Globals.lang("Ok"));
        cancel = new JButton(Globals.lang("Cancel"));
        init(parent);
    }

    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.metaData();
    }

    public final void init(JFrame parent) {

        JButton browseFile = new JButton(Globals.lang("Browse"));
        JButton browsePdf = new JButton(Globals.lang("Browse"));
        JButton browsePs = new JButton(Globals.lang("Browse"));
        browseFile.addActionListener(new BrowseAction(parent, fileDir, true));
        browsePdf.addActionListener(new BrowseAction(parent, pdfDir, true));
        browsePs.addActionListener(new BrowseAction(parent, psDir, true));

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, fill:pref", ""));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        builder.append(Globals.lang("Database encoding"));
        builder.append(encoding);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Override default file directories"));
        builder.nextLine();
        builder.append(Globals.lang("File directory"));
        builder.append(fileDir);
        builder.append(browseFile);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Override legacy file fields"));
		builder.append(new JLabel("<html>"+Globals.lang("Note that these settings are used for the legacy "
			+"<b>pdf</b> and <b>ps</b> fields only.<br>For most users, setting the <b>Main file directory</b> "
			+"above should be sufficient.")+"</html>"), 3);
		builder.nextLine();
        builder.append(Globals.lang("PDF directory"));
        builder.append(pdfDir);
        builder.append(browsePdf);
        builder.nextLine();
        builder.append(Globals.lang("PS directory"));
        builder.append(psDir);
        builder.append(browsePs);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Database protection"));
        builder.nextLine();
        builder.append(protect,3);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        AbstractAction closeAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        };
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                storeSettings();
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    public void setVisible(boolean visible) {
        if (visible)
            setValues();
        super.setVisible(visible);
    }

    public void setValues() {
        encoding.setSelectedItem(panel.getEncoding());

        Vector<String> fileD = metaData.getData(GUIGlobals.FILE_FIELD+"Directory");
        if (fileD == null)
            fileDir.setText("");
        else {
            // Better be a little careful about how many entries the Vector has:
            if (fileD.size() >= 1)
                fileDir.setText((fileD.get(0)).trim());
        }

        Vector<String> pdfD = metaData.getData("pdfDirectory");
        if (pdfD == null)
            pdfDir.setText("");
        else {
            // Better be a little careful about how many entries the Vector has:
            if (pdfD.size() >= 1)
                pdfDir.setText((pdfD.get(0)).trim());
        }

        Vector<String> psD = metaData.getData("psDirectory");
        if (psD == null)
            psDir.setText("");
        else {
            // Better be a little careful about how many entries the Vector has:
            if (psD.size() >= 1)
                psDir.setText((psD.get(0)).trim());
        }

        Vector<String> prot = metaData.getData(Globals.PROTECTED_FLAG_META);
        if (prot == null)
            protect.setSelected(false);
        else {
            if (prot.size() >= 1)
                protect.setSelected(Boolean.parseBoolean(prot.get(0)));
        }

        // Store original values to see if they get changed:
        oldFileVal = fileDir.getText();
        oldPdfVal = pdfDir.getText();
        oldPsVal = psDir.getText();
        oldProtectVal = protect.isSelected();
    }

    public void storeSettings() {
        String oldEncoding = panel.getEncoding();
        String newEncoding = (String)encoding.getSelectedItem();
        panel.setEncoding(newEncoding);

        Vector<String> dir = new Vector<String>(1);
        String text = fileDir.getText().trim();
        if (text.length() > 0) {
            dir.add(text);
            metaData.putData(GUIGlobals.FILE_FIELD+"Directory", dir);
        }
        else
            metaData.remove(GUIGlobals.FILE_FIELD+"Directory");

        dir = new Vector<String>(1);
        text = pdfDir.getText().trim();
        if (text.length() > 0) {
            dir.add(text);
            metaData.putData("pdfDirectory", dir);
        }
        else
            metaData.remove("pdfDirectory");

        dir = new Vector<String>(1);
        text = psDir.getText().trim();
        if (text.length() > 0) {
            dir.add(text);
            metaData.putData("psDirectory", dir);
        }
        else
            metaData.remove("psDirectory");

        if (protect.isSelected()) {
            dir = new Vector<String>(1);
            dir.add("true");
            metaData.putData(Globals.PROTECTED_FLAG_META, dir);
        }
        else
            metaData.remove(Globals.PROTECTED_FLAG_META);


        // See if any of the values have been modified:
        boolean changed = !newEncoding.equals(oldEncoding)
            || !oldPdfVal.equals(pdfDir.getText())
            || !oldPsVal.equals(psDir.getText())
            || (oldProtectVal != protect.isSelected());
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed)
            panel.markNonUndoableBaseChanged();
    }
}

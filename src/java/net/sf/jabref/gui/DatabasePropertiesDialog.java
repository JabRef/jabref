package net.sf.jabref.gui;

import net.sf.jabref.BasePanel;

import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.BrowseAction;

import javax.swing.*;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Vector;

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
    JTextField pdfDir = new JTextField(40), psDir = new JTextField(40);

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

        JButton browsePdf = new JButton(Globals.lang("Browse"));
        JButton browsePs = new JButton(Globals.lang("Browse"));
        browsePdf.addActionListener(new BrowseAction(parent, pdfDir, true));
        browsePs.addActionListener(new BrowseAction(parent, psDir, true));

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, fill:pref", ""));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        builder.append(Globals.lang("Database encoding"));
        builder.append(encoding);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Override default file directories"));
        builder.nextLine();
        builder.append(Globals.lang("PDF directory"));
        builder.append(pdfDir);
        builder.append(browsePdf);
        builder.nextLine();
        builder.append(Globals.lang("PS directory"));
        builder.append(psDir);
        builder.append(browsePs);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

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

        Vector pdfD = metaData.getData("pdfDirectory");
        if (pdfD == null)
            pdfDir.setText("");
        else {
            // Better be a little careful about how many entries the Vector has:
            if (pdfD.size() >= 1)
                pdfDir.setText(((String)pdfD.get(0)).trim());
        }

        Vector psD = metaData.getData("psDirectory");
        if (psD == null)
            psDir.setText("");
        else {
            // Better be a little careful about how many entries the Vector has:
            if (psD.size() >= 1)
                psDir.setText(((String)psD.get(0)).trim());
        }
    }

    public void storeSettings() {
        panel.setEncoding((String)encoding.getSelectedItem());

        Vector dir = new Vector(1);
        String text = pdfDir.getText().trim();
        if (text.length() > 0) {
            dir.add(text);
            metaData.putData("pdfDirectory", dir);
        }
        else
            metaData.remove("pdfDirectory");

        dir = new Vector(1);
        text = psDir.getText().trim();
        if (text.length() > 0) {
            dir.add(text);
            metaData.putData("psDirectory", dir);
        }
        else
            metaData.remove("psDirectory");
    }
}

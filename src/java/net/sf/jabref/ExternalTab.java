package net.sf.jabref;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.io.File;

import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;

public class ExternalTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    //private JComboBox language = new JComboBox(GUIGlobals.LANGUAGES.keySet().toArray());
    JTextField pdfDir, psDir, pdf, ps, html, lyx, winEdt, citeCommand;

    //private HelpAction ownerHelp, pdfHelp;


    public ExternalTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;
        _frame = frame;
        setLayout(new BorderLayout());

        //pdfHelp = new HelpAction(frame.helpDiag, GUIGlobals.pdfHelp,
        //        "Help", GUIGlobals.helpSmallIconFile);

        psDir = new JTextField(30);
        pdfDir = new JTextField(30);
        pdf = new JTextField(30);
        ps = new JTextField(30);
        html = new JTextField(30);
        lyx = new JTextField(30);
        winEdt = new JTextField(30);
        citeCommand = new JTextField(30);
        BrowseAction browse;

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:200dlu, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
                        "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Globals.lang("PDF links"));
        JPanel pan = new JPanel();
        builder.append(pan);
        JLabel lab = new JLabel(Globals.lang("Main PDF directory") + ":");
        builder.append(lab);
        builder.append(pdfDir);
        browse = new BrowseAction(pdfDir, true);
        builder.append(new JButton(browse));
        builder.nextLine();
        builder.appendSeparator(Globals.lang("PS links"));
        pan = new JPanel();
        builder.append(pan);
        lab = new JLabel(Globals.lang("Main PS directory") + ":");
        builder.append(lab);
        builder.append(psDir);
        browse = new BrowseAction(psDir, true);
        builder.append(new JButton(browse));
        builder.nextLine();
        builder.appendSeparator(Globals.lang("External programs"));


        builder.nextLine();
        lab = new JLabel(Globals.lang("Path to PDF viewer") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(pdf);
        browse = new BrowseAction(pdf, false);
        if (Globals.ON_WIN)
            browse.setEnabled(false);
        builder.append(new JButton(browse));
        builder.nextLine();
        lab = new JLabel(Globals.lang("Path to PS viewer") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(ps);
        browse = new BrowseAction(ps, false);
        if (Globals.ON_WIN)
            browse.setEnabled(false);
        builder.append(new JButton(browse));
        builder.nextLine();
        lab = new JLabel(Globals.lang("Path to HTML viewer") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(html);
        browse = new BrowseAction(html, false);
        if (Globals.ON_WIN)
            browse.setEnabled(false);
        builder.append(new JButton(browse));
        builder.nextLine();
        lab = new JLabel(Globals.lang("Path to LyX pipe") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(lyx);
        browse = new BrowseAction(lyx, false);
        builder.append(new JButton(browse));
        builder.nextLine();
        lab = new JLabel(Globals.lang("Path to WinEdt.exe") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(winEdt);
        browse = new BrowseAction(winEdt, false);
        builder.append(new JButton(browse));
        builder.nextLine();
        builder.append(pan);
        builder.append(Globals.lang("Cite command (for Emacs/WinEdt)")+":");
        builder.append(citeCommand);
        //builder.appendSeparator();

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    /**
     * Action used to produce a "Browse" button for one of the text fields.
     */
    class BrowseAction extends AbstractAction {
        JTextField comp;
        boolean dir;

        public BrowseAction(JTextField tc, boolean dir) {
            super(Globals.lang("Browse"));
            this.dir = dir;
            comp = tc;
        }

        public void actionPerformed(ActionEvent e) {
            String chosen = null;
            if (dir)
                chosen = Globals.getNewDir(_frame, _prefs, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            else
                chosen = Globals.getNewFile(_frame, _prefs, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                comp.setText(newFile.getPath());
            }
        }
    }

    public void setValues() {
        pdfDir.setText(_prefs.get("pdfDirectory"));
        psDir.setText(_prefs.get("psDirectory"));
        if (!Globals.ON_WIN) {
            pdf.setText(_prefs.get("pdfviewer"));
            ps.setText(_prefs.get("psviewer"));
            html.setText(_prefs.get("htmlviewer"));
        } else {
            pdf.setText(Globals.lang("Uses default application"));
            ps.setText(Globals.lang("Uses default application"));
            html.setText(Globals.lang("Uses default application"));
            pdf.setEnabled(false);
            ps.setEnabled(false);
            html.setEnabled(false);
        }

        lyx.setText(_prefs.get("lyxpipe"));
        winEdt.setText(_prefs.get("winEdtPath"));
        citeCommand.setText(_prefs.get("citeCommand"));
    }

    public void storeSettings() {

        // We should maybe do some checking on the validity of the contents?
        _prefs.put("pdfDirectory", pdfDir.getText());
        _prefs.put("psDirectory", psDir.getText());
        _prefs.put("pdfviewer", pdf.getText());
        _prefs.put("psviewer", ps.getText());
        _prefs.put("htmlviewer", html.getText());
        _prefs.put("lyxpipe", lyx.getText());
        _prefs.put("winEdtPath", winEdt.getText());
        _prefs.put("citeCommand", citeCommand.getText());
    }

    public boolean readyToClose() {
        return true;
    }

}

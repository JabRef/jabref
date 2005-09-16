package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;
import net.sf.jabref.remote.RemoteListener;

public class AdvancedTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    JabRefFrame _frame;
    HelpDialog helpDiag;
    HelpAction remoteHelp;
    JPanel pan = new JPanel(),
        lnf = new JPanel();
    JLabel lab;
    JCheckBox useDefault, useRemoteServer;
    JTextField className, remoteServerPort;
    JButton def1 = new JButton(Globals.lang("Default")),
        def2 = new JButton(Globals.lang("Default"));
    JPanel p1 = new JPanel(),
        p2 = new JPanel();
    String oldLnf = "";
    boolean oldUseDef;
    int oldPort = -1;

    public AdvancedTab(JabRefPreferences prefs, HelpDialog diag) {
        _prefs = prefs;


    remoteHelp = new HelpAction(diag, GUIGlobals.remoteHelp, "Help",
                GUIGlobals.helpSmallIconFile);
    useDefault = new JCheckBox(Globals.lang("Use other look and feel"));
    useRemoteServer = new JCheckBox(Globals.lang("Listen for remote operation on port")+":");
    remoteServerPort = new JTextField();
    className = new JTextField(50);
    final JTextField clName = className;
    useDefault.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            clName.setEnabled(((JCheckBox)e.getSource()).isSelected());
        }
        });

    FormLayout layout = new FormLayout
        ("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu",//, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
         "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    JPanel pan = new JPanel();
    builder.appendSeparator(Globals.lang("Look and feel"));
    JLabel lab = new JLabel(Globals.lang("Default look and feel")+": "
             +(Globals.ON_WIN ? GUIGlobals.windowsDefaultLookAndFeel :
               GUIGlobals.linuxDefaultLookAndFeel));
    builder.nextLine();
    builder.append(pan);
    builder.append(lab);
    builder.nextLine();
    builder.append(pan);
    builder.append(useDefault);
    builder.nextLine();
    builder.append(pan);
    JPanel pan2 = new JPanel();
    lab = new JLabel(Globals.lang("Class name")+":");
    pan2.add(lab);
    pan2.add(className);
    builder.append(pan2);
    builder.nextLine();
    builder.append(pan);
    lab = new JLabel(Globals.lang("Note that you must specify the fully qualified class name for the look and feel,"));
    builder.append(lab);
    builder.nextLine();
    builder.append(pan);
    lab = new JLabel(Globals.lang("and the class must be available in your classpath next time you start JabRef."));
    builder.append(lab);
    builder.nextLine();
    builder.appendSeparator(Globals.lang("Remote operation"));
    builder.nextLine();
    builder.append(new JPanel());
    JPanel p = new JPanel();
    p.add(useRemoteServer);
    p.add(remoteServerPort);
    p.add(remoteHelp.getIconButton());
    builder.append(p);

    pan = builder.getPanel();
    pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    setLayout(new BorderLayout());
    add(pan, BorderLayout.CENTER);

    }

    public void setValues() {
    oldUseDef = _prefs.getBoolean("useDefaultLookAndFeel");
    oldLnf = _prefs.get("lookAndFeel");
    useDefault.setSelected(!oldUseDef);
    className.setText(oldLnf);
    className.setEnabled(!oldUseDef);
    useRemoteServer.setSelected(_prefs.getBoolean("useRemoteServer"));
    oldPort = _prefs.getInt("remoteServerPort");
    remoteServerPort.setText(String.valueOf(oldPort));
    }

    public void storeSettings() {
        _prefs.putBoolean("useDefaultLookAndFeel", !useDefault.isSelected());
        _prefs.put("lookAndFeel", className.getText());
        try {
            int port = Integer.parseInt(remoteServerPort.getText());
            if (port != oldPort) {
                _prefs.putInt("remoteServerPort", port);
                /*JOptionPane.showMessageDialog(null, Globals.lang("You have changed the menu and label font size. "
                        + "You must restart JabRef for this to come into effect."), Globals.lang("Changed font settings"),
                        JOptionPane.WARNING_MESSAGE);*/
            }

        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        _prefs.putBoolean("useRemoteServer", useRemoteServer.isSelected());
        if (useRemoteServer.isSelected() && (JabRef.remoteListener == null)) {
            // Start the listener now.

            JabRef.remoteListener = RemoteListener.openRemoteListener(JabRef.ths);
            if (JabRef.remoteListener != null) {
                JabRef.remoteListener.start();
            }
        } else if (!useRemoteServer.isSelected() && (JabRef.remoteListener != null)) {
            JabRef.remoteListener.disable();
            JabRef.remoteListener = null;
        }

        if ((useDefault.isSelected() == oldUseDef) ||
            !oldLnf.equals(className.getText())) {
            JOptionPane.showMessageDialog(null, Globals.lang("You have changed the look and feel setting. "
                                                             +"You must restart JabRef for this to come into effect."), Globals.lang("Changed look and feel settings"),
                                          JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean readyToClose() {
	   
        try {
            int size = Integer.parseInt(remoteServerPort.getText());
            return true; // Ok, the number was legal.
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("You must enter an integer value in the text field for") + " '" +
                    Globals.lang("Remote server port") + "'", Globals.lang("Remote server port"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

    }

}

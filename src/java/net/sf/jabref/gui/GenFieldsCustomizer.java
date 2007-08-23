package net.sf.jabref.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.Sizes;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class GenFieldsCustomizer extends JDialog {
  JPanel buttons = new JPanel();
  JButton ok = new JButton();
  JButton cancel = new JButton();
  JButton helpBut = new JButton();
  TitledBorder titledBorder1;
  TitledBorder titledBorder2;
  JLabel jLabel1 = new JLabel();
  JPanel jPanel3 = new JPanel();
  JPanel jPanel4 = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JScrollPane jScrollPane1 = new JScrollPane();
  JLabel jLabel2 = new JLabel();
  JTextArea fieldsArea = new JTextArea();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JabRefFrame parent;
  JButton revert = new JButton();
  //EntryCustomizationDialog diag;
  HelpAction help;

  public GenFieldsCustomizer(JabRefFrame frame/*, EntryCustomizationDialog diag*/) {
    super(frame, Globals.lang("Set general fields"), false);
    parent = frame;
    //this.diag = diag;
    help = new HelpAction(parent.helpDiag, GUIGlobals.generalFieldsHelp,
          "Help", GUIGlobals.getIconUrl("helpSmall"));
    helpBut = new JButton(Globals.lang("Help"));
    helpBut.addActionListener(help);
    try {
      jbInit();
      setSize(new Dimension(650, 300));
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
      ok.setText(Globals.lang("Ok"));
    ok.addActionListener(new GenFieldsCustomizer_ok_actionAdapter(this));
    cancel.setText(Globals.lang("Cancel"));
    cancel.addActionListener(new GenFieldsCustomizer_cancel_actionAdapter(this));
    //buttons.setBackground(GUIGlobals.lightGray);
    jLabel1.setText(Globals.lang("Delimit fields with semicolon, ex.")+": url;pdf;note");
    jPanel3.setLayout(gridBagLayout2);
    jPanel4.setBorder(BorderFactory.createEtchedBorder());
    jPanel4.setLayout(gridBagLayout1);
    jLabel2.setText(Globals.lang("General fields"));

    //    fieldsArea.setText(parent.prefs.get("generalFields"));
    setFieldsText();

    //jPanel3.setBackground(GUIGlobals.lightGray);
    revert.setText(Globals.lang("Default"));
    revert.addActionListener(new GenFieldsCustomizer_revert_actionAdapter(this));
    this.getContentPane().add(buttons, BorderLayout.SOUTH);
    ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
    buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    bb.addGlue();
    bb.addGridded(ok);
    bb.addGridded(revert);
    bb.addGridded(cancel);
    bb.addStrut(Sizes.DLUX5);
    bb.addGridded(helpBut);
    bb.addGlue();
    
    this.getContentPane().add(jPanel3, BorderLayout.CENTER);
    jPanel3.add(jLabel1,    new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    jPanel3.add(jPanel4,   new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 318, 193));
    jPanel4.add(jScrollPane1,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    jScrollPane1.getViewport().add(fieldsArea, null);
    jPanel4.add(jLabel2,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

  // Key bindings:
  ActionMap am = buttons.getActionMap();
  InputMap im = buttons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
  im.put(Globals.prefs.getKey("Close dialog"), "close");
  am.put("close", new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      dispose();
      //diag.requestFocus();
    }
  });

  }

  void ok_actionPerformed(ActionEvent e) {
      String[] lines = fieldsArea.getText().split("\n");
      int i = 0;
      for (; i < lines.length; i++) {
          String[] parts = lines[i].split(":");
          if (parts.length != 2) {
              // Report error and exit.
              String field = Globals.lang("field");
              JOptionPane.showMessageDialog(this, Globals.lang("Each line must be on the following form") + " '" +
                      Globals.lang("Tabname") + ":" + field + "1;" + field + "2;...;" + field + "N'",
                      Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
              return;
          }
          String testString = Util.checkLegalKey(parts[1]);
          if (!testString.equals(parts[1]) || (parts[1].indexOf('&') >= 0)) {
              // Report error and exit.
              JOptionPane.showMessageDialog(this, Globals.lang("Field names are not allowed to contain white space or the following "
                      + "characters") + ": # { } ~ , ^ &",
                      Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);

              return;
          }

          Globals.prefs.put((Globals.prefs.CUSTOM_TAB_NAME + i), parts[0]);
          Globals.prefs.put((Globals.prefs.CUSTOM_TAB_FIELDS + i), parts[1].toLowerCase());
      }
      Globals.prefs.purgeSeries(Globals.prefs.CUSTOM_TAB_NAME, i);
      Globals.prefs.purgeSeries(Globals.prefs.CUSTOM_TAB_FIELDS, i);
      Globals.prefs.updateEntryEditorTabList();
      /*
    String delimStr = fieldsArea.getText().replaceAll("\\s+","")
        .replaceAll("\\n+","").trim();
    parent.prefs.putStringArray("generalFields", Util.delimToStringArray(delimStr, ";"));
      */

      parent.removeCachedEntryEditors();
      dispose();
      //diag.requestFocus();
  }

  void cancel_actionPerformed(ActionEvent e) {
    dispose();
    //diag.requestFocus();
  }

    void setFieldsText() {
        StringBuffer sb = new StringBuffer();

        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i=0; i<tabList.getTabCount(); i++) {
            sb.append(tabList.getTabName(i));
            sb.append(":");
            for (Iterator<String> j = tabList.getTabFields(i).iterator(); j
				.hasNext();) {
				String field = j.next();
				sb.append(field);
				if (j.hasNext())
					sb.append(";");
			}
            sb.append("\n");
        }

        fieldsArea.setText(sb.toString());
    }

    void revert_actionPerformed(ActionEvent e) {
        StringBuffer sb = new StringBuffer();
        String name = null, fields = null;
        int i = 0;
        while ((name = (String) Globals.prefs.defaults.get
                (Globals.prefs.CUSTOM_TAB_NAME + "_def" + i)) != null) {
            sb.append(name);
            fields = (String) Globals.prefs.defaults.get
                    (Globals.prefs.CUSTOM_TAB_FIELDS + "_def" + i);
            sb.append(":");
            sb.append(fields);
            sb.append("\n");
            i++;
        }
        fieldsArea.setText(sb.toString());

    }
}

class GenFieldsCustomizer_ok_actionAdapter implements java.awt.event.ActionListener {
  GenFieldsCustomizer adaptee;

  GenFieldsCustomizer_ok_actionAdapter(GenFieldsCustomizer adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.ok_actionPerformed(e);
  }
}

class GenFieldsCustomizer_cancel_actionAdapter implements java.awt.event.ActionListener {
  GenFieldsCustomizer adaptee;

  GenFieldsCustomizer_cancel_actionAdapter(GenFieldsCustomizer adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.cancel_actionPerformed(e);
  }
}

class GenFieldsCustomizer_revert_actionAdapter implements java.awt.event.ActionListener {
  GenFieldsCustomizer adaptee;

  GenFieldsCustomizer_revert_actionAdapter(GenFieldsCustomizer adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.revert_actionPerformed(e);
  }
}

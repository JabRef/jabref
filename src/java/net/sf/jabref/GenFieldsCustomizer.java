package net.sf.jabref;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class GenFieldsCustomizer extends JDialog {
  JPanel jPanel2 = new JPanel();
  JButton ok = new JButton();
  JButton cancel = new JButton();
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
  EntryCustomizationDialog diag;

  public GenFieldsCustomizer(JabRefFrame frame, EntryCustomizationDialog diag) {
    super(frame, Globals.lang("Set general fields"), false);
    parent = frame;
    this.diag = diag;
    try {
      jbInit();
      setSize(new Dimension(400, 200));
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
    jPanel2.setBackground(GUIGlobals.lightGray);
    jLabel1.setText(Globals.lang("Delimit fields with semicolon, ex.")+": url;pdf;note");
    jPanel3.setLayout(gridBagLayout2);
    jPanel4.setBorder(BorderFactory.createEtchedBorder());
    jPanel4.setLayout(gridBagLayout1);
    jLabel2.setText(Globals.lang("General fields"));
    fieldsArea.setText(parent.prefs.get("generalFields"));
    jPanel3.setBackground(GUIGlobals.lightGray);
    revert.setText(Globals.lang("Default"));
    revert.addActionListener(new GenFieldsCustomizer_revert_actionAdapter(this));
    this.getContentPane().add(jPanel2, BorderLayout.SOUTH);
    jPanel2.add(ok, null);
    jPanel2.add(revert, null);
    jPanel2.add(cancel, null);
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
  ActionMap am = jPanel2.getActionMap();
  InputMap im = jPanel2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
  im.put(GUIGlobals.exitDialog, "close");
  am.put("close", new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      dispose();
      diag.requestFocus();
    }
  });

  }

  void ok_actionPerformed(ActionEvent e) {
    String delimStr = fieldsArea.getText().replaceAll("\\s+","")
        .replaceAll("\\n+","").trim();
    parent.prefs.putStringArray("generalFields", Util.delimToStringArray(delimStr, ";"));
    for (int i=0; i<parent.tabbedPane.getTabCount(); i++) {
      BasePanel bp = (BasePanel)parent.tabbedPane.getComponentAt(i);
      bp.entryEditors.clear();
    }
    dispose();
    diag.requestFocus();
  }

  void cancel_actionPerformed(ActionEvent e) {
    dispose();
    diag.requestFocus();
  }

  void revert_actionPerformed(ActionEvent e) {
    fieldsArea.setText((String)parent.prefs.defaults.get("generalFields"));
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

package net.sf.jabref;

import javax.swing.JDialog;
import java.awt.HeadlessException;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.*;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import net.sf.jabref.export.*;
import java.awt.*;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class DuplicateResolverDialog extends JDialog {

  public static final int
      KEEP_BOTH = 0,
      KEEP_UPPER = 1,
      KEEP_LOWER = 2;

  final Dimension DIM = new Dimension(650, 450);

  PreviewPanel p1, p2;
  JTextArea ta1, ta2;
  JTabbedPane tabbed = new JTabbedPane();
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con = new GridBagConstraints();
  JButton first = new JButton(Globals.lang("Keep upper")),
      second = new JButton(Globals.lang("Keep lower")),
      both = new JButton(Globals.lang("Keep both"));
  JPanel options = new JPanel(),
      main = new JPanel(),
      source = new JPanel();
  int status = KEEP_BOTH;

  public DuplicateResolverDialog(JabRefFrame frame, BibtexEntry one, BibtexEntry two) {
    super(frame, Globals.lang("Possible duplicate entries"), true);

    p1 = new PreviewPanel(one);
    p2 = new PreviewPanel(two);
    ta1 = new JTextArea();
    ta2 = new JTextArea();
    ta1.setEditable(false);
    ta2.setEditable(false);

    setSourceView(one, two);

    //getContentPane().setLayout();
    main.setLayout(gbl);
    source.setLayout(gbl);
    con.insets = new Insets(10,10,10,10);
    con.fill = GridBagConstraints.BOTH;
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 1;
    JScrollPane sp = new JScrollPane(p1);
    gbl.setConstraints(sp, con);
    main.add(sp);
    sp = new JScrollPane(ta1);
    gbl.setConstraints(sp, con);
    source.add(sp);
    sp = new JScrollPane(p2);
    gbl.setConstraints(sp, con);
    main.add(sp);
    sp = new JScrollPane(ta2);
    gbl.setConstraints(sp, con);
    source.add(sp);
    tabbed.add(Globals.lang("Short form"), main);
    tabbed.add(Globals.lang("Complete record"), source);
    options.add(first);
    options.add(second);
    options.add(both);

    first.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_UPPER;
        dispose();
      }
    });

    second.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_LOWER;
        dispose();
      }
    });

    both.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_BOTH;
        dispose();
      }
    });

    getContentPane().add(tabbed, BorderLayout.CENTER);
    getContentPane().add(options, BorderLayout.SOUTH);
    //pack();
    setSize(DIM);
    both.requestFocus();
    Util.placeDialog(this, frame);
  }

  private void setSourceView(BibtexEntry one, BibtexEntry two) {
    try {
      StringWriter sw = new StringWriter();
      one.write(sw, new LatexFieldFormatter(), false);
      ta1.setText(sw.getBuffer().toString());
      sw = new StringWriter();
      two.write(sw, new LatexFieldFormatter(), false);
      ta2.setText(sw.getBuffer().toString());
    }
    catch (IOException ex) {
    }
  }

  public void setEntries(BibtexEntry newOne, BibtexEntry newTwo) {
    setSourceView(newOne, newTwo);
    p1.setEntry(newOne);
    p2.setEntry(newTwo);
  }

  public int getSelected() {
    return status;
  }

  public static int resolveDuplicate(JabRefFrame frame, BibtexEntry one, BibtexEntry two) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two);
    drd.show();
    return drd.getSelected();
  }

}

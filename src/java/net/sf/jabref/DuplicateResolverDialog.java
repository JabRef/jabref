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


// created by : ?
//
// modified : r.nagel 2.09.2004
//            - insert close button

public class DuplicateResolverDialog extends JDialog {

    public static final int
	NOT_CHOSEN = -1,
	KEEP_BOTH = 0,
	KEEP_UPPER = 1,
	KEEP_LOWER = 2,
	BREAK      = 5,  // close
	DUPLICATE_SEARCH = 1,
	IMPORT_CHECK = 2,
    INSPECTION = 3;

    final Dimension DIM = new Dimension(650, 600);
    
    PreviewPanel p1, p2;
    JTextArea ta1, ta2;
    JTabbedPane tabbed = new JTabbedPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JButton first, second, both,
	cancel = new JButton(Globals.lang("Cancel"));
    JPanel options = new JPanel(),
	main = new JPanel(),
	source = new JPanel();
    int status = NOT_CHOSEN;
    boolean block = true;
    TitleLabel lab;
    
  public DuplicateResolverDialog(JabRefFrame frame, BibtexEntry one, BibtexEntry two, int type) {
    super(frame, Globals.lang("Possible duplicate entries"), true);

      switch (type) {
          case DUPLICATE_SEARCH:
              first = new JButton(Globals.lang("Keep upper"));
              second = new JButton(Globals.lang("Keep lower"));
              both = new JButton(Globals.lang("Keep both"));
              break;
          case INSPECTION:
              first = new JButton(Globals.lang("Remove old entry"));
              second = new JButton(Globals.lang("Remove entry from import"));
              both = new JButton(Globals.lang("Keep both"));
              break;
          default:
              first = new JButton(Globals.lang("Import and remove old entry"));
	          second = new JButton(Globals.lang("Do not import entry"));
	          both = new JButton(Globals.lang("Import and keep old entry"));
      }

    String layout = Globals.prefs.get("preview0");
    p1 = new PreviewPanel(one, layout);
    p2 = new PreviewPanel(two, layout);
    ta1 = new JTextArea();
    ta2 = new JTextArea();
    ta1.setEditable(false);
    ta2.setEditable(false);
    
    //ta1.setPreferredSize(dim); 
    //ta2.setPreferredSize(dim);
    
    setSourceView(one, two);

    //getContentPane().setLayout();
    main.setLayout(gbl);
    source.setLayout(gbl);
    con.insets = new Insets(10,10,0,10);
    con.fill = GridBagConstraints.BOTH;
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 0;
    lab = new TitleLabel(Globals.lang((type==DUPLICATE_SEARCH)?"":
				  "Entry in current database"));
    gbl.setConstraints(lab, con);
    main.add(lab);
    con.weighty = 1;
    con.insets = new Insets(5,10,10,10);
    JScrollPane sp = new JScrollPane(p1);
    gbl.setConstraints(sp, con);
    main.add(sp);
    con.weighty = 0;
    con.insets = new Insets(10,10,0,10);
    lab = new TitleLabel(Globals.lang((type==DUPLICATE_SEARCH)?"":
				  "Entry in import"));
    gbl.setConstraints(lab, con);
    main.add(lab);
    con.weighty = 1;
    con.insets = new Insets(5,10,10,10);
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
    if (type != IMPORT_CHECK) {
        options.add(Box.createHorizontalStrut(5));
        options.add(cancel);
    }

    first.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_UPPER;
        block = false;
        dispose();
      }
    });

    second.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_LOWER;
        block = false;
        dispose();
      }
    });

    both.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_BOTH;
        block = false;
        dispose();
      }
    });

    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = BREAK;
        block = false;
        dispose();
      }
    });

    getContentPane().add(tabbed, BorderLayout.CENTER);
    getContentPane().add(options, BorderLayout.SOUTH);
    pack();
    
    
    if (getHeight() > DIM.height) {
        setSize(new Dimension(getWidth(), DIM.height));
    }
    if (getWidth() > DIM.width) {
        setSize(new Dimension(DIM.width, getHeight()));
    }
    
    
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
    status = NOT_CHOSEN;
    p1.revalidate();
    p1.repaint();
    block = true;
  }

public boolean isBlocking() {
  return block;
}

  public int getSelected() {
    return status;
  }

  public static int resolveDuplicate(JabRefFrame frame, BibtexEntry one, BibtexEntry two) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two,
							      DUPLICATE_SEARCH);
    drd.show();
    return drd.getSelected();
  }

}

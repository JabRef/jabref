package net.sf.jabref.collab;

import net.sf.jabref.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.IOException;

public class FileUpdatePanel extends SidePaneComponent implements ActionListener {

  JButton test = new JButton("Jau");
  BasePanel panel;
  JabRefFrame frame;

  public FileUpdatePanel(JabRefFrame frame, BasePanel panel,
                         SidePaneManager manager, JabRefPreferences prefs) {
    super(manager);
    this.panel = panel;
    this.frame = frame;
    SidePaneHeader header = new SidePaneHeader
        ("File changed", GUIGlobals.saveIconFile, this);
    setLayout(new BorderLayout());
    add(header, BorderLayout.NORTH);
    JPanel main = new JPanel();
    main.add(test);
    //main.add(new JLabel("Your file has<BR>been modified<BR>by another process!"));
    add(main, BorderLayout.CENTER);

    test.addActionListener(this);
  }

  /**
   * actionPerformed
   *
   * @param e ActionEvent
   */
  public void actionPerformed(ActionEvent e) {
    ChangeScanner scanner = new ChangeScanner(frame, panel); //, panel.database(), panel.metaData());
    try {
      scanner.changeScan(panel.file());
    } catch (IOException ex) {

    }
  }
}

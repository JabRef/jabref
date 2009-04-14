/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.jabref.plugin;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.HelpAction;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.plugin.PluginInstaller.NameAndVersion;

/**
 *
 * @author alver
 */
public class ManagePluginsDialog {

    private JabRefFrame frame;
    private JDialog diag;
    private SortedList<NameAndVersion> plugins;
    private JTable table;
    private TableFormat tableFormat;
    private JButton close = new JButton(Globals.lang("Close")),
            install = new JButton(Globals.lang("Install plugin")),
            download = new JButton(Globals.lang("Download plugin")),
            remove = new JButton(Globals.lang("Delete")),
            help = new JButton(Globals.lang("Help"));
    
    
    public ManagePluginsDialog(JabRefFrame frame) {
        this.frame = frame;
        diag = new JDialog(frame, Globals.lang("Plugin manager"), false);
        help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.pluginHelp, "Help"));
        
        JLabel lab = new JLabel
                (Globals.lang("Plugins installed in your user plugin directory (%0) are listed below:",
                PluginCore.userPluginDir.getPath()));
        lab.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.getContentPane().add(lab, BorderLayout.NORTH);
        
        table = new JTable();
        buildList();
        
        diag.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        
        ButtonBarBuilder b = new ButtonBarBuilder();
        b.addGlue();
        b.addGridded(install);
        b.addGridded(download);
        b.addGridded(remove);
        b.addGridded(close);
        b.addRelatedGap();
        b.addGridded(help);
        
        b.addGlue();
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(b.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(frame);
        
        install.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                installPlugin();
            }
        });
        
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                downloadPlugin();
            }
        });
        
        Action closeListener = new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                diag.dispose();
            }
        };
        close.addActionListener(closeListener);
        
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                removeSelected();
            }
        });
        
        // Key bindings:
        ActionMap am = b.getPanel().getActionMap();
        InputMap im = b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeListener);

    }
    
    private void removeSelected() {
        int[] sel = table.getSelectedRows();
        if (sel.length > 0) {
            String message = Globals.lang("Delete the %0 selected plugins?", String.valueOf(sel.length));
            String title = Globals.lang("Delete plugins");
            if (sel.length == 1) {
                message = Globals.lang("Delete the selected plugin?");
                title = Globals.lang("Delete plugin");
            }
            int reply = JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
            boolean success = true;
            for (int i=0; i<sel.length; i++) {
                PluginInstaller.NameAndVersion nav = plugins.get(sel[i]);
                success = PluginInstaller.deletePlugin(nav) & success;
            }
            buildList();
        }
    }
    
    private void buildList() {
        plugins = new SortedList<NameAndVersion>(PluginInstaller.findInstalledPlugins());
        tableFormat = new PluginTableFormat();
        EventTableModel tableModel = new EventTableModel(plugins, tableFormat);
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
    }
    
    public void setVisible(boolean visible) {
        diag.setVisible(visible);
    }
    
    public void installPlugin() {
        String filename = Globals.getNewFile(frame, new File(System.getProperty("user.home")),
            ".jar", JFileChooser.OPEN_DIALOG, false);
        if (filename == null)
            return;
        File f = new File(filename);
        if (f != null) {
            if (!f.exists()) {
                JOptionPane.showMessageDialog(frame, Globals.lang("File not found")+".",
                        Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    installFromURL(new URL("file://"+f.getPath()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void downloadPlugin() {
        String url = JOptionPane.showInputDialog(Globals.lang("Enter download URL"));
        if (url == null)
            return;
        try {
            installFromURL(new URL(url));
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL"),
                    Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void installFromURL(URL url) {
        PluginInstaller.installPlugin(frame, url);
        buildList();
    }
    
    class PluginTableFormat implements TableFormat<NameAndVersion> {

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int col) {
            if (col == 0)
                return Globals.lang("Plugin name");
            else if (col == 1)
                return Globals.lang("Version");
            else return Globals.lang("Status");
        }

        public Object getColumnValue(NameAndVersion nav, int col) {
            if (col == 0)
                return nav.name;
            else if (col == 1) {
                if (!nav.version.equals(PluginInstaller.VersionNumber.ZERO))
                    return nav.version.toString();
                else return Globals.lang("Unknown");
            }
            else {
                int status = nav.getStatus();
                if (status == 0)
                    return Globals.lang("Not loaded");
                else if (status == 1)
                    return Globals.lang("Loaded");
                else
                    return Globals.lang("Error");
            }
        }
        
    }
}

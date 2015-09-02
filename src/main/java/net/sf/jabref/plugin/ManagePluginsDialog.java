/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.plugin;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import javax.swing.*;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.plugin.PluginInstaller.NameAndVersion;

/**
 *
 * @author alver
 */
class ManagePluginsDialog {

    private final JabRefFrame frame;
    private final JDialog diag;
    private SortedList<NameAndVersion> plugins;
    private final JTable table;
    private final JTable tableOther;


    public ManagePluginsDialog(JabRefFrame frame) {
        this.frame = frame;
        diag = new JDialog(frame, Globals.lang("Plugin manager"), false);
        JButton help = new JButton(Globals.lang("Help"));
        help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.pluginHelp, "Help"));
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());

        JLabel lab = new JLabel
                (Globals.lang("Plugins installed in your user plugin directory (%0):",
                        PluginCore.userPluginDir.getPath()));
        lab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pan.add(lab, BorderLayout.NORTH);

        // Table for user dir plugins:
        table = new JTable();
        // Table for other plugiuns
        tableOther = new JTable();
        tableOther.setRowSelectionAllowed(false);
        tableOther.setColumnSelectionAllowed(false);
        tableOther.setCellSelectionEnabled(false);
        buildList();
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));
        tableOther.setPreferredScrollableViewportSize(new Dimension(500, 100));
        pan.add(new JScrollPane(table), BorderLayout.CENTER);
        diag.getContentPane().add(pan, BorderLayout.NORTH);
        pan = new JPanel();
        pan.setLayout(new BorderLayout());
        lab = new JLabel(Globals.lang("Plugins installed in other locations:"));
        lab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pan.add(lab, BorderLayout.NORTH);
        pan.add(new JScrollPane(tableOther), BorderLayout.CENTER);

        diag.getContentPane().add(pan, BorderLayout.CENTER);

        ButtonBarBuilder b = new ButtonBarBuilder();
        b.addGlue();
        JButton install = new JButton(Globals.lang("Install plugin"));
        b.addButton(install);
        JButton download = new JButton(Globals.lang("Download plugin"));
        b.addButton(download);
        JButton remove = new JButton(Globals.lang("Delete"));
        b.addButton(remove);
        JButton close = new JButton(Globals.lang("Close"));
        b.addButton(close);
        b.addRelatedGap();
        b.addButton(help);

        b.addGlue();
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(b.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(frame);

        install.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                installPlugin();
            }
        });

        download.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                downloadPlugin();
            }
        });

        Action closeListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                diag.dispose();
            }
        };
        close.addActionListener(closeListener);

        remove.addActionListener(new ActionListener() {

            @Override
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
            if (reply != JOptionPane.YES_OPTION) {
                return;
            }
            boolean success = true;
            for (int aSel : sel) {
                NameAndVersion nav = plugins.get(aSel);
                success = PluginInstaller.deletePlugin(nav) & success;
            }
            if (!success) {

                JOptionPane.showMessageDialog(frame, sel.length > 1 ?
                        Globals.lang("Plugins will be deleted next time JabRef starts up.")
                        : Globals.lang("Plugin will be deleted next time JabRef starts up."),
                        sel.length > 1 ? Globals.lang("Delete plugins") : Globals.lang("Delete plugin"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            buildList();
        }
    }

    private void buildList() {
        plugins = new SortedList<NameAndVersion>(PluginInstaller.findInstalledPlugins());
        // Move those plugins that are not installed in the user plugin dir to another list:
        EventList<NameAndVersion> outsideUserDir = new BasicEventList<NameAndVersion>();
        for (Iterator<NameAndVersion> i = plugins.iterator(); i.hasNext();) {
            NameAndVersion nav = i.next();
            if (!nav.inUserDirectory) {
                outsideUserDir.add(nav);
                i.remove();
            }
        }
        SortedList<NameAndVersion> pluginsOther = new SortedList<NameAndVersion>(outsideUserDir);
        TableFormat<NameAndVersion> tableFormatOther = new PluginTableFormat();
        EventTableModel<NameAndVersion> tableModel = new EventTableModel<NameAndVersion>(pluginsOther, tableFormatOther);
        tableOther.setModel(tableModel);
        tableOther.getColumnModel().getColumn(0).setPreferredWidth(200);
        tableOther.getColumnModel().getColumn(1).setPreferredWidth(50);
        tableOther.getColumnModel().getColumn(2).setPreferredWidth(50);

        TableFormat<NameAndVersion> tableFormat = new PluginTableFormat();
        EventTableModel<NameAndVersion> tableModelOther = new EventTableModel<NameAndVersion>(plugins, tableFormat);
        table.setModel(tableModelOther);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
    }

    public void setVisible(boolean visible) {
        diag.setVisible(visible);
    }

    private void installPlugin() {
        String filename = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")),
                ".jar", JFileChooser.OPEN_DIALOG, false);
        if (filename == null) {
            return;
        }
        File f = new File(filename);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(frame, Globals.lang("File not found") + ".",
                    Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
        } else {
            installFromFile(f);
        }

    }

    private void downloadPlugin() {
        String url = JOptionPane.showInputDialog(Globals.lang("Enter download URL"));
        if (url == null) {
            return;
        }
        try {
            installFromURL(new URL(url));
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL"),
                    Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installFromURL(URL url) {
        try {
            File tmpFile = File.createTempFile("jabref-plugin", ".jar");
            tmpFile.deleteOnExit();
            URLDownload ud = URLDownload.buildMonitoredDownload(frame, url);
            ud.downloadToFile(tmpFile);
            String path = url.getPath();
            int pos = path.lastIndexOf('/');
            if ((pos >= 0) && (pos < (path.length() - 1))) {
                path = path.substring(pos + 1);
            }
            PluginInstaller.installPlugin(frame, tmpFile, path);
            tmpFile.delete();
            buildList();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void installFromFile(File file) {
        PluginInstaller.installPlugin(frame, file, null);
        buildList();
    }


    private class PluginTableFormat implements TableFormat<NameAndVersion> {

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0) {
                return Globals.lang("Plugin name");
            } else if (col == 1) {
                return Globals.lang("Version");
            } else {
                return Globals.lang("Status");
            }
        }

        @Override
        public Object getColumnValue(NameAndVersion nav, int col) {
            if (col == 0) {
                return nav.name;
            } else if (col == 1) {
                if (!nav.version.equals(PluginInstaller.VersionNumber.ZERO)) {
                    return nav.version.toString();
                } else {
                    return Globals.lang("Unknown");
                }
            }
            else {
                int status = nav.getStatus();
                if (status == 0) {
                    return Globals.lang("Not loaded");
                } else if (status == 1) {
                    return Globals.lang("Loaded");
                } else {
                    return Globals.lang("Error");
                }
            }
        }

    }
}

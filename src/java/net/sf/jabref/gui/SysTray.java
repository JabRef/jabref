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
package net.sf.jabref.gui;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SysTray {

    private JabRefFrame frame;
    private TrayIcon icon;
    private PopupMenu popup = new PopupMenu();
    private MenuItem showWindow;
    private SystemTray tray = null;

    public SysTray(JabRefFrame frame) {
        this.frame = frame;

        final ActionListener showJabref = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SysTray.this.frame.showIfMinimizedToSysTray();
                    }
                });

            }
        };
        showWindow = new MenuItem(Globals.lang("Show"));
        showWindow.addActionListener(showJabref);
        popup.add(showWindow);
        ImageIcon imic = new ImageIcon(GUIGlobals.class.getResource("/images/JabRef-icon-48.png"));
        icon = new TrayIcon(imic.getImage(), "JabRef", popup);
        icon.setImageAutoSize(true);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                showJabref.actionPerformed(new ActionEvent(mouseEvent.getSource(), 0, ""));
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                super.mousePressed(mouseEvent);    //To change body of overridden methods use File | Settings | File Templates.
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                super.mouseReleased(mouseEvent);    //To change body of overridden methods use File | Settings | File Templates.
            }
        });
        if (SystemTray.isSupported())
            tray = SystemTray.getSystemTray();
    }

    public void setTrayIconVisible(boolean visible) {
        if (tray == null)
            return;
        try {
            if (visible)
                tray.add(icon);
            else
                tray.remove(icon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(SystemTray.isSupported());
    }
}

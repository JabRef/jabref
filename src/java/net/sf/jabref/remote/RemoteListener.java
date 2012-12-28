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
package net.sf.jabref.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Vector;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.imports.ParserResult;

import javax.swing.*;

public class RemoteListener extends Thread {

    private JabRef jabref;
    private ServerSocket socket;
    private boolean active = true, toStop = false;
    private static final String IDENTIFIER = "jabref";

    public RemoteListener(JabRef jabref, ServerSocket socket) {
        this.jabref = jabref;
        this.socket = socket;
    }

    public void disable() {
        toStop = true;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (active) {
            try {
                Socket newSocket = socket.accept();

                newSocket.setSoTimeout(1000);

                if (toStop) {
                    active = false;
                    return;
                }

                OutputStream out = newSocket.getOutputStream();
                InputStream in = newSocket.getInputStream();
                out.write(IDENTIFIER.getBytes());
                out.write('\0');
                out.flush();

                int c;
                StringBuffer sb = new StringBuffer();
                try {
                    while (((c = in.read()) != '\0') && (c >= 0)) {
                        sb.append((char)c);
                    }
                    if (sb.length() == 0) {
                        continue;
                    }
                    String[] args = sb.toString().split("\n");
                    Vector<ParserResult> loaded = jabref.processArguments(args, false);

                    // put "bringToFront" in the queue
                    // it has to happen before the call to import as the import might open a dialog
                    // --> Globals.prefs.getBoolean("useImportInspectionDialog")
                    // this dialog has to be shown AFTER JabRef has been brought to front
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JabRef.jrf.showIfMinimizedToSysTray();
                        }
                    });

                    for (int i=0; i<loaded.size(); i++) {
                        ParserResult pr = loaded.elementAt(i);
                        JabRef.jrf.addParserResult(pr, (i==0));
                    }

                    in.close();
                    out.close();
                    newSocket.close();
                } catch (SocketTimeoutException ex) {
                    //System.out.println("timeout");
                    in.close();
                    out.close();
                    newSocket.close();
                }



            } catch (SocketException ex) {
                active = false;
                //ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static RemoteListener openRemoteListener(JabRef jabref) {
        try {
            ServerSocket socket = new ServerSocket(Globals.prefs.getInt("remoteServerPort"), 1,
                    InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
            RemoteListener listener = new RemoteListener(jabref, socket);
            return listener;
        } catch (IOException e) {
            if (!e.getMessage().startsWith("Address already in use"))
                e.printStackTrace();
            return null;

        }

    }

    /**
     * Attempt to send command line arguments to already running JabRef instance.
     * @param args Command line arguments.
     * @return true if successful, false otherwise.
     */
    public static boolean sendToActiveJabRefInstance(String[] args) {
        try {
            InetAddress local = InetAddress.getByName("localhost");
            Socket socket = new Socket(local, Globals.prefs.getInt("remoteServerPort"));
            socket.setSoTimeout(2000);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            int c;
            StringBuffer sb = new StringBuffer();
            try {
                while (((c = in.read()) != '\0') && (c >= 0)) {
                    sb.append((char)c);
                }
            } catch (SocketTimeoutException ex) {
                 System.out.println("Connection timed out.");
            }

            if (!IDENTIFIER.equals(sb.toString())) {
            	String port = String.valueOf(Globals.prefs.getInt("remoteServerPort"));
                String error = Globals.lang("Cannot use port %0 for remote operation; another application may be using it. Try specifying another port.", port);
                System.out.println(error);
                return false;
            }

            for (int i=0; i<args.length; i++) {
                byte[] bytes = args[i].getBytes();
                out.write(bytes);
                out.write('\n');
            }
            out.write('\0');
            out.flush();
            in.close();
            out.close();
            socket.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

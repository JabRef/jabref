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
import net.sf.jabref.Util;
import net.sf.jabref.imports.ParserResult;

import javax.swing.*;

public class RemoteListener extends Thread {

    private static final String IDENTIFIER = "jabref";

    private final JabRef jabref;
    private final ServerSocket serverSocket;

    private RemoteListener(JabRef jabref, ServerSocket serverSocket) {
        this.jabref = jabref;
        this.serverSocket = serverSocket;
        this.setName("JabRef - Remote Listener");
    }

    @Override
    public void interrupt() {
        super.interrupt();

        closeServerSocket();
    }

    public void closeServerSocket() {
        // need to close the serverSocket to wake up thread to shut it down
        try {
            serverSocket.close();
        } catch (IOException ignored) {

        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(1000);

                    if (Thread.interrupted()) {
                        return;
                    }

                    Protocol protocol = new Protocol(socket);
                    protocol.sendMessage(IDENTIFIER);

                    try {
                        String message = protocol.receiveMessage();
                        if (message.isEmpty()) {
                            continue;
                        }

                        Vector<ParserResult> loaded = jabref.processArguments(message.split("\n"), false);
                        if (loaded == null) {
                            return;
                        }

                        // put "bringToFront" in the queue
                        // it has to happen before the call to import as the import might open a dialog
                        // --> Globals.prefs.getBoolean("useImportInspectionDialog")
                        // this dialog has to be shown AFTER JabRef has been brought to front
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                JabRef.jrf.showIfMinimizedToSysTray();
                            }
                        });

                        for (int i = 0; i < loaded.size(); i++) {
                            ParserResult pr = loaded.elementAt(i);
                            JabRef.jrf.addParserResult(pr, (i == 0));
                        }

                    } finally {
                        protocol.close();
                    }

                } catch (SocketException ex) {
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    public static RemoteListener openRemoteListener(JabRef jabref) {
        try {
            ServerSocket socket = new ServerSocket(Globals.prefs.getInt("remoteServerPort"), 1,
                    InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
            return new RemoteListener(jabref, socket);
        } catch (IOException e) {
            if (!e.getMessage().startsWith("Address already in use")) {
                e.printStackTrace();
            }
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

            Protocol protocol = new Protocol(socket);
            try {
                String identifier = protocol.receiveMessage();

                if (!RemoteListener.IDENTIFIER.equals(identifier)) {
                    String port = String.valueOf(Globals.prefs.getInt("remoteServerPort"));
                    String error = Globals.lang("Cannot use port %0 for remote operation; another application may be using it. Try specifying another port.", port);
                    System.out.println(error);
                    return false;
                }
                protocol.sendMessage(Util.join(args, "\n"));
                return true;
            } finally {
                protocol.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Every message is terminated with '\0'.
     */
    static class Protocol {

        private final Socket socket;
        private final OutputStream out;
        private final InputStream in;

        Protocol(Socket socket) throws IOException {
            this.socket = socket;
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
        }

        public void sendMessage(String message) throws IOException {
            out.write(message.getBytes());
            out.write('\0');
            out.flush();
        }

        public String receiveMessage() throws IOException {
            int c;
            StringBuilder result = new StringBuilder();
            try {
                while (((c = in.read()) != '\0') && (c >= 0)) {
                    result.append((char) c);
                }
            } catch (SocketTimeoutException ex) {
                System.out.println("Connection timed out.");
            }
            return result.toString();
        }

        void close() {
            try {
                in.close();
            } catch (IOException ignored) {}

            try {
                out.close();
            } catch (IOException ignored) {}

            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

}

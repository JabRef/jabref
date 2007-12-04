package net.sf.jabref.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.imports.ParserResult;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Aug 14, 2005
 * Time: 8:11:58 PM
 * To change this template use File | Settings | File Templates.
 */
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

                    for (int i=0; i<loaded.size(); i++) {
                        ParserResult pr = loaded.elementAt(i);
                        if (!pr.toOpenTab()) {
                            jabref.jrf.addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), (i == 0));
                        } else {
                            // Add the entries to the open tab.
                            BasePanel panel = jabref.jrf.basePanel();
                            if (panel == null) {
                                // There is no open tab to add to, so we create a new tab:
                                jabref.jrf.addTab(pr.getDatabase(), pr.getFile(), pr.getMetaData(), pr.getEncoding(), (i == 0));
                            } else {
                                List<BibtexEntry> entries = new ArrayList<BibtexEntry>(pr.getDatabase().getEntries());
                                jabref.jrf.addImportedEntries(panel, entries, "", false);
                            }
                        }
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
                String error = Globals.lang("Cannot use port %0 for remote operation; another "
                    +"application may be using it. Try specifying another port.",
                        new String[] {String.valueOf(Globals.prefs.getInt("remoteServerPort"))});
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

package net.sf.jabref.imports;

import net.sf.jabref.*;

import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

// The action concerned with opening an existing database.

public class OpenDatabaseAction extends MnemonicAwareAction {
    boolean showDialog;

    private JabRefFrame frame;

    public OpenDatabaseAction(JabRefFrame frame, boolean showDialog) {
        super(new ImageIcon(GUIGlobals.openIconFile));
        this.frame = frame;
        this.showDialog = showDialog;
        putValue(NAME, "Open database");
        putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Open database"));
        putValue(SHORT_DESCRIPTION, Globals.lang("Open BibTeX database"));
    }

    public void actionPerformed(ActionEvent e) {
        File fileToOpen = null;

        if (showDialog) {

            String chosenFile = Globals.getNewFile(frame, Globals.prefs, new File(Globals.prefs.get("workingDirectory")), ".bib",
                    JFileChooser.OPEN_DIALOG, true);

            if (chosenFile != null) {
                fileToOpen = new File(chosenFile);
            }
        } else {
            Util.pr(NAME);
            Util.pr(e.getActionCommand());
            fileToOpen = new File(Util.checkName(e.getActionCommand()));
        }

        // Run the actual open in a thread to prevent the program
        // locking until the file is loaded.
        if (fileToOpen != null) {
            final File theFile = fileToOpen;
            (new Thread() {
                public void run() {
                    openIt(theFile, true);
                }
            }).start();
            frame.getFileHistory().newFile(fileToOpen.getPath());
        }
    }

    class OpenItSwingHelper implements Runnable {
        BasePanel bp;
        boolean raisePanel;
        File file;

        OpenItSwingHelper(BasePanel bp, File file, boolean raisePanel) {
            this.bp = bp;
            this.raisePanel = raisePanel;
            this.file = file;
        }

        public void run() {
            frame.addTab(bp, file, raisePanel);
        }
    }

    public void openIt(File file, boolean raisePanel) {
        if ((file != null) && (file.exists())) {
            frame.output(Globals.lang("Opening") + ": '" + file.getPath() + "'");
            try {
                String fileName = file.getPath();
                Globals.prefs.put("workingDirectory", file.getPath());
                // Should this be done _after_ we know it was successfully opened?
                String encoding = Globals.prefs.get("defaultEncoding");
                ParserResult pr = loadDatabase(file, encoding);
		if ((pr == null) || (pr == ParserResult.INVALID_FORMAT)) {
		    JOptionPane.showMessageDialog(null, Globals.lang("Error opening file"+" '"+fileName+"'"),
						  Globals.lang("Error"),
						  JOptionPane.ERROR_MESSAGE);

		    return;
		}
		 
                BibtexDatabase db = pr.getDatabase();
                HashMap meta = pr.getMetaData();

                if (pr.hasWarnings()) {
                    final String[] wrns = pr.warnings();
                    (new Thread() {
                        public void run() {
                            StringBuffer wrn = new StringBuffer();
                            for (int i = 0; i < wrns.length; i++)
                                wrn.append(i + 1).append(". ").append(wrns[i]).append("\n");

                            if (wrn.length() > 0)
                                wrn.deleteCharAt(wrn.length() - 1);
                            // Note to self or to someone else: The following line causes an
                            // ArrayIndexOutOfBoundsException in situations with a large number of
                            // warnings; approx. 5000 for the database I opened when I observed the problem
                            // (duplicate key warnings). I don't think this is a big problem for normal situations,
                            // and it may possibly be a bug in the Swing code.
                            JOptionPane.showMessageDialog(frame, wrn.toString(),
                                    Globals.lang("Warnings"),
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }).start();
                }

                BasePanel bp = new BasePanel(frame, db, file, meta, pr.getEncoding());

                /*
                 if (Globals.prefs.getBoolean("autoComplete")) {
                 db.setCompleters(autoCompleters);
                 }
                */

                // file is set to null inside the EventDispatcherThread
                SwingUtilities.invokeLater(new OpenItSwingHelper(bp, file, raisePanel));

                // See if any custom entry types were imported, but disregard those we already know:
                for (Iterator i = pr.getEntryTypes().keySet().iterator(); i.hasNext();) {
                    String typeName = ((String) i.next()).toLowerCase();
                    if (BibtexEntryType.ALL_TYPES.get(typeName) != null)
                        i.remove();
                }
                if (pr.getEntryTypes().size() > 0) {


                    StringBuffer sb = new StringBuffer(Globals.lang("Custom entry types found in file") + ": ");
                    Object[] types = pr.getEntryTypes().keySet().toArray();
                    Arrays.sort(types);
                    for (int i = 0; i < types.length; i++) {
                        sb.append(types[i].toString()).append(", ");
                    }
                    String s = sb.toString();
                    int answer = JOptionPane.showConfirmDialog(frame,
                            s.substring(0, s.length() - 2) + ".\n"
                            + Globals.lang("Remember these entry types?"),
                            Globals.lang("Custom entry types"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (answer == JOptionPane.YES_OPTION) {
                        // Import
                        HashMap et = pr.getEntryTypes();
                        for (Iterator i = et.keySet().iterator(); i.hasNext();) {
                            BibtexEntryType typ = (BibtexEntryType) et.get(i.next());
                            //System.out.println(":"+typ.getName()+"\n"+typ.toString());
                            BibtexEntryType.ALL_TYPES.put(typ.getName().toLowerCase(), typ);
                        }

                    }
                }

                frame.output(Globals.lang("Opened database") + " '" + fileName +
                        "' " + Globals.lang("with") + " " +
                        db.getEntryCount() + " " + Globals.lang("entries") + ".");

            } catch (Exception ex) {
                //ex.printStackTrace();
                Util.showQuickErrorDialog(frame, Globals.lang("Open database"), ex);
                /*
                JOptionPane.showMessageDialog
                        (frame, ex.getMessage(),
                                Globals.lang("Open database"), JOptionPane.ERROR_MESSAGE);
                                */
            }
        }
    }

    public static ParserResult loadDatabase(File fileToOpen, String encoding)
            throws IOException {

        // First we make a quick check to see if this looks like a BibTeX file:
        Reader reader;// = ImportFormatReader.getReader(fileToOpen, encoding);
        //if (!BibtexParser.isRecognizedFormat(reader))
        //    return null;

        // The file looks promising. Reinitialize the reader and go on:
        //reader = getReader(fileToOpen, encoding);

        // We want to check if there is a JabRef signature in the file, because that would tell us
        // which character encoding is used. However, to read the signature we must be using a compatible
        // encoding in the first place. Since the signature doesn't contain any fancy characters, we can
        // read it regardless of encoding, with either UTF8 or UTF-16. That's the hypothesis, at any rate.
        // 8 bit is most likely, so we try that first:
        Reader utf8Reader = ImportFormatReader.getReader(fileToOpen, "UTF8");
        String suppliedEncoding = checkForEncoding(utf8Reader);
        utf8Reader.close();
        // Now if that didn't get us anywhere, we check with the 16 bit encoding:
        if (suppliedEncoding == null) {
            Reader utf16Reader = ImportFormatReader.getReader(fileToOpen, "UTF-16");
            suppliedEncoding = checkForEncoding(utf16Reader);
            utf16Reader.close();
            //System.out.println("Result of UTF-16 test: "+suppliedEncoding);
        }

        if ((suppliedEncoding != null)) {
           try {
               reader = ImportFormatReader.getReader(fileToOpen, suppliedEncoding);
               encoding = suppliedEncoding; // Just so we put the right info into the ParserResult.
           } catch (IOException ex) {
                reader = ImportFormatReader.getReader(fileToOpen, encoding); // The supplied encoding didn't work out, so we use the default.
            }
        } else {
            // We couldn't find a header with info about encoding. Use default:
            reader = ImportFormatReader.getReader(fileToOpen, encoding);
        }

        BibtexParser bp = new BibtexParser(reader);

        ParserResult pr = bp.parse();
        pr.setEncoding(encoding);

        return pr;
    }

    private static String checkForEncoding(Reader reader) {
        String suppliedEncoding = null;
        StringBuffer headerText = new StringBuffer();
        try {
            boolean keepon = true;
            int piv = 0;
            int c;

            while (keepon) {
                c = reader.read();
                headerText.append((char) c);
                if (((piv == 0) && Character.isWhitespace((char) c))
                        || (c == GUIGlobals.SIGNATURE.charAt(piv)))
                    piv++;
                else //if (((char)c) == '@')
                    keepon = false;
                //System.out.println(headerText.toString());
                found:
                if (piv == GUIGlobals.SIGNATURE.length()) {
                    keepon = false;

                    //if (headerText.length() > GUIGlobals.SIGNATURE.length())
                    //    System.out.println("'"+headerText.toString().substring(0, headerText.length()-GUIGlobals.SIGNATURE.length())+"'");
                    // Found the signature. The rest of the line is unknown, so we skip
                    // it:
                    while (reader.read() != '\n') ;

                    // Then we must skip the "Encoding: "
                    for (int i = 0; i < GUIGlobals.encPrefix.length(); i++) {
                        if (reader.read() != GUIGlobals.encPrefix.charAt(i))
                            break found; // No,
                        // it
                        // doesn't
                        // seem
                        // to
                        // match.
                    }

                    // If ok, then read the rest of the line, which should contain the
                    // name
                    // of the encoding:
                    StringBuffer sb = new StringBuffer();

                    while ((c = reader.read()) != '\n') sb.append((char) c);

                    suppliedEncoding = sb.toString();
                }
            }
        } catch (IOException ex) {
        }
        return suppliedEncoding;
    }
}

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
package net.sf.jabref.util;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.*;

import net.sf.jabref.Globals;

/**
 * This class redirects the System.err stream so it goes both the way it normally
 * goes, and into a ByteArrayOutputStream. We can use this stream to display any
 * error messages and stack traces to the user. Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 *
 * It also offers a separate tab for the log output.
 *
 * User: alver
 * Date: Mar 1, 2006
 * Time: 11:13:03 PM
 */
public class ErrorConsole  extends Handler {

    ByteArrayOutputStream errByteStream = new ByteArrayOutputStream();
    ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
    
    ArrayList<String> logOutput = new ArrayList<String>();
    String logOutputCache = "";
    boolean logOutputCacheRefreshNeeded = true;
    SimpleFormatter fmt = new SimpleFormatter();
    private static final int MAXLOGLINES = 500;
    
    private static ErrorConsole instance = null;


    public static ErrorConsole getInstance() {
        if (instance == null)
            instance = new ErrorConsole();

        return instance;
    }

    private ErrorConsole() {
        PrintStream myErr = new PrintStream(errByteStream);
        PrintStream tee = new TeeStream(System.err, myErr);
        System.setErr(tee);
        myErr = new PrintStream(outByteStream);
        tee = new TeeStream(System.out, myErr);
        System.setOut(tee);
    }

    private String getErrorMessages() {
        return errByteStream.toString();
    }

    private String getOutput() {
        return outByteStream.toString();
    }
    
    private String getLog() {
    	if (logOutputCacheRefreshNeeded) {
    		StringBuilder sb = new StringBuilder();
    		for(String line: logOutput) {
    			sb.append(line);
    		}
    		logOutputCache = sb.toString();
    	}
    	return logOutputCache;
    }
    
    /**
     * 
     * @param tabbed the tabbed pane to add the tab to
     * @param output the text to display in the tab
     * @param ifEmpty Text to output if textbox is emtpy. may be null
     */
    private void addTextArea(JTabbedPane tabbed, String title, String output, String ifEmpty) {
        JTextArea ta = new JTextArea(output);
        ta.setEditable(false);
        if ((ifEmpty!=null) && (ta.getText().length() == 0)) {
            ta.setText(ifEmpty);
        }
        JScrollPane sp = new JScrollPane(ta);
        tabbed.addTab(title, sp);
    }
    
    public void displayErrorConsole(JFrame parent) {
        JTabbedPane tabbed = new JTabbedPane();
        
        addTextArea(tabbed, Globals.lang("Output"), getOutput(), null);
        addTextArea(tabbed, Globals.lang("Exceptions"), getErrorMessages(),
        		Globals.lang("No exceptions have ocurred."));
        addTextArea(tabbed, Globals.lang("Log"), getLog(), null);

        tabbed.setPreferredSize(new Dimension(500,500));

        JOptionPane.showMessageDialog(parent,  tabbed,
                Globals.lang("Program output"), JOptionPane.ERROR_MESSAGE);
    }

    class ErrorConsoleAction extends AbstractAction {
        JFrame frame;
        public ErrorConsoleAction(JFrame frame) {
            super(Globals.menuTitle("Show error console"));
            putValue(SHORT_DESCRIPTION, Globals.lang("Display all error messages"));
            this.frame = frame;
        }

        public void actionPerformed(ActionEvent e) {
            displayErrorConsole(frame);
        }
    }

    public AbstractAction getAction(JFrame parent) {
        return new ErrorConsoleAction(parent);
    }

    // All writes to this print stream are copied to two print streams
    public class TeeStream extends PrintStream {
        PrintStream out;
        public TeeStream(PrintStream out1, PrintStream out2) {
            super(out1);
            this.out = out2;
        }
        public void write(byte buf[], int off, int len) {
            try {
                super.write(buf, off, len);
                out.write(buf, off, len);
            } catch (Exception e) {
            }
        }
        public void flush() {
            super.flush();
            out.flush();
        }
    }
    
    /* * * methods for Logging (required by Handler) * * */

	@Override
    public void close() throws SecurityException {
    }

	@Override
    public void flush() {
    }

	@Override
    public void publish(LogRecord record) {
		String msg = fmt.format(record);
		logOutput.add(msg);
		if (logOutput.size() < MAXLOGLINES) {
			// if we did not yet reach MAXLOGLINES, we just append the string to the cache
			logOutputCache = logOutputCache + msg;
		} else {
			// if we reached MAXLOGLINES, we switch to the "real" caching method and remove old lines 
			logOutputCacheRefreshNeeded = true;
			while (logOutput.size() > MAXLOGLINES) {
				// if log is too large, remove first line
				// we need a while loop as the formatter may output more than one line
				logOutput.remove(0);
			}
		}
		logOutputCacheRefreshNeeded = true;
    }
}

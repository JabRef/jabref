package antlr.build;

import java.util.*;
import java.io.*;

/** Adapted from JavaWorld article by Michael Daconta */
class StreamScarfer extends Thread
{
    InputStream is;
    String type;
    Tool tool;

    StreamScarfer(InputStream is, String type, Tool tool) {
	this.is = is;
	this.type = type;
	this.tool = tool;
    }

    public void run() {
	try {
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line=null;
	    while ( (line = br.readLine()) != null) {
		if ( type==null || type.equals("stdout") ) {
		    tool.stdout(line);
		}
		else {
		    tool.stderr(line);
		}
	    }
	}
	catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }
}

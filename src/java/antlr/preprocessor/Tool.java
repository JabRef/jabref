package antlr.preprocessor;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.*;
import antlr.collections.impl.Vector;
import java.util.Enumeration;

/** Tester for the preprocessor */
public class Tool {
    protected Hierarchy theHierarchy;
    protected String grammarFileName;
    protected String[] args;
    protected int nargs;		// how many args in new args list
    protected Vector grammars;
    protected antlr.Tool antlrTool;

    public Tool(antlr.Tool t, String[] args) {
        antlrTool = t;
        processArguments(args);
    }

    public static void main(String[] args) {
        antlr.Tool antlrTool = new antlr.Tool();
        Tool theTool = new Tool(antlrTool, args);
        theTool.preprocess();
        String[] a = theTool.preprocessedArgList();
        for (int i = 0; i < a.length; i++) {
            System.out.print(" " + a[i]);
        }
        System.out.println();
    }

    public boolean preprocess() {
        if (grammarFileName == null) {
            antlrTool.toolError("no grammar file specified");
            return false;
        }
        if (grammars != null) {
            theHierarchy = new Hierarchy(antlrTool);
            for (Enumeration e = grammars.elements(); e.hasMoreElements();) {
                String f = (String)e.nextElement();
                try {
                    theHierarchy.readGrammarFile(f);
                }
                catch (FileNotFoundException fe) {
                    antlrTool.toolError("file " + f + " not found");
                    return false;
                }
            }
        }

        // do the actual inheritance stuff
        boolean complete = theHierarchy.verifyThatHierarchyIsComplete();
        if (!complete)
            return false;
        theHierarchy.expandGrammarsInFile(grammarFileName);
        GrammarFile gf = theHierarchy.getFile(grammarFileName);
        String expandedFileName = gf.nameForExpandedGrammarFile(grammarFileName);

        // generate the output file if necessary
        if (expandedFileName.equals(grammarFileName)) {
            args[nargs++] = grammarFileName;			// add to argument list
        }
        else {
            try {
                gf.generateExpandedFile(); 				// generate file to feed ANTLR
                args[nargs++] = antlrTool.getOutputDirectory() +
                    System.getProperty("file.separator") +
                    expandedFileName;		// add to argument list
            }
            catch (IOException io) {
                antlrTool.toolError("cannot write expanded grammar file " + expandedFileName);
                return false;
            }
        }
        return true;
    }

    /** create new arg list with correct length to pass to ANTLR */
    public String[] preprocessedArgList() {
        String[] a = new String[nargs];
        System.arraycopy(args, 0, a, 0, nargs);
        args = a;
        return args;
    }

    /** Process -glib options and grammar file.  Create a new args list
     *  that does not contain the -glib option.  The grammar file name
     *  might be modified and, hence, is not added yet to args list.
     */
    private void processArguments(String[] incomingArgs) {
		 this.nargs = 0;
		 this.args = new String[incomingArgs.length];
		 for (int i = 0; i < incomingArgs.length; i++) {
			 if ( incomingArgs[i].length() == 0 )
			 {
				 antlrTool.warning("Zero length argument ignoring...");
				 continue;
			 }
			 if (incomingArgs[i].equals("-glib")) {
				 // if on a pc and they use a '/', warn them
				 if (File.separator.equals("\\") &&
					  incomingArgs[i].indexOf('/') != -1) {
					 antlrTool.warning("-glib cannot deal with '/' on a PC: use '\\'; ignoring...");
				 }
				 else {
					 grammars = antlrTool.parseSeparatedList(incomingArgs[i + 1], ';');
					 i++;
				 }
			 }
			 else if (incomingArgs[i].equals("-o")) {
				 args[this.nargs++] = incomingArgs[i];
				 if (i + 1 >= incomingArgs.length) {
					 antlrTool.error("missing output directory with -o option; ignoring");
				 }
				 else {
					 i++;
					 args[this.nargs++] = incomingArgs[i];
					 antlrTool.setOutputDirectory(incomingArgs[i]);
				 }
			 }
			 else if (incomingArgs[i].charAt(0) == '-') {
				 args[this.nargs++] = incomingArgs[i];
			 }
			 else {
				 // Must be the grammar file
				 grammarFileName = incomingArgs[i];
				 if (grammars == null) {
					 grammars = new Vector(10);
				 }
				 grammars.appendElement(grammarFileName);	// process it too
				 if ((i + 1) < incomingArgs.length) {
					 antlrTool.warning("grammar file must be last; ignoring other arguments...");
					 break;
				 }
			 }
		 }
    }
}

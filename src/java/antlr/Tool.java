package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import java.io.*;
import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
public class Tool {
    public static final String version = "2.7.1";

    /** Object that handles analysis errors */
    ToolErrorHandler errorHandler;

    /** Was there an error during parsing or analysis? */
    protected boolean hasError = false;

    /** Generate diagnostics? (vs code) */
    boolean genDiagnostics = false;

    /** Generate HTML vs code? */
    boolean genHTML = false;

    /** Current output directory for generated files */
    protected static String outputDir = ".";

    // Grammar input
    String grammarFile;
    transient Reader f = new InputStreamReader(System.in);
    // SAS: changed for proper text io
    //	transient DataInputStream in = null;

    protected static String literalsPrefix = "LITERAL_";
    protected static boolean upperCaseMangledLiterals = false;

    /** C++ file level options */
    protected static NameSpace nameSpace = null;
    protected static String namespaceAntlr = null;
    protected static String namespaceStd = null;
    protected static boolean genHashLines = true;

    private static BitSet cmdLineArgValid = new BitSet();

    /** Construct a new Tool. */
    public Tool() {
	errorHandler = new DefaultToolErrorHandler();
    }

    public static void setFileLineFormatter(FileLineFormatter formatter) {
	FileLineFormatter.setFormatter(formatter);
    }

    private static void checkForInvalidArguments(String[] args, BitSet cmdLineArgValid) {
	// check for invalid command line args
	for (int a = 0; a < args.length; a++) {
	    if (!cmdLineArgValid.member(a)) {
		warning("invalid command-line argument: " + args[a] + "; ignored");
	    }
	}
    }

    /** This example is from the book _Java in a Nutshell_ by David
     * Flanagan.  Written by David Flanagan.  Copyright (c) 1996
     * O'Reilly & Associates.  You may study, use, modify, and
     * distribute this example for any purpose.  This example is
     * provided WITHOUT WARRANTY either expressed or implied.  */
    public static void copyFile(String source_name, String dest_name)
	throws IOException
    {
	File source_file = new File(source_name);
	File destination_file = new File(dest_name);
	FileReader source = null; // SAS: changed for proper text io
	FileWriter destination = null;
	char[] buffer;
	int bytes_read;

	try {
	    // First make sure the specified source file
	    // exists, is a file, and is readable.
	    if (!source_file.exists() || !source_file.isFile())
		throw new FileCopyException("FileCopy: no such source file: " +
					    source_name);
	    if (!source_file.canRead())
		throw new FileCopyException("FileCopy: source file " +
					    "is unreadable: " + source_name);

	    // If the destination exists, make sure it is a writeable file
	    // and ask before overwriting it.  If the destination doesn't
	    // exist, make sure the directory exists and is writeable.
	    if (destination_file.exists()) {
		if (destination_file.isFile()) {
		    DataInputStream in = new DataInputStream(System.in);
		    String response;

		    if (!destination_file.canWrite())
			throw new FileCopyException("FileCopy: destination " +
						    "file is unwriteable: " + dest_name);
		    /*
		      System.out.print("File " + dest_name +
		      " already exists.  Overwrite? (Y/N): ");
		      System.out.flush();
		      response = in.readLine();
		      if (!response.equals("Y") && !response.equals("y"))
		      throw new FileCopyException("FileCopy: copy cancelled.");
		    */
		}
		else{
		    throw new FileCopyException("FileCopy: destination "
						+ "is not a file: " +  dest_name);
		}
	    }
	    else {
		File parentdir = parent(destination_file);
		if (!parentdir.exists())
		    throw new FileCopyException("FileCopy: destination "
						+ "directory doesn't exist: " + dest_name);
		if (!parentdir.canWrite())
		    throw new FileCopyException("FileCopy: destination "
						+ "directory is unwriteable: " + dest_name);
	    }

	    // If we've gotten this far, then everything is okay; we can
	    // copy the file.
	    source = new FileReader(source_file);
	    destination = new FileWriter(destination_file);
	    buffer = new char[1024];
	    while(true) {
		bytes_read = source.read(buffer,0,1024);
		if (bytes_read == -1) break;
		destination.write(buffer, 0, bytes_read);
	    }
	}
	// No matter what happens, always close any streams we've opened.
	finally {
	    if (source != null)
		try { source.close(); } catch (IOException e) { ; }
	    if (destination != null)
		try { destination.close(); } catch (IOException e) { ; }
	}
    }

    /** Perform processing on the grammar file.  Can only be called
     * from main() @param args The command-line arguments passed to
     * main()
     */
    protected void doEverything(String[] args) {
	// SAS: removed "private" so subclass can call
	//      (The subclass is for the VAJ interface)
	// run the preprocessor to handle inheritance first.
	antlr.preprocessor.Tool preTool = new antlr.preprocessor.Tool(this, args);
	if ( !preTool.preprocess() ) {
	    System.exit(1);
	}
	String[] modifiedArgs = preTool.preprocessedArgList();

	// process arguments for the Tool
	processArguments(modifiedArgs);

	f = getGrammarReader();

	TokenBuffer tokenBuf = new TokenBuffer(new ANTLRLexer(f));
	LLkAnalyzer analyzer = new LLkAnalyzer(this);
	MakeGrammar behavior = new MakeGrammar(this, args, analyzer);

	try {
	    ANTLRParser p = new ANTLRParser(tokenBuf, behavior, this);
	    p.setFilename(grammarFile);
	    p.grammar();
	    if (hasError) {
		System.err.println("Exiting due to errors.");
		System.exit(1);
	    }
	    checkForInvalidArguments(modifiedArgs, cmdLineArgValid);

	    // Create the right code generator according to the "language" option
	    CodeGenerator codeGen;

	    // SAS: created getLanguage() method so subclass can override
	    //      (necessary for VAJ interface)
	    String codeGenClassName = "antlr." + getLanguage(behavior) + "CodeGenerator";
	    try {
		Class codeGenClass = Class.forName(codeGenClassName);
		codeGen = (CodeGenerator)codeGenClass.newInstance();
		codeGen.setBehavior(behavior);
		codeGen.setAnalyzer(analyzer);
		codeGen.setTool(this);
		codeGen.gen();
	    }
	    catch (ClassNotFoundException cnfe) {
		panic("Cannot instantiate code-generator: " + codeGenClassName);
	    }
	    catch (InstantiationException ie) {
		panic("Cannot instantiate code-generator: " + codeGenClassName);
	    }
	    catch (IllegalArgumentException ie) {
		panic("Cannot instantiate code-generator: " + codeGenClassName);
	    }
	    catch (IllegalAccessException iae) {
		panic("code-generator class '" + codeGenClassName + "' is not accessible");
	    }
	}
	catch (RecognitionException pe) {
	    System.err.println("Unhandled parser error: " + pe.getMessage());
	    System.exit(1);
	}
	catch (TokenStreamException io) {
	    System.err.println("TokenStreamException: " + io.getMessage());
	    System.exit(1);
	}
	System.exit(0);
    }

    /** Issue an error
     * @param s The message
     */
    public void error(String s) {
	hasError = true;
	System.err.println("error: "+s);
    }

    /** Issue an error with line number information
     * @param s The message
     * @param file The file that has the error
     * @param line The grammar file line number on which the error occured
     */
    public void error(String s, String file, int line) {
	hasError = true;
	if ( file!=null ) {
	    System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+s);
	}
	else {
	    System.err.println("line "+line+": "+s);
	}
    }

    /** When we are 1.1 compatible...
public static Object factory2 (String p, Object[] initargs) {
  Class c;
  Object o = null;
  try {
	int argslen = initargs.length;
	Class cl[] = new Class[argslen];
	for (int i=0;i&lt;argslen;i++) {
	  cl[i] = Class.forName(initargs[i].getClass().getName());
	}
	c = Class.forName (p);
	Constructor con = c.getConstructor (cl);
	o = con.newInstance (initargs);
  } catch (Exception e) {
	System.err.println ("Can't make a " + p);
  }
  return o;
}
*/
    public static Object factory(String p) {
	Class c;
	Object o=null;
	try {
	    c = Class.forName(p);// get class def
	    o = c.newInstance(); // make a new one
	}
	catch (Exception e) {
	    // either class not found,
	    // class is interface/abstract, or
	    // class or initializer is not accessible.
	    warning("Can't create an object of type " + p);
	    return null;
	}
	return o;
    }

    public static String fileMinusPath(String f) {
	String separator = System.getProperty("file.separator");
	int endOfPath = f.lastIndexOf(separator);
	if ( endOfPath == -1 ) {
	    return f;	// no path found
	}
	return f.substring(endOfPath+1);
    }

    /** Determine the language used for this run of ANTLR
     *  This was made a method so the subclass can override it
     */
    public String getLanguage(MakeGrammar behavior) {
	if (genDiagnostics) {
	    return "Diagnostic";
	}
	if (genHTML) {
	    return "HTML";
	}
	return behavior.language;
    }

    public static String getOutputDirectory() { return outputDir; }

    private static void help() {
	System.err.println("usage: java antlr.Tool [args] file.g");
	System.err.println("  -o outputDir       specify output directory where all output generated.");
	System.err.println("  -glib superGrammar specify location of supergrammar file.");
	System.err.println("  -debug             launch the ParseView debugger upon parser invocation.");
	System.err.println("  -html              generate an html file from your grammar (minus actions).");
	System.err.println("  -diagnostic        generate a textfile with diagnostics.");
	System.err.println("  -trace             have all rules call traceIn/traceOut.");
	System.err.println("  -traceParser       have parser rules call traceIn/traceOut.");
	System.err.println("  -traceLexer        have lexer rules call traceIn/traceOut.");
	System.err.println("  -traceTreeParser   have tree parser rules call traceIn/traceOut.");
    }

    public static void main(String[] args) {
	System.err.println("ANTLR Parser Generator   Version "+
			   Tool.version+"   1989-2000 jGuru.com");
	try {
	    if ( args.length==0 ) {
		help();
	    }
	    Tool theTool = new Tool();
	    theTool.doEverything(args);
	    theTool = null;
	}
	catch (Exception e) {
	    System.err.println(System.getProperty("line.separator")+
			       System.getProperty("line.separator"));
	    System.err.println("#$%%*&@# internal error: "+e.toString());
	    System.err.println("[complain to nearest government official");
	    System.err.println(" or send hate-mail to parrt@jguru.com;");
	    System.err.println(" please send stack trace with report.]"+
			       System.getProperty("line.separator"));
	    e.printStackTrace();
	}
	System.exit(0);
    }

    public static PrintWriter openOutputFile(String f) throws IOException {
	return new PrintWriter(new FileWriter(outputDir+System.getProperty("file.separator")+f));
    }

    public Reader getGrammarReader() {
	try {
	    if (grammarFile != null) {
		f = new FileReader(grammarFile);
	    }
	}
	catch (IOException e) {
	    panic("Error: cannot open grammar file " + grammarFile);
	    help();
	    System.exit(1);
	}
	return f;
    }

    /** Issue an unknown fatal error */
    public static void panic() {
	System.err.println("panic");
	System.exit(1);
    }

    /** Issue a fatal error message
     * @param s The message
     */
    public static void panic(String s) {
	System.err.println("panic: "+s);
	System.exit(1);
    }

    // File.getParent() can return null when the file is specified without
    // a directory or is in the root directory.
    // This method handles those cases.
    public static File parent(File f) {
	String dirname = f.getParent();
	if (dirname == null) {
	    if (f.isAbsolute()) return new File(File.separator);
	    else return new File(System.getProperty("user.dir"));
	}
	return new File(dirname);
    }

    /** Parse a list such as "f1.g;f2.g;..." and return a Vector
     *  of the elements.
     */
    public static Vector parseSeparatedList(String list, char separator) {
	Vector v = new Vector(10);
	StringBuffer buf = new StringBuffer(100);
	int i=0;
	while ( i<list.length() ) {
	    while ( i<list.length() && list.charAt(i)!=separator ) {
		buf.append(list.charAt(i));
		i++;
	    }
	    // add element to vector
	    v.appendElement(buf.toString());
	    buf.setLength(0);
	    // must be a separator or finished.
	    if ( i<list.length() ) {	// not done?
		i++;	// skip separator
	    }
	}
	if ( v.size()==0 ) return null;
	return v;
    }

    /** given a filename, strip off the directory prefix (if any)
     *  and return it.  Return "./" if f has no dir prefix.
     */
    public static String pathToFile(String f) {
	String separator = System.getProperty("file.separator");
	int endOfPath = f.lastIndexOf(separator);
	if ( endOfPath == -1 ) {
	    // no path, use current directory
	    return "."+System.getProperty("file.separator");
	}
	return f.substring(0, endOfPath+1);
    }

    /** Process the command-line arguments.  Can only be called by Tool.
     * @param args The command-line arguments passed to main()
     */
    private void processArguments(String[] args) {
	for (int i = 0; i < args.length; i++) {
	    if (args[i].equals("-diagnostic")) {
		genDiagnostics = true;
		genHTML = false;
		Tool.setArgOK(i);
	    } else {
		if (args[i].equals("-o")) {
		    Tool.setArgOK(i);
		    if (i + 1 >= args.length) {
			error("missing output directory with -o option; ignoring");
		    } else {
			i++;
			setOutputDirectory(args[i]);
			Tool.setArgOK(i);
		    }
		} else
		    if (args[i].equals("-html")) {
			genHTML = true;
			genDiagnostics = false;
			Tool.setArgOK(i);
		    } else {
			if (args[i].charAt(0) != '-') {
			    // Must be the grammar file
			    grammarFile = args[i];
			    Tool.setArgOK(i);
			}
		    }
	    }
	}
    }

    public static void setArgOK(int i) {
	cmdLineArgValid.add(i);
    }

    public static void setOutputDirectory(String o) { outputDir = o; }

    /** General-purpose utility function for removing
     * characters from back of string
     * @param s The string to process
     * @param c The character to remove
     * @return The resulting string
     */
    static public String stripBack(String s, char c) {
	while (s.length() > 0 && s.charAt(s.length()-1) == c)
	    {
		s = s.substring(0, s.length()-1);
	    }
	return s;
    }

    /** General-purpose utility function for removing
     * characters from back of string
     * @param s The string to process
     * @param remove A string containing the set of characters to remove
     * @return The resulting string
     */
    static public String stripBack(String s, String remove) {
	boolean changed;
	do {
	    changed = false;
	    for (int i = 0; i < remove.length(); i++) {
		char c = remove.charAt(i);
		while (s.length() > 0 && s.charAt(s.length()-1) == c)
		    {
			changed = true;
			s = s.substring(0, s.length()-1);
		    }
	    }
	} while (changed);
	return s;
    }

    /** General-purpose utility function for removing
     * characters from front of string
     * @param s The string to process
     * @param c The character to remove
     * @return The resulting string
     */
    static public String stripFront(String s, char c) {
	while (s.length() > 0 && s.charAt(0) == c) {
	    s = s.substring(1);
	}
	return s;
    }

    /** General-purpose utility function for removing
     * characters from front of string
     * @param s The string to process
     * @param remove A string containing the set of characters to remove
     * @return The resulting string
     */
    static public String stripFront(String s, String remove) {
	boolean changed;
	do {
	    changed = false;
	    for (int i = 0; i < remove.length(); i++) {
		char c = remove.charAt(i);
		while (s.length() > 0 && s.charAt(0) == c) {
		    changed = true;
		    s = s.substring(1);
		}
	    }
	} while (changed);
	return s;
    }

    /** General-purpose utility function for removing
     * characters from the front and back of string
     * @param s The string to process
     * @param head exact string to strip from head
     * @param tail exact string to strip from tail
     * @return The resulting string
     */
    public static String stripFrontBack(String src, String head, String tail) {
	int h = src.indexOf(head);
	int t = src.lastIndexOf(tail);
	if ( h==-1 || t==-1 ) return src;
	return src.substring(h+1,t);
    }

    /** Issue an error; used for general tool errors not for grammar stuff
     * @param s The message
     */
    public static void toolError(String s) {
	System.err.println("error: "+s);
    }

    /** Issue a warning
     * @param s the message
     */
    public static void warning(String s) {
	System.err.println("warning: "+s);
    }

    /** Issue a warning with line number information
     * @param s The message
     * @param file The file that has the warning
     * @param line The grammar file line number on which the warning occured
     */
    public static void warning(String s, String file, int line) {
	if ( file!=null ) {
	    System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+"warning:"+s);
	}
	else {
	    System.err.println("warning; line "+line+": "+s);
	}
    }

    /** Issue a warning with line number information
     * @param s The lines of the message
     * @param file The file that has the warning
     * @param line The grammar file line number on which the warning occured
     */
    public static void warning(String[] s, String file, int line) {
	if ( s==null || s.length==0 ) {
	    panic("bad multi-line message to Tool.warning");
	}
	if ( file!=null ) {
	    System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+"warning:"+s[0]);
	    for (int i=1; i<s.length; i++) {
		System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+s[i]);
	    }
	}
	else {
	    System.err.println("warning: line "+line+": "+s[0]);
	    for (int i=1; i<s.length; i++) {
		System.err.println("warning: line "+line+": "+s[i]);
	    }
	}
    }

    /**
     * Support C++ namespaces (for now).  Add a nested namespace name to the
     * current namespace.
     * DAW: David Wagner
     */
    public void setNameSpace(String name) {
	if ( null == nameSpace )
	    nameSpace = new NameSpace(stripFrontBack(name,"\"", "\""));
    }
}

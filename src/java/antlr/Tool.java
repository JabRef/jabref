package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.*;

import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
import antlr.PreservingFileWriter;
import antlr.Version;

public class Tool {
    public static String version = "";

    /** Object that handles analysis errors */
    ToolErrorHandler errorHandler;

    /** Was there an error during parsing or analysis? */
    protected boolean hasError = false;

    /** Generate diagnostics? (vs code) */
    boolean genDiagnostics = false;

    /** Generate DocBook vs code? */
    boolean genDocBook = false;

    /** Generate HTML vs code? */
    boolean genHTML = false;

    /** Current output directory for generated files */
    protected static String outputDir = ".";

    // Grammar input
    protected String grammarFile;
    transient Reader f = new InputStreamReader(System.in);
    // SAS: changed for proper text io
    //  transient DataInputStream in = null;

    protected static String literalsPrefix = "LITERAL_";
    protected static boolean upperCaseMangledLiterals = false;

    /** C++ file level options */
    protected NameSpace nameSpace = null;
    protected String namespaceAntlr = null;
    protected String namespaceStd = null;
    protected boolean genHashLines = true;
    protected boolean noConstructors = false;

    private BitSet cmdLineArgValid = new BitSet();

    /** Construct a new Tool. */
    public Tool() {
        errorHandler = new DefaultToolErrorHandler(this);
    }

    public String getGrammarFile() {
        return grammarFile;
    }

    public boolean hasError() {
        return hasError;
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public String getNamespaceStd() {
        return namespaceStd;
    }

    public String getNamespaceAntlr() {
        return namespaceAntlr;
    }

    public boolean getGenHashLines() {
        return genHashLines;
    }

    public String getLiteralsPrefix() {
        return literalsPrefix;
    }

    public boolean getUpperCaseMangledLiterals() {
        return upperCaseMangledLiterals;
    }

    public void setFileLineFormatter(FileLineFormatter formatter) {
        FileLineFormatter.setFormatter(formatter);
    }

    protected void checkForInvalidArguments(String[] args, BitSet cmdLineArgValid) {
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
    public void copyFile(String source_name, String dest_name)
        throws IOException {
        File source_file = new File(source_name);
        File destination_file = new File(dest_name);
        Reader source = null;
        Writer destination = null;
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
                else {
                    throw new FileCopyException("FileCopy: destination "
                                                + "is not a file: " + dest_name);
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
            source = new BufferedReader(new FileReader(source_file));
            destination = new BufferedWriter(new FileWriter(destination_file));

            buffer = new char[1024];
            while (true) {
                bytes_read = source.read(buffer, 0, 1024);
                if (bytes_read == -1) break;
                destination.write(buffer, 0, bytes_read);
            }
        }
            // No matter what happens, always close any streams we've opened.
        finally {
            if (source != null) {
                try {
                    source.close();
                }
                catch (IOException e) {
                    ;
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                }
                catch (IOException e) {
                    ;
                }
            }
        }
    }

    /** Perform processing on the grammar file.  Can only be called
     * from main() @param args The command-line arguments passed to
     * main().  This wrapper does the System.exit for use with command-line.
     */
    public void doEverythingWrapper(String[] args) {
        int exitCode = doEverything(args);
        System.exit(exitCode);
    }

    /** Process args and have ANTLR do it's stuff without calling System.exit.
     *  Just return the result code.  Makes it easy for ANT build tool.
     */
    public int doEverything(String[] args) {
        // run the preprocessor to handle inheritance first.

        // Start preprocessor. This strips generates an argument list
        // without -glib options (inside preTool)
        antlr.preprocessor.Tool preTool = new antlr.preprocessor.Tool(this, args);

        boolean preprocess_ok = preTool.preprocess();
        String[] modifiedArgs = preTool.preprocessedArgList();

        // process arguments for the Tool
        processArguments(modifiedArgs);
        if (!preprocess_ok) {
            return 1;
        }

        f = getGrammarReader();

        ANTLRLexer lexer = new ANTLRLexer(f);
        TokenBuffer tokenBuf = new TokenBuffer(lexer);
        LLkAnalyzer analyzer = new LLkAnalyzer(this);
        MakeGrammar behavior = new MakeGrammar(this, args, analyzer);

        try {
            ANTLRParser p = new ANTLRParser(tokenBuf, behavior, this);
            p.setFilename(grammarFile);
            p.grammar();
            if (hasError()) {
                fatalError("Exiting due to errors.");
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
            fatalError("Unhandled parser error: " + pe.getMessage());
        }
        catch (TokenStreamException io) {
            fatalError("TokenStreamException: " + io.getMessage());
        }
        return 0;
    }

    /** Issue an error
     * @param s The message
     */
    public void error(String s) {
        hasError = true;
        System.err.println("error: " + s);
    }

    /** Issue an error with line number information
     * @param s The message
     * @param file The file that has the error (or null)
     * @param line The grammar file line number on which the error occured (or -1)
     * @param column The grammar file column number on which the error occured (or -1)
     */
    public void error(String s, String file, int line, int column) {
        hasError = true;
        System.err.println(FileLineFormatter.getFormatter().
                           getFormatString(file, line, column) + s);
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
    public Object factory(String p) {
        Class c;
        Object o = null;
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

    public String fileMinusPath(String f) {
        String separator = System.getProperty("file.separator");
        int endOfPath = f.lastIndexOf(separator);
        if (endOfPath == -1) {
            return f;   // no path found
        }
        return f.substring(endOfPath + 1);
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
        if (genDocBook) {
            return "DocBook";
        }
        return behavior.language;
    }

    public String getOutputDirectory() {
        return outputDir;
    }

    private static void help() {
        System.err.println("usage: java antlr.Tool [args] file.g");
        System.err.println("  -o outputDir       specify output directory where all output generated.");
        System.err.println("  -glib superGrammar specify location of supergrammar file.");
        System.err.println("  -debug             launch the ParseView debugger upon parser invocation.");
        System.err.println("  -html              generate a html file from your grammar.");
        System.err.println("  -docbook           generate a docbook sgml file from your grammar.");
        System.err.println("  -diagnostic        generate a textfile with diagnostics.");
        System.err.println("  -trace             have all rules call traceIn/traceOut.");
        System.err.println("  -traceLexer        have lexer rules call traceIn/traceOut.");
        System.err.println("  -traceParser       have parser rules call traceIn/traceOut.");
        System.err.println("  -traceTreeParser   have tree parser rules call traceIn/traceOut.");
        System.err.println("  -h|-help|--help    this message");
    }

    public static void main(String[] args) {
        System.err.println("ANTLR Parser Generator   Version " +
                           Version.project_version + "   1989-2004 jGuru.com");
        version = Version.project_version;

        try {
            if (args.length == 0) {
                help();
                System.exit(1);
            }
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals("-h")
                    || args[i].equals("-help")
                    || args[i].equals("--help")
                ) {
                    help();
                    System.exit(1);
                }
            }

            Tool theTool = new Tool();
            theTool.doEverything(args);
            theTool = null;
        }
        catch (Exception e) {
            System.err.println(System.getProperty("line.separator") +
                               System.getProperty("line.separator"));
            System.err.println("#$%%*&@# internal error: " + e.toString());
            System.err.println("[complain to nearest government official");
            System.err.println(" or send hate-mail to parrt@jguru.com;");
            System.err.println(" please send stack trace with report.]" +
                               System.getProperty("line.separator"));
            e.printStackTrace();
        }
        System.exit(0);
    }

	/** This method is used by all code generators to create new output
	 * files. If the outputDir set by -o is not present it will be created here.
	 */
	public PrintWriter openOutputFile(String f) throws IOException {
		if( outputDir != "." ) {
			File out_dir = new File(outputDir);
			if( ! out_dir.exists() )
				out_dir.mkdirs();
		}
		return new PrintWriter(new PreservingFileWriter(outputDir + System.getProperty("file.separator") + f));
	}

    public Reader getGrammarReader() {
        Reader f = null;
        try {
            if (grammarFile != null) {
                f = new BufferedReader(new FileReader(grammarFile));
            }
        }
        catch (IOException e) {
            fatalError("cannot open grammar file " + grammarFile);
        }
        return f;
    }

    /** @since 2.7.2
     */
    public void reportException(Exception e, String message) {
        System.err.println(message == null ? e.getMessage()
                                           : message + ": " + e.getMessage());
    }

    /** @since 2.7.2
     */
    public void reportProgress(String message) {
        System.out.println(message);
    }

    /** An error occured that should stop the Tool from doing any work.
     *  The default implementation currently exits (via
     *  {@link java.lang.System.exit(int)} after printing an error message to
     *  <var>stderr</var>. However, the tools should expect that a subclass
     *  will override this to throw an unchecked exception such as
     *  {@link java.lang.IllegalStateException} or another subclass of
     *  {@link java.lang.RuntimeException}. <em>If this method is overriden,
     *  <strong>it must never return normally</strong>; i.e. it must always
     *  throw an exception or call System.exit</em>.
     *  @since 2.7.2
     *  @param s The message
     */
    public void fatalError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    /** Issue an unknown fatal error. <em>If this method is overriden,
     *  <strong>it must never return normally</strong>; i.e. it must always
     *  throw an exception or call System.exit</em>.
     *  @deprecated as of 2.7.2 use {@link #fatalError(String)}. By default
     *              this method executes <code>fatalError("panic");</code>.
     */
    public void panic() {
        fatalError("panic");
    }

    /** Issue a fatal error message. <em>If this method is overriden,
     *  <strong>it must never return normally</strong>; i.e. it must always
     *  throw an exception or call System.exit</em>.
     *  @deprecated as of 2.7.2 use {@link #fatalError(String)}. By defaykt
     *              this method executes <code>fatalError("panic: " + s);</code>.
     * @param s The message
     */
    public void panic(String s) {
        fatalError("panic: " + s);
    }

    // File.getParent() can return null when the file is specified without
    // a directory or is in the root directory.
    // This method handles those cases.
    public File parent(File f) {
        String dirname = f.getParent();
        if (dirname == null) {
            if (f.isAbsolute())
                return new File(File.separator);
            else
                return new File(System.getProperty("user.dir"));
        }
        return new File(dirname);
    }

    /** Parse a list such as "f1.g;f2.g;..." and return a Vector
     *  of the elements.
     */
    public static Vector parseSeparatedList(String list, char separator) {
        java.util.StringTokenizer st =
		new java.util.StringTokenizer(list, String.valueOf(separator));
        Vector v = new Vector(10);
        while ( st.hasMoreTokens() ) {
             v.appendElement(st.nextToken());
        }
        if (v.size() == 0) return null;
        return v;
    }

    /** given a filename, strip off the directory prefix (if any)
     *  and return it.  Return "./" if f has no dir prefix.
     */
    public String pathToFile(String f) {
        String separator = System.getProperty("file.separator");
        int endOfPath = f.lastIndexOf(separator);
        if (endOfPath == -1) {
            // no path, use current directory
            return "." + System.getProperty("file.separator");
        }
        return f.substring(0, endOfPath + 1);
    }

    /** <p>Process the command-line arguments.  Can only be called by Tool.
     * A bitset is collected of all correct arguments via setArgOk.</p>
     * @param args The command-line arguments passed to main()
     *
     */
    protected void processArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-diagnostic")) {
                genDiagnostics = true;
                genHTML = false;
                setArgOK(i);
            }
            else if (args[i].equals("-o")) {
                setArgOK(i);
                if (i + 1 >= args.length) {
                    error("missing output directory with -o option; ignoring");
                }
                else {
                    i++;
                    setOutputDirectory(args[i]);
                    setArgOK(i);
                }
            }
            else if (args[i].equals("-html")) {
                genHTML = true;
                genDiagnostics = false;
                setArgOK(i);
            }
            else if (args[i].equals("-docbook")) {
                genDocBook = true;
                genDiagnostics = false;
                setArgOK(i);
            }
            else {
                if (args[i].charAt(0) != '-') {
                    // Must be the grammar file
                    grammarFile = args[i];
                    setArgOK(i);
                }
            }
        }
    }

    public void setArgOK(int i) {
        cmdLineArgValid.add(i);
    }

    public void setOutputDirectory(String o) {
        outputDir = o;
    }

    /** Issue an error; used for general tool errors not for grammar stuff
     * @param s The message
     */
    public void toolError(String s) {
        System.err.println("error: " + s);
    }

    /** Issue a warning
     * @param s the message
     */
    public void warning(String s) {
        System.err.println("warning: " + s);
    }

    /** Issue a warning with line number information
     * @param s The message
     * @param file The file that has the warning (or null)
     * @param line The grammar file line number on which the warning occured (or -1)
     * @param column The grammar file line number on which the warning occured (or -1)
     */
    public void warning(String s, String file, int line, int column) {
        System.err.println(FileLineFormatter.getFormatter().
                           getFormatString(file, line, column) + "warning:" + s);
    }

    /** Issue a warning with line number information
     * @param s The lines of the message
     * @param file The file that has the warning
     * @param line The grammar file line number on which the warning occured
     */
    public void warning(String[] s, String file, int line, int column) {
        if (s == null || s.length == 0) {
            panic("bad multi-line message to Tool.warning");
        }
        System.err.println(FileLineFormatter.getFormatter().
                           getFormatString(file, line, column) + "warning:" + s[0]);
        for (int i = 1; i < s.length; i++) {
            System.err.println(FileLineFormatter.getFormatter().
                               getFormatString(file, line, column) + "    " + s[i]);
        }
    }

    /**
     * Support C++ & C# namespaces (for now).
     * C++: Add a nested namespace name to the current namespace.
     * C# : Specify an enclosing namespace for the generated code.
     * DAW: David Wagner -- C# support by kunle odutola
     */
    public void setNameSpace(String name) {
        if (null == nameSpace)
            nameSpace = new NameSpace(StringUtils.stripFrontBack(name, "\"", "\""));
    }
}

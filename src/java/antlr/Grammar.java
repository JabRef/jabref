package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;

/**A Grammar holds a set of rules (which are stored
 * in a symbol table).  Most of the time a grammar
 * needs a code generator and an LLkAnalyzer too.
 */
public abstract class Grammar {
    protected Tool antlrTool;
    protected CodeGenerator generator;
    protected LLkGrammarAnalyzer theLLkAnalyzer;
    protected Hashtable symbols;
    protected boolean buildAST = false;
    protected boolean analyzerDebug = false;
    protected boolean interactive = false;
    protected String superClass = null;

    /** The token manager associated with the grammar, if any.
     // The token manager is responsible for maintaining the set of valid tokens, and
     // is conceptually shared between the lexer and parser.  This may be either a
     // LexerGrammar or a ImportVocabTokenManager.
     */
    protected TokenManager tokenManager;

    /** The name of the export vocabulary...used to generate the output
     *  token types interchange file.
     */
    protected String exportVocab = null;

    /** The name of the import vocabulary.  "Initial conditions"
     */
    protected String importVocab = null;

    // Mapping from String keys to Token option values
    protected Hashtable options;
    // Vector of RuleSymbol entries
    protected Vector rules;

    protected Token preambleAction = new CommonToken(Token.INVALID_TYPE, "");
    protected String className = null;
    protected String fileName = null;
    protected Token classMemberAction = new CommonToken(Token.INVALID_TYPE, "");
    protected boolean hasSyntacticPredicate = false;
    protected boolean hasUserErrorHandling = false;

    // max lookahead that can be attempted for this parser.
    protected int maxk = 1;

    // options
    protected boolean traceRules = false;
    protected boolean debuggingOutput = false;
    protected boolean defaultErrorHandler = true;

    protected String comment = null; // javadoc comment

    public Grammar(String className_, Tool tool_, String superClass) {
        className = className_;
        antlrTool = tool_;
        symbols = new Hashtable();
        options = new Hashtable();
        rules = new Vector(100);
        this.superClass = superClass;
    }

    /** Define a rule */
    public void define(RuleSymbol rs) {
        rules.appendElement(rs);
        // add the symbol to the rules hash table
        symbols.put(rs.getId(), rs);
    }

    /** Top-level call to generate the code for this grammar */
    public abstract void generate() throws IOException;

    protected String getClassName() {
        return className;
    }

    /* Does this grammar have a default error handler? */
    public boolean getDefaultErrorHandler() {
        return defaultErrorHandler;
    }

    public String getFilename() {
        return fileName;
    }

    /** Get an integer option.  Given the name of the option find its
     * associated integer value.  If the associated value is not an integer or
     * is not in the table, then throw an exception of type NumberFormatException.
     * @param key The name of the option
     * @return The value associated with the key.
     */
    public int getIntegerOption(String key) throws NumberFormatException {
        Token t = (Token)options.get(key);
        if (t == null || t.getType() != ANTLRTokenTypes.INT) {
            throw new NumberFormatException();
        }
        else {
            return Integer.parseInt(t.getText());
        }
    }

    /** Get an option.  Given the name of the option find its associated value.
     * @param key The name of the option
     * @return The value associated with the key, or null if the key has not been set.
     */
    public Token getOption(String key) {
        return (Token)options.get(key);
    }

    // Get name of class from which generated parser/lexer inherits
    protected abstract String getSuperClass();

    public GrammarSymbol getSymbol(String s) {
        return (GrammarSymbol)symbols.get(s);
    }

    public Enumeration getSymbols() {
        return symbols.elements();
    }

    /** Check the existence of an option in the table
     * @param key The name of the option
     * @return true if the option is in the table
     */
    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    /** Is a rule symbol defined? (not used for tokens) */
    public boolean isDefined(String s) {
        return symbols.containsKey(s);
    }

    /**Process command line arguments.  Implemented in subclasses */
    public abstract void processArguments(String[] args);

    public void setCodeGenerator(CodeGenerator gen) {
        generator = gen;
    }

    public void setFilename(String s) {
        fileName = s;
    }

    public void setGrammarAnalyzer(LLkGrammarAnalyzer a) {
        theLLkAnalyzer = a;
    }

    /** Set a generic option.
     * This associates a generic option key with a Token value.
     * No validation is performed by this method, although users of the value
     * (code generation and/or analysis) may require certain formats.
     * The value is stored as a token so that the location of an error
     * can be reported.
     * @param key The name of the option.
     * @param value The value to associate with the key.
     * @return true if the option was a valid generic grammar option, false o/w
     */
    public boolean setOption(String key, Token value) {
        options.put(key, value);
        String s = value.getText();
        int i;
        if (key.equals("k")) {
            try {
                maxk = getIntegerOption("k");
				if ( maxk<=0 ) {
					antlrTool.error("option 'k' must be greater than 0 (was " +
									value.getText() + ")",
									getFilename(),
									value.getLine(),
									value.getColumn());
					maxk = 1;
				}
            }
            catch (NumberFormatException e) {
                antlrTool.error("option 'k' must be an integer (was " + value.getText() + ")", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("codeGenMakeSwitchThreshold")) {
            try {
                i = getIntegerOption("codeGenMakeSwitchThreshold");
            }
            catch (NumberFormatException e) {
                antlrTool.error("option 'codeGenMakeSwitchThreshold' must be an integer", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("codeGenBitsetTestThreshold")) {
            try {
                i = getIntegerOption("codeGenBitsetTestThreshold");
            }
            catch (NumberFormatException e) {
                antlrTool.error("option 'codeGenBitsetTestThreshold' must be an integer", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("defaultErrorHandler")) {
            if (s.equals("true")) {
                defaultErrorHandler = true;
            }
            else if (s.equals("false")) {
                defaultErrorHandler = false;
            }
            else {
                antlrTool.error("Value for defaultErrorHandler must be true or false", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("analyzerDebug")) {
            if (s.equals("true")) {
                analyzerDebug = true;
            }
            else if (s.equals("false")) {
                analyzerDebug = false;
            }
            else {
                antlrTool.error("option 'analyzerDebug' must be true or false", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("codeGenDebug")) {
            if (s.equals("true")) {
                analyzerDebug = true;
            }
            else if (s.equals("false")) {
                analyzerDebug = false;
            }
            else {
                antlrTool.error("option 'codeGenDebug' must be true or false", getFilename(), value.getLine(), value.getColumn());
            }
            return true;
        }
        if (key.equals("classHeaderSuffix")) {
            return true;
        }
        if (key.equals("classHeaderPrefix")) {
            return true;
        }
        if (key.equals("namespaceAntlr")) {
            return true;
        }
        if (key.equals("namespaceStd")) {
            return true;
        }
        if (key.equals("genHashLines")) {
            return true;
        }
        if (key.equals("noConstructors")) {
            return true;
        }
        return false;
    }

    public void setTokenManager(TokenManager tokenManager_) {
        tokenManager = tokenManager_;
    }

    /** Print out the grammar without actions */
    public String toString() {
        StringBuffer buf = new StringBuffer(20000);
        Enumeration ids = rules.elements();
        while (ids.hasMoreElements()) {
            RuleSymbol rs = (RuleSymbol)ids.nextElement();
            if (!rs.id.equals("mnextToken")) {
                buf.append(rs.getBlock().toString());
                buf.append("\n\n");
            }
        }
        return buf.toString();
    }

}

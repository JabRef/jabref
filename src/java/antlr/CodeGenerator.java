package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileWriter;

import antlr.collections.impl.Vector;
import antlr.collections.impl.BitSet;

/**A generic ANTLR code generator.  All code generators
 * Derive from this class.
 *
 * <p>
 * A CodeGenerator knows about a Grammar data structure and
 * a grammar analyzer.  The Grammar is walked to generate the
 * appropriate code for both a parser and lexer (if present).
 * This interface may change slightly so that the lexer is
 * itself living inside of a Grammar object (in which case,
 * this class generates only one recognizer).  The main method
 * to call is <tt>gen()</tt>, which initiates all code gen.
 *
 * <p>
 * The interaction of the code generator with the analyzer is
 * simple: each subrule block calls deterministic() before generating
 * code for the block.  Method deterministic() sets lookahead caches
 * in each Alternative object.  Technically, a code generator
 * doesn't need the grammar analyzer if all lookahead analysis
 * is done at runtime, but this would result in a slower parser.
 *
 * <p>
 * This class provides a set of support utilities to handle argument
 * list parsing and so on.
 *
 * @author  Terence Parr, John Lilley
 * @version 2.00a
 * @see     antlr.JavaCodeGenerator
 * @see     antlr.DiagnosticCodeGenerator
 * @see     antlr.LLkAnalyzer
 * @see     antlr.Grammar
 * @see     antlr.AlternativeElement
 * @see     antlr.Lookahead
 */
public abstract class CodeGenerator {
    protected antlr.Tool antlrTool;

    /** Current tab indentation for code output */
    protected int tabs = 0;

    /** Current output Stream */
    transient protected PrintWriter currentOutput; // SAS: for proper text i/o

    /** The grammar for which we generate code */
    protected Grammar grammar = null;

    /** List of all bitsets that must be dumped.  These are Vectors of BitSet. */
    protected Vector bitsetsUsed;

    /** The grammar behavior */
    protected DefineGrammarSymbols behavior;

    /** The LLk analyzer */
    protected LLkGrammarAnalyzer analyzer;

    /** Object used to format characters in the target language.
     * subclass must initialize this to the language-specific formatter
     */
    protected CharFormatter charFormatter;

    /** Use option "codeGenDebug" to generate debugging output */
    protected boolean DEBUG_CODE_GENERATOR = false;

    /** Default values for code-generation thresholds */
    protected static final int DEFAULT_MAKE_SWITCH_THRESHOLD = 2;
    protected static final int DEFAULT_BITSET_TEST_THRESHOLD = 4;

    /** If there are more than 8 long words to init in a bitset,
     *  try to optimize it; e.g., detect runs of -1L and 0L.
     */
    protected static final int BITSET_OPTIMIZE_INIT_THRESHOLD = 8;

    /** This is a hint for the language-specific code generator.
     * A switch() or language-specific equivalent will be generated instead
     * of a series of if/else statements for blocks with number of alternates
     * greater than or equal to this number of non-predicated LL(1) alternates.
     * This is modified by the grammar option "codeGenMakeSwitchThreshold"
     */
    protected int makeSwitchThreshold = DEFAULT_MAKE_SWITCH_THRESHOLD;

    /** This is a hint for the language-specific code generator.
     * A bitset membership test will be generated instead of an
     * ORed series of LA(k) comparisions for lookahead sets with
     * degree greater than or equal to this value.
     * This is modified by the grammar option "codeGenBitsetTestThreshold"
     */
    protected int bitsetTestThreshold = DEFAULT_BITSET_TEST_THRESHOLD;

    private static boolean OLD_ACTION_TRANSLATOR = true;

    public static String TokenTypesFileSuffix = "TokenTypes";
    public static String TokenTypesFileExt = ".txt";

    /** Construct code generator base class */
    public CodeGenerator() {
    }

    /** Output a String to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output
     */
    protected void _print(String s) {
        if (s != null) {
            currentOutput.print(s);
        }
    }

    /** Print an action without leading tabs, attempting to
     * preserve the current indentation level for multi-line actions
     * Ignored if string is null.
     * @param s The action string to output
     */
    protected void _printAction(String s) {
        if (s == null) {
            return;
        }

        // Skip leading newlines, tabs and spaces
        int start = 0;
        while (start < s.length() && Character.isSpaceChar(s.charAt(start))) {
            start++;
        }

        // Skip leading newlines, tabs and spaces
        int end = s.length() - 1;
        while (end > start && Character.isSpaceChar(s.charAt(end))) {
            end--;
        }

        char c = 0;
        for (int i = start; i <= end;) {
            c = s.charAt(i);
            i++;
            boolean newline = false;
            switch (c) {
                case '\n':
                    newline = true;
                    break;
                case '\r':
                    if (i <= end && s.charAt(i) == '\n') {
                        i++;
                    }
                    newline = true;
                    break;
                default:
                    currentOutput.print(c);
                    break;
            }
            if (newline) {
                currentOutput.println();
                printTabs();
                // Absorb leading whitespace
                while (i <= end && Character.isSpaceChar(s.charAt(i))) {
                    i++;
                }
                newline = false;
            }
        }
        currentOutput.println();
    }

    /** Output a String followed by newline, to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output
     */
    protected void _println(String s) {
        if (s != null) {
            currentOutput.println(s);
        }
    }

    /** Test if a set element array represents a contiguous range.
     * @param elems The array of elements representing the set, usually from BitSet.toArray().
     * @return true if the elements are a contiguous range (with two or more).
     */
    public static boolean elementsAreRange(int[] elems) {
        if (elems.length == 0) {
            return false;
        }
        int begin = elems[0];
        int end = elems[elems.length - 1];
        if (elems.length <= 2) {
            // Not enough elements for a range expression
            return false;
        }
        if (end - begin + 1 > elems.length) {
            // The set does not represent a contiguous range
            return false;
        }
        int v = begin + 1;
        for (int i = 1; i < elems.length - 1; i++) {
            if (v != elems[i]) {
                // The set does not represent a contiguous range
                return false;
            }
            v++;
        }
        return true;
    }

    /** Get the identifier portion of an argument-action token.
     * The ID of an action is assumed to be a trailing identifier.
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param t The action token
     * @return A string containing the text of the identifier
     */
    protected String extractIdOfAction(Token t) {
        return extractIdOfAction(t.getText(), t.getLine(), t.getColumn());
    }

    /** Get the identifier portion of an argument-action.
     * The ID of an action is assumed to be a trailing identifier.
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param s The action text
     * @param line Line used for error reporting.
     * @param column Line used for error reporting.
     * @return A string containing the text of the identifier
     */
    protected String extractIdOfAction(String s, int line, int column) {
        s = removeAssignmentFromDeclaration(s);
        // Search back from the end for a non alphanumeric.  That marks the
        // beginning of the identifier
        for (int i = s.length() - 2; i >= 0; i--) {
            // TODO: make this work for language-independent identifiers?
            if (!Character.isLetterOrDigit(s.charAt(i)) && s.charAt(i) != '_') {
                // Found end of type part
                return s.substring(i + 1);
            }
        }
        // Something is bogus, but we cannot parse the language-specific
        // actions any better.  The compiler will have to catch the problem.
        antlrTool.warning("Ill-formed action", grammar.getFilename(), line, column);
        return "";
    }

    /** Get the type string out of an argument-action token.
     * The type of an action is assumed to precede a trailing identifier
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param t The action token
     * @return A string containing the text of the type
     */
    protected String extractTypeOfAction(Token t) {
        return extractTypeOfAction(t.getText(), t.getLine(), t.getColumn());
    }

    /** Get the type portion of an argument-action.
     * The type of an action is assumed to precede a trailing identifier
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param s The action text
     * @param line Line used for error reporting.
     * @return A string containing the text of the type
     */
    protected String extractTypeOfAction(String s, int line, int column) {
        s = removeAssignmentFromDeclaration(s);
        // Search back from the end for a non alphanumeric.  That marks the
        // beginning of the identifier
        for (int i = s.length() - 2; i >= 0; i--) {
            // TODO: make this work for language-independent identifiers?
            if (!Character.isLetterOrDigit(s.charAt(i)) && s.charAt(i) != '_') {
                // Found end of type part
                return s.substring(0, i + 1);
            }
        }
        // Something is bogus, but we cannot parse the language-specific
        // actions any better.  The compiler will have to catch the problem.
        antlrTool.warning("Ill-formed action", grammar.getFilename(), line, column);
        return "";
    }

    /** Generate the code for all grammars
     */
    public abstract void gen();

    /** Generate code for the given grammar element.
     * @param action The {...} action to generate
     */
    public abstract void gen(ActionElement action);

    /** Generate code for the given grammar element.
     * @param blk The "x|y|z|..." block to generate
     */
    public abstract void gen(AlternativeBlock blk);

    /** Generate code for the given grammar element.
     * @param end The block-end element to generate.  Block-end
     * elements are synthesized by the grammar parser to represent
     * the end of a block.
     */
    public abstract void gen(BlockEndElement end);

    /** Generate code for the given grammar element.
     * @param atom The character literal reference to generate
     */
    public abstract void gen(CharLiteralElement atom);

    /** Generate code for the given grammar element.
     * @param r The character-range reference to generate
     */
    public abstract void gen(CharRangeElement r);

    /** Generate the code for a parser */
    public abstract void gen(LexerGrammar g) throws IOException;

    /** Generate code for the given grammar element.
     * @param blk The (...)+ block to generate
     */
    public abstract void gen(OneOrMoreBlock blk);

    /** Generate the code for a parser */
    public abstract void gen(ParserGrammar g) throws IOException;

    /** Generate code for the given grammar element.
     * @param rr The rule-reference to generate
     */
    public abstract void gen(RuleRefElement rr);

    /** Generate code for the given grammar element.
     * @param atom The string-literal reference to generate
     */
    public abstract void gen(StringLiteralElement atom);

    /** Generate code for the given grammar element.
     * @param r The token-range reference to generate
     */
    public abstract void gen(TokenRangeElement r);

    /** Generate code for the given grammar element.
     * @param atom The token-reference to generate
     */
    public abstract void gen(TokenRefElement atom);

    /** Generate code for the given grammar element.
     * @param blk The tree to generate code for.
     */
    public abstract void gen(TreeElement t);

    /** Generate the code for a parser */
    public abstract void gen(TreeWalkerGrammar g) throws IOException;

    /** Generate code for the given grammar element.
     * @param wc The wildcard element to generate
     */
    public abstract void gen(WildcardElement wc);

    /** Generate code for the given grammar element.
     * @param blk The (...)* block to generate
     */
    public abstract void gen(ZeroOrMoreBlock blk);

    /** Generate the token types as a text file for persistence across shared lexer/parser */
    protected void genTokenInterchange(TokenManager tm) throws IOException {
        // Open the token output Java file and set the currentOutput stream
        String fName = tm.getName() + TokenTypesFileSuffix + TokenTypesFileExt;
        currentOutput = antlrTool.openOutputFile(fName);

        println("// $ANTLR " + antlrTool.version + ": " +
                antlrTool.fileMinusPath(antlrTool.grammarFile) +
                " -> " +
                fName +
                "$");

        tabs = 0;

        // Header
        println(tm.getName() + "    // output token vocab name");

        // Generate a definition for each token type
        Vector v = tm.getVocabulary();
        for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
            String s = (String)v.elementAt(i);
            if (DEBUG_CODE_GENERATOR) {
                System.out.println("gen persistence file entry for: " + s);
            }
            if (s != null && !s.startsWith("<")) {
                // if literal, find label
                if (s.startsWith("\"")) {
                    StringLiteralSymbol sl = (StringLiteralSymbol)tm.getTokenSymbol(s);
                    if (sl != null && sl.label != null) {
                        print(sl.label + "=");
                    }
                    println(s + "=" + i);
                }
                else {
                    print(s);
                    // check for a paraphrase
                    TokenSymbol ts = (TokenSymbol)tm.getTokenSymbol(s);
                    if (ts == null) {
                        antlrTool.warning("undefined token symbol: " + s);
                    }
                    else {
                        if (ts.getParaphrase() != null) {
                            print("(" + ts.getParaphrase() + ")");
                        }
                    }
                    println("=" + i);
                }
            }
        }

        // Close the tokens output file
        currentOutput.close();
        currentOutput = null;
    }

    /** Process a string for an simple expression for use in xx/action.g
     * it is used to cast simple tokens/references to the right type for
     * the generated language.
     * @param str A String.
     */
    public String processStringForASTConstructor(String str) {
        return str;
    }

    /** Get a string for an expression to generate creation of an AST subtree.
     * @param v A Vector of String, where each element is an expression in the target language yielding an AST node.
     */
    public abstract String getASTCreateString(Vector v);

    /** Get a string for an expression to generate creating of an AST node
     * @param str The text of the arguments to the AST construction
     */
    public abstract String getASTCreateString(GrammarAtom atom, String str);

    /** Given the index of a bitset in the bitset list, generate a unique name.
     * Specific code-generators may want to override this
     * if the language does not allow '_' or numerals in identifiers.
     * @param index  The index of the bitset in the bitset list.
     */
    protected String getBitsetName(int index) {
        return "_tokenSet_" + index;
    }

    public static String encodeLexerRuleName(String id) {
        return "m" + id;
    }

    public static String decodeLexerRuleName(String id) {
        if ( id==null ) {
            return null;
        }
        return id.substring(1,id.length());
    }

    /** Map an identifier to it's corresponding tree-node variable.
     * This is context-sensitive, depending on the rule and alternative
     * being generated
     * @param id The identifier name to map
     * @param forInput true if the input tree node variable is to be returned, otherwise the output variable is returned.
     * @return The mapped id (which may be the same as the input), or null if the mapping is invalid due to duplicates
     */
    public abstract String mapTreeId(String id, ActionTransInfo tInfo);

    /** Add a bitset to the list of bitsets to be generated.
     * if the bitset is already in the list, ignore the request.
     * Always adds the bitset to the end of the list, so the
     * caller can rely on the position of bitsets in the list.
     * The returned position can be used to format the bitset
     * name, since it is invariant.
     * @param p Bit set to mark for code generation
     * @param forParser true if the bitset is used for the parser, false for the lexer
     * @return The position of the bitset in the list.
     */
    protected int markBitsetForGen(BitSet p) {
        // Is the bitset (or an identical one) already marked for gen?
        for (int i = 0; i < bitsetsUsed.size(); i++) {
            BitSet set = (BitSet)bitsetsUsed.elementAt(i);
            if (p.equals(set)) {
                // Use the identical one already stored
                return i;
            }
        }

        // Add the new bitset
        bitsetsUsed.appendElement(p.clone());
        return bitsetsUsed.size() - 1;
    }

    /** Output tab indent followed by a String, to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output.
     */
    protected void print(String s) {
        if (s != null) {
            printTabs();
            currentOutput.print(s);
        }
    }

    /** Print an action with leading tabs, attempting to
     * preserve the current indentation level for multi-line actions
     * Ignored if string is null.
     * @param s The action string to output
     */
    protected void printAction(String s) {
        if (s != null) {
            printTabs();
            _printAction(s);
        }
    }

    /** Output tab indent followed by a String followed by newline,
     * to the currentOutput stream.  Ignored if string is null.
     * @param s The string to output
     */
    protected void println(String s) {
        if (s != null) {
            printTabs();
            currentOutput.println(s);
        }
    }

    /** Output the current tab indentation.  This outputs the number of tabs
     * indicated by the "tabs" variable to the currentOutput stream.
     */
    protected void printTabs() {
        for (int i = 1; i <= tabs; i++) {
            currentOutput.print("\t");
        }
    }

    /** Lexically process $ and # references within the action.
     *  This will replace #id and #(...) with the appropriate
     *  function calls and/or variables etc...
     */
    protected abstract String processActionForSpecialSymbols(String actionStr,
															 int line,
															 RuleBlock currentRule,
															 ActionTransInfo tInfo);

	public String getFOLLOWBitSet(String ruleName, int k) {
		GrammarSymbol rs = grammar.getSymbol(ruleName);
		if ( !(rs instanceof RuleSymbol) ) {
			return null;
		}
		RuleBlock blk = ((RuleSymbol)rs).getBlock();
        Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(k, blk.endNode);
		String followSetName = getBitsetName(markBitsetForGen(follow.fset));
		return followSetName;
    }

	public String getFIRSTBitSet(String ruleName, int k) {
		GrammarSymbol rs = grammar.getSymbol(ruleName);
		if ( !(rs instanceof RuleSymbol) ) {
			return null;
		}
		RuleBlock blk = ((RuleSymbol)rs).getBlock();
        Lookahead first = grammar.theLLkAnalyzer.look(k, blk);
		String firstSetName = getBitsetName(markBitsetForGen(first.fset));
		return firstSetName;
    }

    /**
     * Remove the assignment portion of a declaration, if any.
     * @param d the declaration
     * @return the declaration without any assignment portion
     */
    protected String removeAssignmentFromDeclaration(String d) {
        // If d contains an equal sign, then it's a declaration
        // with an initialization.  Strip off the initialization part.
        if (d.indexOf('=') >= 0) d = d.substring(0, d.indexOf('=')).trim();
        return d;
    }

    /** Set all fields back like one just created */
    private void reset() {
        tabs = 0;
        // Allocate list of bitsets tagged for code generation
        bitsetsUsed = new Vector();
        currentOutput = null;
        grammar = null;
        DEBUG_CODE_GENERATOR = false;
        makeSwitchThreshold = DEFAULT_MAKE_SWITCH_THRESHOLD;
        bitsetTestThreshold = DEFAULT_BITSET_TEST_THRESHOLD;
    }

    public static String reverseLexerRuleName(String id) {
        return id.substring(1, id.length());
    }

    public void setAnalyzer(LLkGrammarAnalyzer analyzer_) {
        analyzer = analyzer_;
    }

    public void setBehavior(DefineGrammarSymbols behavior_) {
        behavior = behavior_;
    }

    /** Set a grammar for the code generator to use */
    protected void setGrammar(Grammar g) {
        reset();
        grammar = g;
        // Lookup make-switch threshold in the grammar generic options
        if (grammar.hasOption("codeGenMakeSwitchThreshold")) {
            try {
                makeSwitchThreshold = grammar.getIntegerOption("codeGenMakeSwitchThreshold");
                //System.out.println("setting codeGenMakeSwitchThreshold to " + makeSwitchThreshold);
            }
            catch (NumberFormatException e) {
                Token tok = grammar.getOption("codeGenMakeSwitchThreshold");
                antlrTool.error(
                    "option 'codeGenMakeSwitchThreshold' must be an integer",
                    grammar.getClassName(),
                    tok.getLine(), tok.getColumn()
                );
            }
        }

        // Lookup bitset-test threshold in the grammar generic options
        if (grammar.hasOption("codeGenBitsetTestThreshold")) {
            try {
                bitsetTestThreshold = grammar.getIntegerOption("codeGenBitsetTestThreshold");
                //System.out.println("setting codeGenBitsetTestThreshold to " + bitsetTestThreshold);
            }
            catch (NumberFormatException e) {
                Token tok = grammar.getOption("codeGenBitsetTestThreshold");
                antlrTool.error(
                    "option 'codeGenBitsetTestThreshold' must be an integer",
                    grammar.getClassName(),
                    tok.getLine(), tok.getColumn()
                );
            }
        }

        // Lookup debug code-gen in the grammar generic options
        if (grammar.hasOption("codeGenDebug")) {
            Token t = grammar.getOption("codeGenDebug");
            if (t.getText().equals("true")) {
                //System.out.println("setting code-generation debug ON");
                DEBUG_CODE_GENERATOR = true;
            }
            else if (t.getText().equals("false")) {
                //System.out.println("setting code-generation debug OFF");
                DEBUG_CODE_GENERATOR = false;
            }
            else {
                antlrTool.error("option 'codeGenDebug' must be true or false", grammar.getClassName(), t.getLine(), t.getColumn());
            }
        }
    }

    public void setTool(Tool tool) {
        antlrTool = tool;
    }
}

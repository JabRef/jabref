package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

// C++ code generator by Pete Wells: pete@yamuna.demon.co.uk
// #line generation contributed by: Ric Klaren <klaren@cs.utwente.nl>

import java.util.Enumeration;
import java.util.Hashtable;
import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
import java.io.PrintWriter; //SAS: changed for proper text file io
import java.io.IOException;
import java.io.FileWriter;

/**Generate MyParser.cpp, MyParser.hpp, MyLexer.cpp, MyLexer.hpp and MyParserTokenTypes.hpp */
public class CppCodeGenerator extends CodeGenerator {
	// non-zero if inside syntactic predicate generation
	protected int syntacticPredLevel = 0;

	// Are we generating ASTs (for parsers and tree parsers) right now?
	protected boolean genAST = false;

	// Are we saving the text consumed (for lexers) right now?
	protected boolean saveText = false;

	// Generate #line's
	protected boolean genHashLines = true;
	// Used to keep track of lineno in output
	protected int outputLine;
	protected String outputFile;

	// Grammar parameters set up to handle different grammar classes.
	// These are used to get instanceof tests out of code generation
	boolean usingCustomAST = false;
	String labeledElementType;
	String labeledElementASTType; // mostly the same as labeledElementType except in parsers
	String labeledElementASTInit;
	String labeledElementInit;
	String commonExtraArgs;
	String commonExtraParams;
	String commonLocalVars;
	String lt1Value;
	String exceptionThrown;
	String throwNoViable;

	// Tracks the rule being generated.  Used for mapTreeId
	RuleBlock currentRule;
	// Tracks the rule or labeled subrule being generated.  Used for AST generation.
	String currentASTResult;
	// Mapping between the ids used in the current alt, and the
	// names of variables used to represent their AST values.
	Hashtable treeVariableMap = new Hashtable();
	// Count of unnamed generated variables
	int astVarNumber = 1;
	// Special value used to mark duplicate in treeVariableMap
	protected static final String NONUNIQUE = new String();

	public static final int caseSizeThreshold = 127; // ascii is max

	private Vector semPreds;

	private static String namespaceStd   = "ANTLR_USE_NAMESPACE(std)";
	private static String namespaceAntlr = "ANTLR_USE_NAMESPACE(antlr)";
	private static NameSpace nameSpace = null;

	private static final String preIncludeCpp  = "pre_include_cpp";
	private static final String preIncludeHpp  = "pre_include_hpp";
	private static final String postIncludeCpp = "post_include_cpp";
	private static final String postIncludeHpp = "post_include_hpp";

	/** Create a C++ code-generator using the given Grammar.
	 * The caller must still call setTool, setBehavior, and setAnalyzer
	 * before generating code.
	 */
	public CppCodeGenerator() {
		super();
		charFormatter = new CppCharFormatter();
	}
	/** Adds a semantic predicate string to the sem pred vector
	    These strings will be used to build an array of sem pred names
	    when building a debugging parser.  This method should only be
	    called when the debug option is specified
	 */
	protected int addSemPred(String predicate) {
		semPreds.appendElement(predicate);
		return semPreds.size()-1;
	}
	public void exitIfError() {
		if (tool.hasError) {
			System.out.println("Exiting due to errors.");
			System.exit(1);
		}
	}
	protected int countLines( String s )
	{
		int lines = 0;
		for( int i = 0; i < s.length(); i++ )
		{
			if( s.charAt(i) == '\n' )
				lines++;
		}
		return lines;
	}
	/** Output a String to the currentOutput stream.
	 * Ignored if string is null.
	 * @param s The string to output
	 */
	protected void _print(String s) {
		if (s != null) {
			outputLine += countLines(s);
			currentOutput.print(s);
		}
	}
	/** Print an action without leading tabs, attempting to
	 * preserve the current indentation level for multi-line actions
	 * Ignored if string is null.
	 * @param s The action string to output
	 */
	protected void _printAction(String s) {
		if (s != null) {
			outputLine += countLines(s)+1;
			super._printAction(s);
		}
	}
	/** Print an action stored in a token surrounded by #line stuff */
	public void printAction(Token t) {
		if (t != null) {
			genLineNo(t.getLine());
			printTabs();
			_printAction(t.getText());
			genLineNo2();
		}
	}
	/** Print a header action by #line stuff
	* @param name The name of the header part
	*/
	public void printHeaderAction(String name) {
		Token a = (antlr.Token)behavior.headerActions.get(name);
		if (a != null) {
			genLineNo(a.getLine());
			println(a.getText());
			genLineNo2();
		}
	}
	/** Output a String followed by newline, to the currentOutput stream.
	 * Ignored if string is null.
	 * @param s The string to output
	 */
	protected void _println(String s) {
		if (s != null) {
			outputLine += countLines(s)+1;
			currentOutput.println(s);
		}
	}
	/** Output tab indent followed by a String followed by newline,
	 * to the currentOutput stream.  Ignored if string is null.
	 * @param s The string to output
	 */
	protected void println(String s) {
		if (s != null) {
			printTabs();
			outputLine += countLines(s)+1;
			currentOutput.println(s);
		}
	}

	/** Generate a #line or // line depending on options */
	public void genLineNo(int line) {
	    if ( line == 0 ) {
		line++;
	    }
	    if( genHashLines )
		_println("#line "+line+" \""+Tool.fileMinusPath(tool.grammarFile)+"\"");
	    else
		println("// line "+line+" \""+Tool.fileMinusPath(tool.grammarFile)+"\"");
	}

	/** Generate a #line or // line depending on options */
	public void genLineNo(GrammarElement el)
	{
		if( el != null )
			genLineNo(el.getLine());
	}
	/** Generate a #line or // line depending on options */
	public void genLineNo(Token t)
	{
		if (t != null)
			genLineNo(t.getLine());
	}
	/** Generate a #line or // line depending on options */
	public void genLineNo2()
	{
		if( genHashLines )
		{
			_println("#line "+(outputLine+1)+" \""+outputFile+"\"");
		}
	}
	/**Generate the parser, lexer, treeparser, and token types in C++ */
	public void gen() {
		// Do the code generation
		try {
			// Loop over all grammars
			Enumeration grammarIter = behavior.grammars.elements();
			while (grammarIter.hasMoreElements()) {
				Grammar g = (Grammar)grammarIter.nextElement();
				// Connect all the components to each other
				g.setGrammarAnalyzer(analyzer);
				g.setCodeGenerator(this);
				analyzer.setGrammar(g);
				// To get right overloading behavior across hetrogeneous grammars
				setupGrammarParameters(g);
				g.generate();
				exitIfError();
			}

			// Loop over all token managers (some of which are lexers)
			Enumeration tmIter = behavior.tokenManagers.elements();
			while (tmIter.hasMoreElements()) {
				TokenManager tm = (TokenManager)tmIter.nextElement();
				if (!tm.isReadOnly()) {
					// Write the token manager tokens as C++
					// this must appear before genTokenInterchange so that
					// labels are set on string literals
					genTokenTypes(tm);
					// Write the token manager tokens as plain text
					genTokenInterchange(tm);
				}
				exitIfError();
			}
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The {...} action to generate
	 */
	public void gen(ActionElement action) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genAction("+action+")");
		if ( action.isSemPred ) {
			genSemPred(action.actionText, action.line);
		}
		else {
			if ( grammar.hasSyntacticPredicate ) {
				println("if ( inputState->guessing==0 ) {");
				tabs++;
			}

			ActionTransInfo tInfo = new ActionTransInfo();
			String actionStr = processActionForTreeSpecifiers(action.actionText, action.getLine(), currentRule, tInfo);

			if ( tInfo.refRuleRoot!=null ) {
				// Somebody referenced "#rule", make sure translated var is valid
				// assignment to #rule is left as a ref also, meaning that assignments
				// with no other refs like "#rule = foo();" still forces this code to be
				// generated (unnecessarily).
				println(tInfo.refRuleRoot + " = "+labeledElementASTType+"(currentAST.root);");
			}

			// dump the translated action
			genLineNo(action);
			printAction(actionStr);
			genLineNo2();

			if ( tInfo.assignToRoot ) {
				// Somebody did a "#rule=", reset internal currentAST.root
				println("currentAST.root = "+tInfo.refRuleRoot+";");
				// reset the child pointer too to be last sibling in sibling list
				// now use if else in stead of x ? y : z to shut CC 4.2 up.
				println("if ( "+tInfo.refRuleRoot+"!="+labeledElementASTInit+" &&");
				tabs++;
				println(tInfo.refRuleRoot+"->getFirstChild() != "+labeledElementASTInit+" )");
				println("  currentAST.child = "+tInfo.refRuleRoot+"->getFirstChild();");
			  	tabs--;
				println("else");
				tabs++;
				println("currentAST.child = "+tInfo.refRuleRoot+";");
				tabs--;
				println("currentAST.advanceChildToEnd();");
			}

			if ( grammar.hasSyntacticPredicate ) {
				tabs--;
				println("}");
			}
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The "x|y|z|..." block to generate
	 */
	public void gen(AlternativeBlock blk) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen("+blk+")");
		println("{");
		genBlockPreamble(blk);

		// Tell AST generation to build subrule result
		String saveCurrentASTResult = currentASTResult;
		if (blk.getLabel() != null) {
			currentASTResult = blk.getLabel();
		}

		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

		CppBlockFinishingInfo howToFinish = genCommonBlock(blk, true);
		genBlockFinish(howToFinish, throwNoViable);

		println("}");

		// Restore previous AST generation
		currentASTResult = saveCurrentASTResult;
	}
	/** Generate code for the given grammar element.
	 * @param blk The block-end element to generate.  Block-end
	 * elements are synthesized by the grammar parser to represent
	 * the end of a block.
	 */
	public void gen(BlockEndElement end) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRuleEnd("+end+")");
	}
	/** Generate code for the given grammar element.
	 * @param blk The character literal reference to generate
	 */
	public void gen(CharLiteralElement atom) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genChar("+atom+")");

		if ( atom.getLabel()!=null ) {
			println(atom.getLabel() + " = " + lt1Value + ";");
		}

		boolean oldsaveText = saveText;
		saveText = saveText && atom.getAutoGenType()==GrammarElement.AUTO_GEN_NONE;
		genMatch(atom);
		saveText = oldsaveText;
	}
	/** Generate code for the given grammar element.
	 * @param blk The character-range reference to generate
	 */
	public void gen(CharRangeElement r) {
		if ( r.getLabel()!=null  && syntacticPredLevel == 0) {
			println(r.getLabel() + " = " + lt1Value + ";");
		}
		println("matchRange("+textOrChar(r.beginText)+","+textOrChar(r.endText)+");");
	}
	/** Generate the lexer C++ files */
	public  void gen(LexerGrammar g) throws IOException {
		// If debugging, create a new sempred vector for this grammar
		if (g.debuggingOutput)
			semPreds = new Vector();

		setGrammar(g);
		if (!(grammar instanceof LexerGrammar)) {
			tool.panic("Internal error generating lexer");
		}

		genBody(g);
		genInclude(g);
	}
	/** Generate code for the given grammar element.
	 * @param blk The (...)+ block to generate
	 */
	public void gen(OneOrMoreBlock blk) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen+("+blk+")");
		String label;
		String cnt;
		println("{");
		genBlockPreamble(blk);
		if ( blk.getLabel() != null ) {
			cnt = "_cnt_"+blk.getLabel();
		}
		else {
			cnt = "_cnt" + blk.ID;
		}
		println("int "+cnt+"=0;");
		if ( blk.getLabel() != null ) {
			label = blk.getLabel();
		}
		else {
			label = "_loop" + blk.ID;
		}

		println("for (;;) {");
		tabs++;

		// Tell AST generation to build subrule result
		String saveCurrentASTResult = currentASTResult;
		if (blk.getLabel() != null) {
			currentASTResult = blk.getLabel();
		}

		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

		// generate exit test if greedy set to false
		// and an alt is ambiguous with exit branch
		// or when lookahead derived purely from end-of-file
		// Lookahead analysis stops when end-of-file is hit,
		// returning set {epsilon}.  Since {epsilon} is not
		// ambig with any real tokens, no error is reported
		// by deterministic() routines and we have to check
		// for the case where the lookahead depth didn't get
		// set to NONDETERMINISTIC (this only happens when the
		// FOLLOW contains real atoms + epsilon).
		boolean generateNonGreedyExitPath = false;
		int nonGreedyExitDepth = grammar.maxk;

		if ( !blk.greedy &&
			 blk.exitLookaheadDepth<=grammar.maxk &&
			 blk.exitCache[blk.exitLookaheadDepth].containsEpsilon() )
		{
			generateNonGreedyExitPath = true;
			nonGreedyExitDepth = blk.exitLookaheadDepth;
		}
		else if ( !blk.greedy &&
				  blk.exitLookaheadDepth==LLkGrammarAnalyzer.NONDETERMINISTIC )
		{
			generateNonGreedyExitPath = true;
		}

		// generate exit test if greedy set to false
		// and an alt is ambiguous with exit branch
		if ( generateNonGreedyExitPath ) {
			if ( DEBUG_CODE_GENERATOR ) {
				System.out.println("nongreedy (...)+ loop; exit depth is "+
								   blk.exitLookaheadDepth);
			}
			String predictExit =
				getLookaheadTestExpression(blk.exitCache,
										   nonGreedyExitDepth);
			println("// nongreedy exit test");
			println("if ( "+cnt+">=1 && "+predictExit+") goto "+label+";");
		}

		CppBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
		genBlockFinish(
			howToFinish,
			"if ( "+cnt+">=1 ) { goto "+label+"; } else {" + throwNoViable + "}"
		);

		println(cnt+"++;");
		tabs--;
		println("}");
		println(label+":;");
		println("}");

		// Restore previous AST generation
		currentASTResult = saveCurrentASTResult;
	}
	/** Generate the parser C++ file */
	public void gen(ParserGrammar g) throws IOException {

		// if debugging, set up a new vector to keep track of sempred
		//   strings for this grammar
		if (g.debuggingOutput)
			semPreds = new Vector();

		setGrammar(g);
		if (!(grammar instanceof ParserGrammar)) {
			tool.panic("Internal error generating parser");
		}

		genBody(g);
		genInclude(g);
	}
	/** Generate code for the given grammar element.
	 * @param blk The rule-reference to generate
	 */
	public void gen(RuleRefElement rr) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRR("+rr+")");
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
		if (rs == null || !rs.isDefined())
		{
			// Is this redundant???
			tool.error("Rule '" + rr.targetRule + "' is not defined", grammar.getFilename(), rr.getLine());
			return;
		}
		if (!(rs instanceof RuleSymbol))
		{
			// Is this redundant???
			tool.error("'" + rr.targetRule + "' does not name a grammar rule", grammar.getFilename(), rr.getLine());
			return;
		}

		genErrorTryForElement(rr);

		// AST value for labeled rule refs in tree walker.
		// This is not AST construction;  it is just the input tree node value.
		if ( grammar instanceof TreeWalkerGrammar &&
			rr.getLabel() != null &&
			syntacticPredLevel == 0 )
		{
			println(rr.getLabel() + " = (_t == ASTNULL) ? "+labeledElementASTInit+" : "+lt1Value+";");
		}

		// if in lexer and ! on rule ref or alt or rule, save buffer index to kill later
		if ( grammar instanceof LexerGrammar && (!saveText||rr.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			println("_saveIndex = text.length();");
		}

		// Process return value assignment if any
		printTabs();
		if (rr.idAssign != null)
		{
			// Warn if the rule has no return type
			if (rs.block.returnAction == null)
			{
				tool.warning("Rule '" + rr.targetRule + "' has no return type", grammar.getFilename(), rr.getLine());
			}
			_print(rr.idAssign + "=");
		} else {
			// Warn about return value if any, but not inside syntactic predicate
			if ( !(grammar instanceof LexerGrammar) && syntacticPredLevel == 0 && rs.block.returnAction != null)
			{
				tool.warning("Rule '" + rr.targetRule + "' returns a value", grammar.getFilename(), rr.getLine());
			}
		}

		// Call the rule
		GenRuleInvocation(rr);

		// if in lexer and ! on element or alt or rule, save buffer index to kill later
		if ( grammar instanceof LexerGrammar && (!saveText||rr.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			println("text.erase(_saveIndex);");
		}

		// if not in a syntactic predicate
		if (syntacticPredLevel == 0) {
			boolean doNoGuessTest = (
				grammar.hasSyntacticPredicate &&
				(
					grammar.buildAST && rr.getLabel() != null ||
					(genAST && rr.getAutoGenType() == GrammarElement.AUTO_GEN_NONE)
				)
			);
			if (doNoGuessTest) {
				println("if (inputState->guessing==0) {");
				tabs++;
			}

			if (grammar.buildAST && rr.getLabel() != null) {
				// always gen variable for rule return on labeled rules
				// FIXME: maybe broken....
				println(rr.getLabel() + "_AST = "+labeledElementASTType+"(returnAST);");
			}
			if (genAST) {
				switch (rr.getAutoGenType()) {
				case GrammarElement.AUTO_GEN_NONE:
					// println("theASTFactory.addASTChild(currentAST, returnAST);");
					println("astFactory.addASTChild(currentAST, "+namespaceAntlr+"RefAST(returnAST));");
					break;
				case GrammarElement.AUTO_GEN_CARET:
					tool.error("Internal: encountered ^ after rule reference");
					break;
				default:
					break;
				}
			}

			// if a lexer and labeled, Token label defined at rule level, just set it here
			if ( grammar instanceof LexerGrammar && rr.getLabel() != null ) {
				println(rr.getLabel()+"=_returnToken;");
			}

			if (doNoGuessTest) {
				tabs--;
				println("}");
			}
		}
		genErrorCatchForElement(rr);
	}
	/** Generate code for the given grammar element.
	 * @param blk The string-literal reference to generate
	 */
	public void gen(StringLiteralElement atom) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genString("+atom+")");

		// Variable declarations for labeled elements
		if (atom.getLabel()!=null && syntacticPredLevel == 0) {
			println(atom.getLabel() + " = " + lt1Value + ";");
		}

		// AST
		genElementAST(atom);

		// is there a bang on the literal?
		boolean oldsaveText = saveText;
		saveText = saveText && atom.getAutoGenType()==GrammarElement.AUTO_GEN_NONE;

		// matching
		genMatch(atom);

		saveText = oldsaveText;

		// tack on tree cursor motion if doing a tree walker
		if (grammar instanceof TreeWalkerGrammar) {
			println("_t = _t->getNextSibling();");
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The token-range reference to generate
	 */
	public void gen(TokenRangeElement r) {
		genErrorTryForElement(r);
		if ( r.getLabel()!=null  && syntacticPredLevel == 0) {
			println(r.getLabel() + " = " + lt1Value + ";");
		}

		// AST
		genElementAST(r);

		// match
		println("matchRange("+r.beginText+","+r.endText+");");
		genErrorCatchForElement(r);
	}
	/** Generate code for the given grammar element.
	 * @param blk The token-reference to generate
	 */
	public void gen(TokenRefElement atom) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genTokenRef("+atom+")");
		if ( grammar instanceof LexerGrammar ) {
			tool.panic("Token reference found in lexer");
		}
		genErrorTryForElement(atom);
		// Assign Token value to token label variable
		if ( atom.getLabel()!=null && syntacticPredLevel == 0) {
			println(atom.getLabel() + " = " + lt1Value + ";");
		}

		// AST
		genElementAST(atom);
		// matching
		genMatch(atom);
		genErrorCatchForElement(atom);

		// tack on tree cursor motion if doing a tree walker
		if (grammar instanceof TreeWalkerGrammar) {
			println("_t = _t->getNextSibling();");
		}
	}
	public void gen(TreeElement t) {
		// save AST cursor
		println(labeledElementType+" __t" + t.ID + " = _t;");

		// If there is a label on the root, then assign that to the variable
		if (t.root.getLabel() != null) {
//			println(t.root.getLabel() + " = _t==ASTNULL ? "+namespaceAntlr+"nullAST :"+labeledElementASTType+"(_t);");
			println(t.root.getLabel() + " = (_t == ASTNULL) ? "+labeledElementASTInit+" : _t;");
		}

		// Generate AST variables
		genElementAST(t.root);
		if (grammar.buildAST) {
			// Save the AST construction state
			println(namespaceAntlr+"ASTPair __currentAST" + t.ID + " = currentAST;");
			// Make the next item added a child of the TreeElement root
			println("currentAST.root = currentAST.child;");
			println("currentAST.child = "+labeledElementASTInit+";");
		}

		// match root
		genMatch(t.root);
		// move to list of children
		println("_t = _t->getFirstChild();");

		// walk list of children, generating code for each
		for (int i=0; i<t.getAlternatives().size(); i++) {
			Alternative a = t.getAlternativeAt(i);
			AlternativeElement e = a.head;
			while ( e != null ) {
				e.generate();
				e = e.next;
			}
		}

		if (grammar.buildAST) {
			// restore the AST construction state to that just after the
			// tree root was added
			println("currentAST = __currentAST" + t.ID + ";");
		}
		// restore AST cursor
		println("_t = __t" + t.ID + ";");
		// move cursor to sibling of tree just parsed
		println("_t = _t->getNextSibling();");
	}
	/** Generate the tree-parser C++ files */
	public void gen(TreeWalkerGrammar g) throws IOException {
		setGrammar(g);
		if (!(grammar instanceof TreeWalkerGrammar)) {
			tool.panic("Internal error generating tree-walker");
		}

		genBody(g);
		genInclude(g);
	}
	/** Generate code for the given grammar element.
	 * @param wc The wildcard element to generate
	 */
	public void gen(WildcardElement wc) {
		// Variable assignment for labeled elements
		if (wc.getLabel()!=null && syntacticPredLevel == 0) {
			println(wc.getLabel() + " = " + lt1Value + ";");
		}

		// AST
		genElementAST(wc);
		// Match anything but EOF
		if (grammar instanceof TreeWalkerGrammar) {
			println("if ( _t == "+labeledElementASTInit+" ) throw "+namespaceAntlr+"MismatchedTokenException();");
		}
		else if (grammar instanceof LexerGrammar) {
			if ( grammar instanceof LexerGrammar &&
					(!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
				println("_saveIndex = text.length();");
			}
			println("matchNot(EOF/*_CHAR*/);");
			if ( grammar instanceof LexerGrammar &&
					(!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
				println("text.erase(_saveIndex);");      // kill text atom put in buffer
			}
		}
		else {
			println("matchNot(" + getValueString(Token.EOF_TYPE) + ");");
		}

		// tack on tree cursor motion if doing a tree walker
		if (grammar instanceof TreeWalkerGrammar) {
			println("_t = _t->getNextSibling();");
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The (...)* block to generate
	 */
	public void gen(ZeroOrMoreBlock blk) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen*("+blk+")");
		println("{");
		genBlockPreamble(blk);
		String label;
		if ( blk.getLabel() != null ) {
			label = blk.getLabel();
		}
		else {
			label = "_loop" + blk.ID;
		}
		println("for (;;) {");
		tabs++;

		// Tell AST generation to build subrule result
		String saveCurrentASTResult = currentASTResult;
		if (blk.getLabel() != null) {
			currentASTResult = blk.getLabel();
		}

		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

		// generate exit test if greedy set to false
		// and an alt is ambiguous with exit branch
		// or when lookahead derived purely from end-of-file
		// Lookahead analysis stops when end-of-file is hit,
		// returning set {epsilon}.  Since {epsilon} is not
		// ambig with any real tokens, no error is reported
		// by deterministic() routines and we have to check
		// for the case where the lookahead depth didn't get
		// set to NONDETERMINISTIC (this only happens when the
		// FOLLOW contains real atoms + epsilon).
		boolean generateNonGreedyExitPath = false;
		int nonGreedyExitDepth = grammar.maxk;

		if ( !blk.greedy &&
			 blk.exitLookaheadDepth<=grammar.maxk &&
			 blk.exitCache[blk.exitLookaheadDepth].containsEpsilon() )
		{
			generateNonGreedyExitPath = true;
			nonGreedyExitDepth = blk.exitLookaheadDepth;
		}
		else if ( !blk.greedy &&
				  blk.exitLookaheadDepth==LLkGrammarAnalyzer.NONDETERMINISTIC )
		{
			generateNonGreedyExitPath = true;
		}
		if ( generateNonGreedyExitPath ) {
			if ( DEBUG_CODE_GENERATOR ) {
				System.out.println("nongreedy (...)* loop; exit depth is "+
								   blk.exitLookaheadDepth);
			}
			String predictExit =
				getLookaheadTestExpression(blk.exitCache,
										   nonGreedyExitDepth);
			println("// nongreedy exit test");
			println("if ("+predictExit+") goto "+label+";");
		}

		CppBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
		genBlockFinish(howToFinish, "goto " + label + ";");

		tabs--;
		println("}");
		println(label+":;");
		println("}");

		// Restore previous AST generation
		currentASTResult = saveCurrentASTResult;
	}
	/** Generate an alternative.
	  * @param alt  The alternative to generate
	  * @param blk The block to which the alternative belongs
	  */
	protected void genAlt(Alternative alt, AlternativeBlock blk) {
		// Save the AST generation state, and set it to that of the alt
		boolean savegenAST = genAST;
		genAST = genAST && alt.getAutoGen();

		boolean oldsaveTest = saveText;
		saveText = saveText && alt.getAutoGen();

		// Reset the variable name map for the alternative
		Hashtable saveMap = treeVariableMap;
		treeVariableMap = new Hashtable();

		// Generate try block around the alt for  error handling
		if (alt.exceptionSpec != null) {
			println("try {      // for error handling");
			tabs++;
		}

		AlternativeElement elem = alt.head;
		while ( !(elem instanceof BlockEndElement) ) {
			elem.generate(); // alt can begin with anything. Ask target to gen.
			elem = elem.next;
		}

		if ( genAST) {
			if (blk instanceof RuleBlock) {
				// Set the AST return value for the rule
				RuleBlock rblk = (RuleBlock)blk;
				println(rblk.getRuleName() + "_AST = "+labeledElementASTType+"(currentAST.root);");
			}
			else if (blk.getLabel() != null) {
				// ### future: also set AST value for labeled subrules.
				// println(blk.getLabel() + "_AST = "+labeledElementASTType+"(currentAST.root);");
			}
		}

		if (alt.exceptionSpec != null) {
			// close try block
			tabs--;
			println("}");
			genErrorHandler(alt.exceptionSpec);
		}

		genAST = savegenAST;
		saveText = oldsaveTest;

		treeVariableMap = saveMap;
	}
	/** Generate all the bitsets to be used in the parser or lexer
	 * Generate the raw bitset data like "long _tokenSet1_data[] = {...};"
	 * and the BitSet object declarations like "BitSet _tokenSet1 = new BitSet(_tokenSet1_data);"
	 * Note that most languages do not support object initialization inside a
	 * class definition, so other code-generators may have to separate the
	 * bitset declarations from the initializations (e.g., put the initializations
	 * in the generated constructor instead).
	 * @param bitsetList The list of bitsets to generate.
	 * @param maxVocabulary Ensure that each generated bitset can contain at least this value.
	 * @param dumpSets Dump out the token definitions of the contents of the bitset
	 * only for grammars/parsers.
	 */
	protected void genBitsets(
		Vector bitsetList,
		int maxVocabulary,
		String prefix,
		boolean dumpSets
	) {
		println("");
		for (int i = 0; i < bitsetList.size(); i++)
		{
			BitSet p = (BitSet)bitsetList.elementAt(i);
			// Ensure that generated BitSet is large enough for vocabulary
			p.growToInclude(maxVocabulary);
			// initialization data
			println(
				"const unsigned long " + prefix + getBitsetName(i) + "_data_" + "[] = { " +
				p.toStringOfHalfWords() +
				" };"
			);

			if( dumpSets )
			{
				// Dump the contents of the bitset in readable format...
				String t = "// ";
				for( int j = 0; j < p.size(); j++ )
				{
					if ( p.member( j ) )
					{
						t += grammar.tokenManager.getTokenStringAt(j)+" ";
						if( t.length() > 70 )
						{
							println(t);
							t = "// ";
						}
					}
				}
				if ( t != "// " )
					println(t);
			}

			// BitSet object
			println(
				"const "+namespaceAntlr+"BitSet " + prefix + getBitsetName(i) + "(" +
				getBitsetName(i) + "_data_," + p.size()/32 +
				");"
			);
		}
	}
	protected void genBitsetsHeader(
		Vector bitsetList,
		int maxVocabulary
	) {
		println("");
		for (int i = 0; i < bitsetList.size(); i++)
		{
			BitSet p = (BitSet)bitsetList.elementAt(i);
			// Ensure that generated BitSet is large enough for vocabulary
			p.growToInclude(maxVocabulary);
			// initialization data
			println("static const unsigned long " + getBitsetName(i) + "_data_" + "[];");
			// BitSet object
			println("static const "+namespaceAntlr+"BitSet " + getBitsetName(i) + ";");
		}
	}
	/** Generate the finish of a block, using a combination of the info
	 * returned from genCommonBlock() and the action to perform when
	 * no alts were taken
	 * @param howToFinish The return of genCommonBlock()
	 * @param noViableAction What to generate when no alt is taken
	 */
	private void genBlockFinish(CppBlockFinishingInfo howToFinish, String noViableAction)
	{
		if (howToFinish.needAnErrorClause &&
			 (howToFinish.generatedAnIf || howToFinish.generatedSwitch)) {
			if ( howToFinish.generatedAnIf ) {
				println("else {");
			}
			else {
				println("{");
			}
			tabs++;
			println(noViableAction);
			tabs--;
			println("}");
		}

		if ( howToFinish.postscript!=null ) {
			println(howToFinish.postscript);
		}
	}
	/** Generate the header for a block, which may be a RuleBlock or a
	 * plain AlternativeBLock.  This generates any variable declarations,
	 * init-actions, and syntactic-predicate-testing variables.
	 * @blk The block for which the preamble is to be generated.
	 */
	protected void genBlockPreamble(AlternativeBlock blk) {
		// define labels for rule blocks.
		if ( blk instanceof RuleBlock ) {
			RuleBlock rblk = (RuleBlock)blk;
			if ( rblk.labeledElements!=null ) {
				for (int i=0; i<rblk.labeledElements.size(); i++) {

					AlternativeElement a = (AlternativeElement)rblk.labeledElements.elementAt(i);
					//System.out.println("looking at labeled element: "+a);
					// Variables for labeled rule refs and subrules are different than
					// variables for grammar atoms.  This test is a little tricky because
					// we want to get all rule refs and ebnf, but not rule blocks or
					// syntactic predicates
					if (
						a instanceof RuleRefElement ||
						a instanceof AlternativeBlock &&
						!(a instanceof RuleBlock) &&
						!(a instanceof SynPredBlock)
					) {

						if (
							!(a instanceof RuleRefElement) &&
							((AlternativeBlock)a).not &&
							analyzer.subruleCanBeInverted(((AlternativeBlock)a), grammar instanceof LexerGrammar)
						) {
							// Special case for inverted subrules that will be inlined.
							// Treat these like token or char literal references
							println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
							if (grammar.buildAST) {
								println(labeledElementASTType+" " + a.getLabel() + "_AST = "+labeledElementASTInit+";");
							}
						}
						else {
							if (grammar.buildAST) {
								// Always gen AST variables for labeled elements, even if the
								// element itself is marked with !
								println(labeledElementASTType+" " + a.getLabel() + "_AST = "+labeledElementASTInit+";");
							}
							if ( grammar instanceof LexerGrammar ) {
								println(namespaceAntlr+"RefToken "+a.getLabel()+";");
							}
							if (grammar instanceof TreeWalkerGrammar) {
								// always generate rule-ref variables for tree walker
								println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
							}
						}
					}
					else {
						// It is a token or literal reference.  Generate the
						// correct variable type for this grammar
						println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
						// In addition, generate *_AST variables if building ASTs
						if (grammar.buildAST) {
							println(labeledElementASTType+" " + a.getLabel() + "_AST = "+labeledElementASTInit+";");
						}
					}
				}
			}
		}

		// dump out init action
		if ( blk.initAction!=null ) {
			genLineNo(blk);
			printAction(
				processActionForTreeSpecifiers(blk.initAction, 0, currentRule, null)
			);
			genLineNo2();
		}
	}
	public void genBody(LexerGrammar g) throws IOException {
		outputFile = grammar.getClassName() + ".cpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = false;	// no way to gen trees.
		saveText = true;	// save consumed characters.

		tabs=0;

		// Generate header common to all C++ output files
		genHeader(outputFile);

		printHeaderAction(preIncludeCpp);
		// Generate header specific to lexer C++ file
		println("#include \"" + grammar.getClassName() + ".hpp\"");
		println("#include \"antlr/CharBuffer.hpp\"");
		println("#include \"antlr/TokenStreamException.hpp\"");
		println("#include \"antlr/TokenStreamIOException.hpp\"");
		println("#include \"antlr/TokenStreamRecognitionException.hpp\"");
		println("#include \"antlr/CharStreamException.hpp\"");
		println("#include \"antlr/CharStreamIOException.hpp\"");
		println("#include \"antlr/NoViableAltForCharException.hpp\"");
		if (grammar.debuggingOutput)
			println("#include \"antlr/DebuggingInputBuffer.hpp\"");
		println("");
		printHeaderAction(postIncludeCpp);

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		// Generate user-defined lexer file preamble
		printAction(grammar.preambleAction);

		// Generate lexer class definition
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
		}
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			sup = namespaceAntlr + sup;
		}

		//
		// Generate the constructor from InputStream
		//
		println(grammar.getClassName() + "::" + grammar.getClassName() + "(" + namespaceStd + "istream& in)");
		tabs++;
		// if debugging, wrap the input buffer in a debugger
		if (grammar.debuggingOutput)
			println(": " + sup + "(new "+namespaceAntlr+"DebuggingInputBuffer(new "+namespaceAntlr+"CharBuffer(in)))");
		else
			println(": " + sup + "(new "+namespaceAntlr+"CharBuffer(in))");
		tabs--;
		println("{");
		tabs++;

		// if debugging, set up array variables and call user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging();");
		}

		println("setCaseSensitive("+g.caseSensitive+");");
		println("initLiterals();");
		tabs--;
		println("}");
		println("");

		// Generate the constructor from InputBuffer
		println(grammar.getClassName() + "::" + grammar.getClassName() + "("+namespaceAntlr+"InputBuffer& ib)");
		tabs++;
		// if debugging, wrap the input buffer in a debugger
		if (grammar.debuggingOutput)
			println(": " + sup + "(new "+namespaceAntlr+"DebuggingInputBuffer(ib))");
		else
			println(": " + sup + "(ib)");
		tabs--;
		println("{");
		tabs++;

		// if debugging, set up array variables and call user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging();");
		}

		println("setCaseSensitive("+g.caseSensitive+");");
		println("initLiterals();");
		tabs--;
		println("}");
		println("");

		// Generate the constructor from LexerSharedInputState
		println(grammar.getClassName() + "::" + grammar.getClassName() + "(const "+namespaceAntlr+"LexerSharedInputState& state)");
		tabs++;
		println(": " + sup + "(state)");
		tabs--;
		println("{");
		tabs++;

		// if debugging, set up array variables and call user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging();");
		}

		println("setCaseSensitive("+g.caseSensitive+");");
		println("initLiterals();");
		tabs--;
		println("}");
		println("");

		println("void " + grammar.getClassName() + "::initLiterals()");
		println("{");
		tabs++;
		// Generate the initialization of the map
		// containing the string literals used in the lexer
		// The literals variable itself is in CharScanner
		/* TJP: get keys now and check to make sure it's a literal not
		 * a label to a literal; was dup'ing string literals before
		 * change.
		 *
		Enumeration ids = grammar.tokenManager.getTokenSymbolElements();
		while ( ids.hasMoreElements() ) {
			TokenSymbol sym = (TokenSymbol)ids.nextElement();
			if ( sym instanceof StringLiteralSymbol ) {
				StringLiteralSymbol s = (StringLiteralSymbol)sym;
				println("literals["+s.getId()+"] = "+s.getTokenType()+";");
			}
		}
		*/
		// TJP changed it to following loop.
		Enumeration keys = grammar.tokenManager.getTokenSymbolKeys();
		while ( keys.hasMoreElements() ) {
			String key = (String)keys.nextElement();
			if ( key.charAt(0) != '"' ) {
				continue;
			}
			TokenSymbol sym = grammar.tokenManager.getTokenSymbol(key);
			if ( sym instanceof StringLiteralSymbol ) {
				StringLiteralSymbol s = (StringLiteralSymbol)sym;
				println("literals["+s.getId()+"] = "+s.getTokenType()+";");
			}
		}

		// Generate the setting of various generated options.
		tabs--;
		println("}");

		// Generate getCaseSensitiveLiterals() method
		println("bool " + grammar.getClassName() + "::getCaseSensitiveLiterals() const");
		println("{");
		tabs++;
		println("return "+g.caseSensitiveLiterals + ";");
		tabs--;
		println("}");

		Enumeration ids;
		// generate the rule name array for debugging
		if (grammar.debuggingOutput) {
			println("const char* "+grammar.getClassName()+"::_ruleNames[] = {");
			tabs++;

			ids = grammar.rules.elements();
			int ruleNum=0;
			while ( ids.hasMoreElements() ) {
				GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
				if ( sym instanceof RuleSymbol)
					println("\""+((RuleSymbol)sym).getId()+"\",");
			}
			println("0");
			tabs--;
			println("};");
		}

		// Generate nextToken() rule.
		// nextToken() is a synthetic lexer rule that is the implicit OR of all
		// user-defined lexer rules.
		genNextToken();

		// Generate code for each rule in the lexer
		ids = grammar.rules.elements();
		int ruleNum=0;
		while ( ids.hasMoreElements() ) {
			RuleSymbol sym = (RuleSymbol) ids.nextElement();
			// Don't generate the synthetic rules
			if (!sym.getId().equals("mnextToken")) {
				genRule(sym, false, ruleNum++, grammar.getClassName() + "::");
			}
			exitIfError();
		}

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			genSemPredMap(grammar.getClassName() + "::");

		// Generate the bitsets used throughout the lexer
		genBitsets(bitsetsUsed, ((LexerGrammar)grammar).charVocabulary.size(), grammar.getClassName() + "::", false);

		println("");
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the lexer output stream
		currentOutput.close();
		currentOutput = null;
	}
	public void genBody(ParserGrammar g) throws IOException {
		// Open the output stream for the parser and set the currentOutput
		outputFile = grammar.getClassName() + ".cpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = grammar.buildAST;

		tabs = 0;

		// Generate the header common to all output files.
		genHeader(outputFile);

		printHeaderAction(preIncludeCpp);

		// Generate header for the parser
		println("#include \"" + grammar.getClassName() + ".hpp\"");
		println("#include \"antlr/NoViableAltException.hpp\"");
		println("#include \"antlr/SemanticException.hpp\"");

		printHeaderAction(postIncludeCpp);

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		// Output the user-defined parser preamble
		printAction(grammar.preambleAction);

		String sup=null;
		if ( grammar.superClass!=null )
			sup = grammar.superClass;
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			sup = namespaceAntlr + sup;
		}

		// set up an array of all the rule names so the debugger can
		// keep track of them only by number -- less to store in tree...
		if (grammar.debuggingOutput) {
			println("const char* "+grammar.getClassName()+"::_ruleNames[] = {");
			tabs++;

			Enumeration ids = grammar.rules.elements();
			int ruleNum=0;
			while ( ids.hasMoreElements() ) {
				GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
				if ( sym instanceof RuleSymbol)
					println("\""+((RuleSymbol)sym).getId()+"\",");
			}
			println("0");
			tabs--;
			println("};");
		}

		// Generate parser class constructor from TokenBuffer
		print(grammar.getClassName() + "::" + grammar.getClassName());
		println("("+namespaceAntlr+"TokenBuffer& tokenBuf, int k)");
		println(": " + sup + "(tokenBuf,k)");
		println("{");
		tabs++;
		println("setTokenNames(_tokenNames);");
		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging();");
		}
		tabs--;
		println("}");
		println("");

		print(grammar.getClassName() + "::" + grammar.getClassName());
		println("("+namespaceAntlr+"TokenBuffer& tokenBuf)");
		println(": " + sup + "(tokenBuf," + grammar.maxk + ")");
		println("{");
		tabs++;
		println("setTokenNames(_tokenNames);");
		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging();");
		}
		tabs--;
		println("}");
		println("");

		// Generate parser class constructor from TokenStream
		print(grammar.getClassName() + "::" + grammar.getClassName());
		println("("+namespaceAntlr+"TokenStream& lexer, int k)");
		println(": " + sup + "(lexer,k)");
		println("{");
		tabs++;
		println("setTokenNames(_tokenNames);");
		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging(&lexer);");
		}
		tabs--;
		println("}");
		println("");

		print(grammar.getClassName() + "::" + grammar.getClassName());
		println("("+namespaceAntlr+"TokenStream& lexer)");
		println(": " + sup + "(lexer," + grammar.maxk + ")");
		println("{");
		tabs++;
		println("setTokenNames(_tokenNames);");
		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging(&lexer);");
		}
		tabs--;
		println("}");
		println("");

		print(grammar.getClassName() + "::" + grammar.getClassName());
		println("(const "+namespaceAntlr+"ParserSharedInputState& state)");
		println(": " + sup + "(state," + grammar.maxk + ")");
		println("{");
		tabs++;
		println("setTokenNames(_tokenNames);");
		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("setRuleNames(_ruleNames);");
			println("setSemPredNames(_semPredNames);");
			println("setupDebugging(&lexer);");
		}
		tabs--;
		println("}");
		println("");

		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		int ruleNum=0;
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRule(rs, rs.references.size()==0, ruleNum++, grammar.getClassName() + "::");
			}
			exitIfError();
		}
		if ( usingCustomAST )
		{
//			println("void "+grammar.getClassName()+"::setASTNodeFactory("+labeledElementASTType+" (*factory)() )");
//			println("{");
//			println("}");
//			println("");

			// when we are using a custom ast override Parser::getAST to return the
			// custom AST type
			println(labeledElementASTType+" "+grammar.getClassName()+"::getAST()");
			println("{");
			println("\treturn returnAST;");
			println("}");
			println("");
		}

		// Generate the token names
		genTokenStrings(grammar.getClassName() + "::");

		// Generate the bitsets used throughout the grammar
		genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType(), grammar.getClassName() + "::", true);

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			genSemPredMap(grammar.getClassName() + "::");

		// Close class definition
		println("");
		println("");
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	public void genBody(TreeWalkerGrammar g) throws IOException {
		// Open the output stream for the parser and set the currentOutput
		outputFile = grammar.getClassName() + ".cpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = grammar.buildAST;
		tabs = 0;

		// Generate the header common to all output files.
		genHeader(outputFile);

		printHeaderAction(preIncludeCpp);

		// Generate header for the parser
		println("#include \"" + grammar.getClassName() + ".hpp\"");
		println("#include \"antlr/Token.hpp\"");
		println("#include \"antlr/AST.hpp\"");
		println("#include \"antlr/NoViableAltException.hpp\"");
		println("#include \"antlr/MismatchedTokenException.hpp\"");
		println("#include \"antlr/SemanticException.hpp\"");
		println("#include \"antlr/BitSet.hpp\"");

		printHeaderAction(postIncludeCpp);

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		// Output the user-defined parser premamble
		printAction(grammar.preambleAction);

		// Generate parser class definition
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
		}
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			sup = namespaceAntlr + sup;
		}

		// Generate default parser class constructor
		println(grammar.getClassName() + "::" + grammar.getClassName() + "()");
		println("\t: "+namespaceAntlr+"TreeParser() {");
		tabs++;
		println("setTokenNames(_tokenNames);");
		tabs--;
		println("}");
		println("");

		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		int ruleNum=0;
		String ruleNameInits = "";
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRule(rs, rs.references.size()==0, ruleNum++, grammar.getClassName() + "::");
			}
			exitIfError();
		}

		if ( usingCustomAST )
		{
			// when we are using a custom ast override Parser::getAST to return the
			// custom AST type
			println(labeledElementASTType+" "+grammar.getClassName()+"::getAST()");
			println("{");
			println("\treturn returnAST;");
			println("}");
			println("");
		}

		// Generate the token names
		genTokenStrings(grammar.getClassName() + "::");

		// Generate the bitsets used throughout the grammar
		genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType(), grammar.getClassName() + "::", true);

		// Close class definition
		println("");
		println("");

		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	/** Generate a series of case statements that implement a BitSet test.
	 * @param p The Bitset for which cases are to be generated
	 */
	protected void genCases(BitSet p) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genCases("+p+")");
		int[] elems;

		elems = p.toArray();
		// Wrap cases four-per-line for lexer, one-per-line for parser
		int wrap = 1; //(grammar instanceof LexerGrammar) ? 4 : 1;
		int j=1;
		boolean startOfLine = true;
		for (int i = 0; i < elems.length; i++) {
			if (j==1) {
				print("");
			} else {
				_print("  ");
			}
			_print("case " + getValueString(elems[i]) + ":");

			if (j==wrap) {
				_println("");
				startOfLine = true;
				j=1;
			}
			else {
				j++;
				startOfLine = false;
			}
		}
		if (!startOfLine) {
			_println("");
		}
	}
	/**Generate common code for a block of alternatives; return a postscript
	 * that needs to be generated at the end of the block.  Other routines
	 * may append else-clauses and such for error checking before the postfix
	 * is generated.
	 * If the grammar is a lexer, then generate alternatives in an order where
	 * alternatives requiring deeper lookahead are generated first, and
	 * EOF in the lookahead set reduces the depth of the lookahead.
	 * @param blk The block to generate
	 * @param noTestForSingle If true, then it does not generate a test for a single alternative.
	 */
	public CppBlockFinishingInfo genCommonBlock(
		AlternativeBlock blk,
		boolean noTestForSingle)
	{
		int nIF=0;
		boolean createdLL1Switch = false;
		int closingBracesOfIFSequence = 0;
		CppBlockFinishingInfo finishingInfo = new CppBlockFinishingInfo();
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genCommonBlk("+blk+")");

		// Save the AST generation state, and set it to that of the block
		boolean savegenAST = genAST;
		genAST = genAST && blk.getAutoGen();

		boolean oldsaveTest = saveText;
		saveText = saveText && blk.getAutoGen();

		// Is this block inverted?  If so, generate special-case code
		if (
			blk.not &&
			analyzer.subruleCanBeInverted(blk, grammar instanceof LexerGrammar)
		) {
			Lookahead p = analyzer.look(1, blk);
			// Variable assignment for labeled elements
			if (blk.getLabel() != null && syntacticPredLevel == 0) {
				println(blk.getLabel() + " = " + lt1Value + ";");
			}

			// AST
			genElementAST(blk);

			String astArgs="";
			if (grammar instanceof TreeWalkerGrammar) {
				if( usingCustomAST )
					astArgs="static_cast<"+namespaceAntlr+"RefAST"+">(_t),";
				else
					astArgs="_t,";
			}

			// match the bitset for the alternative
			println("match(" + astArgs + getBitsetName(markBitsetForGen(p.fset)) + ");");

			// tack on tree cursor motion if doing a tree walker
			if (grammar instanceof TreeWalkerGrammar) {
				println("_t = _t->getNextSibling();");
			}
			return finishingInfo;
		}

		// Special handling for single alt
		if (blk.getAlternatives().size() == 1) {
			Alternative alt = blk.getAlternativeAt(0);
			// Generate a warning if there is a synPred for single alt.
			if (alt.synPred != null)
			{
				tool.warning(
					"Syntactic predicate superfluous for single alternative",
					grammar.getFilename(),
					blk.getAlternativeAt(0).synPred.getLine()
				);
			}
			if (noTestForSingle) {
				if (alt.semPred != null) {
					// Generate validating predicate
					genSemPred(alt.semPred, blk.line);
				}
				genAlt(alt, blk);
				return finishingInfo;
			}
		}

		// count number of simple LL(1) cases; only do switch for
		// many LL(1) cases (no preds, no end of token refs)
		// We don't care about exit paths for (...)*, (...)+
		// because we don't explicitly have a test for them
		// as an alt in the loop.
		//
		// Also, we now count how many unicode lookahead sets
		// there are--they must be moved to DEFAULT or ELSE
		// clause.

		int nLL1 = 0;
		for (int i=0; i<blk.getAlternatives().size(); i++) {
			Alternative a = blk.getAlternativeAt(i);
			if ( suitableForCaseExpression(a) ) {
				nLL1++;
			}
		}

		// do LL(1) cases
		if ( nLL1 >= makeSwitchThreshold) {
			// Determine the name of the item to be compared
			String testExpr = lookaheadString(1);
			createdLL1Switch = true;
			// when parsing trees, convert null to valid tree node with NULL lookahead
			if ( grammar instanceof TreeWalkerGrammar ) {
				println("if (_t == "+labeledElementASTInit+" )");
				tabs++;
				println("_t = ASTNULL;");
				tabs--;
			}
			println("switch ( "+testExpr+") {");
			for (int i=0; i<blk.alternatives.size(); i++) {
				Alternative alt = blk.getAlternativeAt(i);
				// ignore any non-LL(1) alts, predicated alts or end-of-token alts
				// or end-of-token alts for case expressions
				if ( !suitableForCaseExpression(alt) ) {
					continue;
				}
				Lookahead p = alt.cache[1];
				if (p.fset.degree() == 0 && !p.containsEpsilon()) {
					tool.warning("Alternate omitted due to empty prediction set",
						grammar.getFilename(),
						alt.head.getLine());
				}
				else {
					genCases(p.fset);
					println("{");
					tabs++;
					genAlt(alt, blk);
					println("break;");
					tabs--;
					println("}");
				}
			}
			println("default:");
			tabs++;
		}

		// do non-LL(1) and nondeterministic cases
		// This is tricky in the lexer, because of cases like:
		//     STAR : '*' ;
		//     ASSIGN_STAR : "*=";
		// Since nextToken is generated without a loop, then the STAR will
		// have end-of-token as it's lookahead set for LA(2).  So, we must generate the
		// alternatives containing trailing end-of-token in their lookahead sets *after*
		// the alternatives without end-of-token.  This implements the usual
		// lexer convention that longer matches come before shorter ones, e.g.
		// "*=" matches ASSIGN_STAR not STAR
		//
		// For non-lexer grammars, this does not sort the alternates by depth
		// Note that alts whose lookahead is purely end-of-token at k=1 end up
		// as default or else clauses.
		int startDepth = (grammar instanceof LexerGrammar) ? grammar.maxk : 0;
		for (int altDepth = startDepth; altDepth >= 0; altDepth--) {
			if ( DEBUG_CODE_GENERATOR ) System.out.println("checking depth "+altDepth);
			for (int i=0; i<blk.alternatives.size(); i++) {
				Alternative alt = blk.getAlternativeAt(i);
				if ( DEBUG_CODE_GENERATOR ) System.out.println("genAlt: "+i);
				// if we made a switch above, ignore what we already took care
				// of.  Specifically, LL(1) alts with no preds
				// that do not have end-of-token in their prediction set
				if ( createdLL1Switch &&
					 suitableForCaseExpression(alt) ) {
					if ( DEBUG_CODE_GENERATOR )
						System.out.println("ignoring alt because it was in the switch");
					continue;
				}
				String e;

				boolean unpredicted = false;

				if (grammar instanceof LexerGrammar) {
					// Calculate the "effective depth" of the alt, which is the max
					// depth at which cache[depth]!=end-of-token
					int effectiveDepth = alt.lookaheadDepth;
					if (effectiveDepth == GrammarAnalyzer.NONDETERMINISTIC) {
						// use maximum lookahead
						effectiveDepth = grammar.maxk;
					}
					while ( effectiveDepth >= 1 &&
							 alt.cache[effectiveDepth].containsEpsilon() )
					{
						effectiveDepth--;
					}
					// Ignore alts whose effective depth is other than the ones we
					// are generating for this iteration.
					if (effectiveDepth != altDepth) {
						if ( DEBUG_CODE_GENERATOR )
							System.out.println("ignoring alt because effectiveDepth!=altDepth;"+effectiveDepth+"!="+altDepth);
						continue;
					}
					unpredicted = lookaheadIsEmpty(alt, effectiveDepth);
					e = getLookaheadTestExpression(alt, effectiveDepth);
				} else {
					unpredicted = lookaheadIsEmpty(alt, grammar.maxk);
					e = getLookaheadTestExpression(alt, grammar.maxk);
				}

				// Was it a big unicode range that forced unsuitability
				// for a case expression?
				if ( alt.cache[1].fset.degree() > caseSizeThreshold )
				{
					if ( nIF==0 )
					{
						// generate this only for the first if the elseif's
						// are covered by this one
						if ( grammar instanceof TreeWalkerGrammar ) {
							println("if (_t == "+labeledElementASTInit+" )");
							tabs++;
							println("_t = ASTNULL;");
							tabs--;
						}
						println("if " + e + " {");
					}
					else
						println("else if " + e + " {");
				}
				else if (unpredicted &&
							alt.semPred==null &&
							alt.synPred==null)
				{
					// The alt has empty prediction set and no
					// predicate to help out.  if we have not
					// generated a previous if, just put {...} around
					// the end-of-token clause
					if ( nIF==0 ) {
						println("{");
					}
					else {
						println("else {");
					}
					finishingInfo.needAnErrorClause = false;
				}
				else
				{
					// check for sem and syn preds
					// Add any semantic predicate expression to the lookahead test
					if ( alt.semPred != null ) {
						// if debugging, wrap the evaluation of the predicate in a method
						//
						// translate $ and # references
						ActionTransInfo tInfo = new ActionTransInfo();
						String actionStr = processActionForTreeSpecifiers(alt.semPred,
																		  blk.line,
																		  currentRule,
																		  tInfo);
						// ignore translation info...we don't need to do anything with it.

						// call that will inform SemanticPredicateListeners of the result
						if (((grammar instanceof ParserGrammar) || (grammar instanceof LexerGrammar)) && grammar.debuggingOutput)
							e = "("+e+"&& fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.PREDICTING,"+ //FIXME
									addSemPred(charFormatter.escapeString(actionStr))+","+actionStr+"))";
						else
							e = "("+e+"&&("+actionStr +"))";
					}

					// Generate any syntactic predicates
					if ( nIF>0 ) {
						if ( alt.synPred != null ) {
							println("else {");
							tabs++;
							genSynPred( alt.synPred, e );
							closingBracesOfIFSequence++;
						}
						else {
							println("else if " + e + " {");
						}
					}
					else {
						if ( alt.synPred != null ) {
							genSynPred( alt.synPred, e );
						}
						else {
							// when parsing trees, convert null to valid tree node
							// with NULL lookahead.
							if ( grammar instanceof TreeWalkerGrammar ) {
								println("if (_t == "+labeledElementASTInit+" )");
								tabs++;
								println("_t = ASTNULL;");
								tabs--;
							}
							println("if " + e + " {");
						}
					}

				}

				nIF++;
				tabs++;
				genAlt(alt, blk);
				tabs--;
				println("}");
			}
		}
		String ps = "";
		for (int i=1; i<=closingBracesOfIFSequence; i++) {
			tabs--; // does JavaCodeGenerator need this?
			ps+="}";
		}

		// Restore the AST generation state
		genAST = savegenAST;

		// restore save text state
		saveText=oldsaveTest;

		// Return the finishing info.
		if ( createdLL1Switch ) {
			tabs--;
			finishingInfo.postscript = ps+"}";
			finishingInfo.generatedSwitch = true;
			finishingInfo.generatedAnIf = nIF>0;
			//return new CppBlockFinishingInfo(ps+"}",true,nIF>0); // close up switch statement

		}
		else {
			finishingInfo.postscript = ps;
			finishingInfo.generatedSwitch = false;
			finishingInfo.generatedAnIf = nIF>0;
			//return new CppBlockFinishingInfo(ps, false,nIF>0);
		}
		return finishingInfo;
	}

	private static boolean suitableForCaseExpression(Alternative a) {
		return a.lookaheadDepth == 1 &&
			a.semPred == null &&
			!a.cache[1].containsEpsilon() &&
			a.cache[1].fset.degree()<=caseSizeThreshold;
	}

	/** Generate code to link an element reference into the AST */
	private void genElementAST(AlternativeElement el) {
		// handle case where you're not building trees, but are in tree walker.
		// Just need to get labels set up.
		if ( grammar instanceof TreeWalkerGrammar && !grammar.buildAST ) {
			String elementRef;
			String astName;

			// Generate names and declarations of the AST variable(s)
			if (el.getLabel() == null) {
				elementRef = lt1Value;
				// Generate AST variables for unlabeled stuff
				astName = "tmp" + astVarNumber + "_AST";
				astVarNumber++;
				// Map the generated AST variable in the alternate
				mapTreeVariable(el, astName);
				// Generate an "input" AST variable also
				println(labeledElementASTType+" "+astName+"_in = "+elementRef+";");
			}
			return;
		}

		if (grammar.buildAST && syntacticPredLevel == 0) {
			boolean doNoGuessTest = (
				grammar.hasSyntacticPredicate &&
				(
					el.getLabel() != null ||
					el.getAutoGenType() != GrammarElement.AUTO_GEN_BANG
				)
			);

			String elementRef;
			String astName;

			// Generate names and declarations of the AST variable(s)
			if (el.getLabel() != null) {
				elementRef = el.getLabel();
				astName = el.getLabel() + "_AST";
			} else {
				elementRef = lt1Value;
				// Generate AST variables for unlabeled stuff
				astName = "tmp" + astVarNumber + "_AST";
				astVarNumber++;
				// Generate the declaration
				if ( el instanceof GrammarAtom ) {
					GrammarAtom ga = (GrammarAtom)el;
					if ( ga.getASTNodeType()!=null ) {
						println("Ref"+ga.getASTNodeType()+" " + astName + ";");
					}
					else {
						println(labeledElementASTType+" " + astName + " = "+labeledElementASTInit+";");
					}
				}
				else {
					println(labeledElementASTType+" " + astName + " = "+labeledElementASTInit+";");
				}

				// Map the generated AST variable in the alternate
				mapTreeVariable(el, astName);
				if (grammar instanceof TreeWalkerGrammar) {
					// Generate an "input" AST variable also
					println(labeledElementASTType+" " + astName + "_in = "+labeledElementASTInit+";");
				}
			}

			// Enclose actions with !guessing
			if (doNoGuessTest) {
				println("if (inputState->guessing==0) {");
				tabs++;
			}

			if (el.getLabel() != null) {
				if ( el instanceof GrammarAtom ) {
					println(astName + " = "+
							getASTCreateString((GrammarAtom)el,elementRef) + ";");
				}
				else {
					println(astName + " = "+
							getASTCreateString(elementRef) + ";");
				}
			} else {
				elementRef = lt1Value;
				if ( el instanceof GrammarAtom ) {
					println(astName + " = "+
							getASTCreateString((GrammarAtom)el,elementRef) + ";");
				}
				else {
					println(astName + " = "+
							getASTCreateString(elementRef) + ";");
				}
				// Map the generated AST variable in the alternate
				if (grammar instanceof TreeWalkerGrammar) {
					// set "input" AST variable also
					println(astName + "_in = " + elementRef + ";");
				}
			}

			if (genAST) {
				switch (el.getAutoGenType()) {
				case GrammarElement.AUTO_GEN_NONE:
					println("astFactory.addASTChild(currentAST, "+namespaceAntlr+"RefAST(" + astName + "));");
					break;
				case GrammarElement.AUTO_GEN_CARET:
//					println("astFactory.makeASTRoot(currentAST, " + astName + ");");
					println("astFactory.makeASTRoot(currentAST, "+namespaceAntlr+"RefAST(" + astName + "));");
					break;
				default:
					break;
				}
			}
			if (doNoGuessTest) {
				tabs--;
				println("}");
			}
		}
	}
	/** Close the try block and generate catch phrases
	 * if the element has a labeled handler in the rule
	 */
	private void genErrorCatchForElement(AlternativeElement el) {
		if (el.getLabel() == null) return;
		String r = el.enclosingRuleName;
		if ( grammar instanceof LexerGrammar ) {
			r = CodeGenerator.lexerRuleName(el.enclosingRuleName);
		}
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
		if (rs == null) {
			tool.panic("Enclosing rule not found!");
		}
		ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
		if (ex != null) {
			tabs--;
			println("}");
			genErrorHandler(ex);
		}
	}
	/** Generate the catch phrases for a user-specified error handler */
	private void genErrorHandler(ExceptionSpec ex) {
		// Each ExceptionHandler in the ExceptionSpec is a separate catch
		for (int i = 0; i < ex.handlers.size(); i++)
		{
			ExceptionHandler handler = (ExceptionHandler)ex.handlers.elementAt(i);
			// Generate catch phrase
			println("catch (" + handler.exceptionTypeAndName.getText() + ") {");
			tabs++;
			if (grammar.hasSyntacticPredicate) {
				println("if (inputState->guessing==0) {");
				tabs++;
			}

			// When not guessing, execute user handler action
			genLineNo(handler.action);
			printAction(
				processActionForTreeSpecifiers(handler.action.getText(), 0, currentRule, null)
			);
			genLineNo2();

			if (grammar.hasSyntacticPredicate) {
				tabs--;
				println("} else {");
				tabs++;
				// When guessing, rethrow exception
				println(
					"throw " +
					extractIdOfAction(handler.exceptionTypeAndName) +
					";"
				);
				tabs--;
				println("}");
			}
			// Close catch phrase
			tabs--;
			println("}");
		}
	}
	/** Generate a try { opening if the element has a labeled handler in the rule */
	private void genErrorTryForElement(AlternativeElement el) {
		if (el.getLabel() == null) return;
		String r = el.enclosingRuleName;
		if ( grammar instanceof LexerGrammar ) {
			r = CodeGenerator.lexerRuleName(el.enclosingRuleName);
		}
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
		if (rs == null) {
			tool.panic("Enclosing rule not found!");
		}
		ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
		if (ex != null) {
			println("try { // for error handling");
			tabs++;
		}
	}
	/** Generate a header that is common to all C++ files */
	protected void genHeader(String fileName)
	{
		println("/* $ANTLR "+Tool.version+": "+
				"\""+Tool.fileMinusPath(tool.grammarFile)+"\""+
				" -> "+
				"\""+fileName+"\"$ */");
	}
	// these are unique to C++ mode

	public void genInclude(LexerGrammar g) throws IOException {
		outputFile = grammar.getClassName() + ".hpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = false;	// no way to gen trees.
		saveText = true;	// save consumed characters.

		tabs=0;

		// Generate a guard wrapper
		println("#ifndef INC_"+grammar.getClassName()+"_hpp_");
		println("#define INC_"+grammar.getClassName()+"_hpp_");
		println("");

		printHeaderAction(preIncludeHpp);

		println("#include \"antlr/config.hpp\"");

		// Generate header common to all C++ output files
		genHeader(outputFile);

		// Generate header specific to lexer header file
		println("#include \"antlr/CommonToken.hpp\"");
		println("#include \"antlr/InputBuffer.hpp\"");
		println("#include \"antlr/BitSet.hpp\"");
		println("#include \"" + grammar.tokenManager.getName() + TokenTypesFileSuffix+".hpp\"");

		// Find the name of the super class
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
			println("#include \""+sup+".hpp\"");
		}
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			println("#include \"antlr/"+sup+".hpp\"");
			sup = namespaceAntlr + sup;
		}

		// Do not use printAction because we assume tabs==0
		printHeaderAction(postIncludeHpp);

		if (nameSpace != null)
			   nameSpace.emitDeclarations(currentOutput);

		printHeaderAction("");

		// print javadoc comment if any
		if ( grammar.comment!=null ) {
			_println(grammar.comment);
		}

		// Generate lexer class definition
		print("class " + grammar.getClassName() + " : public " + sup);
		println(", public " + grammar.tokenManager.getName() + TokenTypesFileSuffix);

		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null ) {
			String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null ) {
				print(", "+suffix);  // must be an interface name for Java
			}
		}
		println(" {");

		// Generate user-defined lexer class members
		if (grammar.classMemberAction != null) {
			genLineNo(grammar.classMemberAction);
			print(
				processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
			);
			genLineNo2();
		}

		// Generate initLiterals() method
		tabs=0;
		println("private:");
		tabs=1;
		println("void initLiterals();");

		// Generate getCaseSensitiveLiterals() method
		tabs=0;
		println("public:");
		tabs=1;
		println("bool getCaseSensitiveLiterals() const;");

		// Make constructors public
		tabs=0;
		println("public:");
		tabs=1;

		// Generate the constructor from std::istream
		println(grammar.getClassName() + "(" + namespaceStd + "istream& in);");

		// Generate the constructor from InputBuffer
		println(grammar.getClassName() + "("+namespaceAntlr+"InputBuffer& ib);");

		println(grammar.getClassName() + "(const "+namespaceAntlr+"LexerSharedInputState& state);");

		// Generate nextToken() rule.
		// nextToken() is a synthetic lexer rule that is the implicit OR of all
		// user-defined lexer rules.
		println(namespaceAntlr+"RefToken nextToken();");

		// Generate code for each rule in the lexer
		Enumeration ids = grammar.rules.elements();
		while ( ids.hasMoreElements() ) {
			RuleSymbol sym = (RuleSymbol) ids.nextElement();
			// Don't generate the synthetic rules
			if (!sym.getId().equals("mnextToken")) {
				genRuleHeader(sym, false);
			}
			exitIfError();
		}

		// Make the rest private
		tabs=0;
		println("private:");
		tabs=1;

		// generate the rule name array for debugging
		if ( grammar.debuggingOutput ) {
			println("static const char* _ruleNames[];");
		}

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			println("static const char* _semPredNames[];");

		// Generate the bitsets used throughout the lexer
		genBitsetsHeader(bitsetsUsed, ((LexerGrammar)grammar).charVocabulary.size());

		tabs=0;
		println("};");
		println("");
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Generate a guard wrapper
		println("#endif /*INC_"+grammar.getClassName()+"_hpp_*/");

		// Close the lexer output stream
		currentOutput.close();
		currentOutput = null;
	}
	public void genInclude(ParserGrammar g) throws IOException {
		// Open the output stream for the parser and set the currentOutput
		outputFile = grammar.getClassName() + ".hpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = grammar.buildAST;

		tabs = 0;

		// Generate a guard wrapper
		println("#ifndef INC_"+grammar.getClassName()+"_hpp_");
		println("#define INC_"+grammar.getClassName()+"_hpp_");
		println("");
		printHeaderAction(preIncludeHpp);
		println("#include \"antlr/config.hpp\"");

		// Generate the header common to all output files.
		genHeader(outputFile);

		// Generate header for the parser
		println("#include \"antlr/TokenStream.hpp\"");
		println("#include \"antlr/TokenBuffer.hpp\"");
		println("#include \"" + grammar.tokenManager.getName() + TokenTypesFileSuffix+".hpp\"");

		// Generate parser class definition
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
			println("#include \""+sup+".hpp\"");
		}
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			println("#include \"antlr/"+sup+".hpp\"");
			sup = namespaceAntlr + sup;
		}
		println("");

		// Do not use printAction because we assume tabs==0
		printHeaderAction(postIncludeHpp);

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		printHeaderAction("");

		// print javadoc comment if any
		if ( grammar.comment!=null ) {
			_println(grammar.comment);
		}

		// generate the actual class definition
		print("class " + grammar.getClassName() + " : public " + sup);
		println(", public " + grammar.tokenManager.getName() + TokenTypesFileSuffix);

		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null ) {
			String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null )
				print(", "+suffix);  // must be an interface name for Java
		}
		println(" {");

		// set up an array of all the rule names so the debugger can
		// keep track of them only by number -- less to store in tree...
		if (grammar.debuggingOutput) {
			println("public: static const char* _ruleNames[];");
		}

		// Generate user-defined parser class members
		if (grammar.classMemberAction != null) {
			genLineNo(grammar.classMemberAction.getLine());
			print(
				processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
			);
			genLineNo2();
		}

		// Generate parser class constructor from TokenBuffer
		tabs=0;
		println("protected:");
		tabs=1;
		println(grammar.getClassName() + "("+namespaceAntlr+"TokenBuffer& tokenBuf, int k);");
		tabs=0;
		println("public:");
		tabs=1;
		println(grammar.getClassName() + "("+namespaceAntlr+"TokenBuffer& tokenBuf);");

		// Generate parser class constructor from TokenStream
		tabs=0;
		println("protected:");
		tabs=1;
		println(grammar.getClassName()+"("+namespaceAntlr+"TokenStream& lexer, int k);");
		tabs=0;
		println("public:");
		tabs=1;
		println(grammar.getClassName()+"("+namespaceAntlr+"TokenStream& lexer);");

		println(grammar.getClassName()+"(const "+namespaceAntlr+"ParserSharedInputState& state);");

		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRuleHeader(rs, rs.references.size()==0);
			}
			exitIfError();
		}

		if ( usingCustomAST )
		{
			// when we are using a custom ast override Parser::getAST to return the
			// custom AST type
			tabs=0; println("public:"); tabs=1;
			println(labeledElementASTType+" getAST();");
			println("");
			tabs=0; println("protected:"); tabs=1;
			println(labeledElementASTType+" returnAST;");
		}

		// Make the rest private
		tabs=0;
		println("private:");
		tabs=1;

		// Generate the token names
		println("static const char* _tokenNames[];");

		// Generate the bitsets used throughout the grammar
		genBitsetsHeader(bitsetsUsed, grammar.tokenManager.maxTokenType());

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			println("static const char* _semPredNames[];");

		// Close class definition
		tabs=0;
		println("};");
		println("");
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Generate a guard wrapper
		println("#endif /*INC_"+grammar.getClassName()+"_hpp_*/");

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	public void genInclude(TreeWalkerGrammar g) throws IOException {
		// Open the output stream for the parser and set the currentOutput
		outputFile = grammar.getClassName() + ".hpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		genAST = grammar.buildAST;
		tabs = 0;

		// Generate a guard wrapper
		println("#ifndef INC_"+grammar.getClassName()+"_hpp_");
		println("#define INC_"+grammar.getClassName()+"_hpp_");
		println("");
		printHeaderAction(preIncludeHpp);
		println("#include \"antlr/config.hpp\"");
		println("#include \"" + grammar.tokenManager.getName() + TokenTypesFileSuffix+".hpp\"");

		// Generate the header common to all output files.
		genHeader(outputFile);

		// Find the name of the super class
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
			println("#include \""+sup+".hpp\"");
		}
		else {
			sup = grammar.getSuperClass();
			if (sup.lastIndexOf('.') != -1)
				sup = sup.substring(sup.lastIndexOf('.')+1);
			println("#include \"antlr/"+sup+".hpp\"");
			sup = namespaceAntlr + sup;
		}
		println("");

		// Generate header for the parser

		// Do not use printAction because we assume tabs==0
		printHeaderAction(postIncludeHpp);

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		printHeaderAction("");

		// print javadoc comment if any
		if ( grammar.comment!=null ) {
			_println(grammar.comment);
		}

		// Generate parser class definition
		print("class " + grammar.getClassName() + " : public "+sup);
		println(", public " + grammar.tokenManager.getName() + TokenTypesFileSuffix);

		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null ) {
			String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null ) {
				print(", "+suffix);  // must be an interface name for Java
			}
		}
		println(" {");

		// Generate user-defined parser class members
		if (grammar.classMemberAction != null) {
			genLineNo(grammar.classMemberAction.getLine());
			print(
				processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
			);
			genLineNo2();
		}

		// Generate default parser class constructor
		tabs=0;
		println("public:");
		tabs=1;
		println(grammar.getClassName() + "();");

		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		String ruleNameInits = "";
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRuleHeader(rs, rs.references.size()==0);
			}
			exitIfError();
		}
		if ( usingCustomAST )
		{
			// when we are using a custom ast override TreeParser::getAST to return the
			// custom AST type
			tabs=0; println("public:"); tabs=1;
			println(labeledElementASTType+" getAST();");
			println("");
			tabs=0; println("protected:"); tabs=1;
			println(labeledElementASTType+" returnAST;");
			println(labeledElementASTType+" _retTree;");
		}

		// Make the rest private
		tabs=0;
		println("private:");
		tabs=1;

		// Generate the token names
		println("static const char* _tokenNames[];");

		// Generate the bitsets used throughout the grammar
		genBitsetsHeader(bitsetsUsed, grammar.tokenManager.maxTokenType());

		// Close class definition
		tabs=0;
		println("};");
		println("");
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Generate a guard wrapper
		println("#endif /*INC_"+grammar.getClassName()+"_hpp_*/");

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	private void genLiteralsTest() {
		println("_ttype = testLiteralsTable(_ttype);");
	}
	private void genLiteralsTestForPartialToken() {
		println("_ttype = testLiteralsTable(text.substr(_begin, text.length()-_begin),_ttype);");
	}
	protected void genMatch(BitSet b) {
	}
	protected void genMatch(GrammarAtom atom) {
		if ( atom instanceof StringLiteralElement ) {
			if ( grammar instanceof LexerGrammar ) {
				genMatchUsingAtomText(atom);
			}
			else {
				genMatchUsingAtomTokenType(atom);
			}
		}
		else if ( atom instanceof CharLiteralElement ) {
			if ( grammar instanceof LexerGrammar ) {
				genMatchUsingAtomText(atom);
			}
			else {
				tool.error("cannot ref character literals in grammar: "+atom);
			}
		}
		else if ( atom instanceof TokenRefElement ) {
			genMatchUsingAtomText(atom);
		}
	}
	protected void genMatchUsingAtomText(GrammarAtom atom) {
		// match() for trees needs the _t cursor
		String astArgs="";
		if (grammar instanceof TreeWalkerGrammar) {
			if( usingCustomAST )
				astArgs="static_cast<"+namespaceAntlr+"RefAST"+">(_t),";
			else
				astArgs="_t,";
		}

		// if in lexer and ! on element, save buffer index to kill later
		if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			println("_saveIndex=text.length();");
		}

		print(atom.not ? "matchNot(" : "match(");
		_print(astArgs);

		// print out what to match
		if (atom.atomText.equals("EOF")) {
			// horrible hack to handle EOF case
			_print(namespaceAntlr+"Token::EOF_TYPE");
		}
		else {
			_print(textOrChar(atom.atomText));
		}
		_println(");");

		if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			println("text.erase(_saveIndex);");      // kill text atom put in buffer
		}
	}
	protected void genMatchUsingAtomTokenType(GrammarAtom atom) {
		// match() for trees needs the _t cursor
		String astArgs="";
		if (grammar instanceof TreeWalkerGrammar) {
			if( usingCustomAST )
				astArgs="static_cast<"+namespaceAntlr+"RefAST"+">(_t),";
			else
				astArgs="_t,";
		}

		// If the literal can be mangled, generate the symbolic constant instead
		String mangledName = null;
		String s = astArgs + getValueString(atom.getType());

		// matching
		println( (atom.not ? "matchNot(" : "match(") + s + ");");
	}
	/** Generate the nextToken() rule.
	 * nextToken() is a synthetic lexer rule that is the implicit OR of all
	 * user-defined lexer rules.
	 * @param RuleBlock
	 */
	public void genNextToken() {
		// Are there any public rules?  If not, then just generate a
		// fake nextToken().
		boolean hasPublicRules = false;
		for (int i = 0; i < grammar.rules.size(); i++) {
			RuleSymbol rs = (RuleSymbol)grammar.rules.elementAt(i);
			if ( rs.isDefined() && rs.access.equals("public") ) {
				hasPublicRules = true;
				break;
			}
		}
		if (!hasPublicRules) {
			println("");
			println(namespaceAntlr+"RefToken "+grammar.getClassName()+"::nextToken() { return "+namespaceAntlr+"RefToken(new "+namespaceAntlr+"CommonToken("+namespaceAntlr+"Token::EOF_TYPE, \"\")); }");
			println("");
			return;
		}

		// Create the synthesized nextToken() rule
		RuleBlock nextTokenBlk = MakeGrammar.createNextTokenRule(grammar, grammar.rules, "nextToken");
		// Define the nextToken rule symbol
		RuleSymbol nextTokenRs = new RuleSymbol("mnextToken");
		nextTokenRs.setDefined();
		nextTokenRs.setBlock(nextTokenBlk);
		nextTokenRs.access = "private";
		grammar.define(nextTokenRs);
		// Analyze the nextToken rule
		boolean ok = grammar.theLLkAnalyzer.deterministic(nextTokenBlk);

		// Generate the next token rule
		String filterRule=null;
		if ( ((LexerGrammar)grammar).filterMode ) {
			filterRule = ((LexerGrammar)grammar).filterRule;
		}

		println("");
		println(namespaceAntlr+"RefToken "+grammar.getClassName()+"::nextToken()");
		println("{");
		tabs++;
		println(namespaceAntlr+"RefToken theRetToken;");
		println("for (;;) {");
		tabs++;
		println(namespaceAntlr+"RefToken theRetToken;");
		println("int _ttype = "+namespaceAntlr+"Token::INVALID_TYPE;");
		if ( ((LexerGrammar)grammar).filterMode ) {
			println("setCommitToPath(false);");
			if ( filterRule!=null ) {
				// Here's a good place to ensure that the filter rule actually exists
				if ( !grammar.isDefined(CodeGenerator.lexerRuleName(filterRule)) ) {
					grammar.tool.error("Filter rule "+filterRule+" does not exist in this lexer");
				}
				else {
					RuleSymbol rs = (RuleSymbol)grammar.getSymbol(CodeGenerator.lexerRuleName(filterRule));
					if ( !rs.isDefined() ) {
						grammar.tool.error("Filter rule "+filterRule+" does not exist in this lexer");
					}
					else if ( rs.access.equals("public") ) {
						grammar.tool.error("Filter rule "+filterRule+" must be protected");
					}
				}
				println("int _m;");
				println("_m = mark();");
			}
		}
		println("resetText();");

		println("try {   // for char stream error handling");
		tabs++;

		// Generate try around whole thing to trap scanner errors
		println("try {   // for lexical error handling");
		tabs++;

		// Test for public lexical rules with empty paths
		for (int i=0; i<nextTokenBlk.getAlternatives().size(); i++) {
			Alternative a = nextTokenBlk.getAlternativeAt(i);
			if ( a.cache[1].containsEpsilon() ) {
				tool.warning("found optional path in nextToken()");
			}
		}

		// Generate the block
		String newline = System.getProperty("line.separator");
		CppBlockFinishingInfo howToFinish = genCommonBlock(nextTokenBlk, false);
		String errFinish = "if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken("+namespaceAntlr+"Token::EOF_TYPE);}";
		errFinish += newline+"\t\t\t\t";
		if ( ((LexerGrammar)grammar).filterMode ) {
			if ( filterRule==null ) {
				errFinish += "else {consume(); goto tryAgain;}";
			}
			else {
				errFinish += "else {"+newline+
						"\t\t\t\t\tcommit();"+newline+
						"\t\t\t\t\ttry {m"+filterRule+"(false);}"+newline+
						"\t\t\t\t\tcatch("+namespaceAntlr+"RecognitionException& e) {"+newline+
						"\t\t\t\t\t	// catastrophic failure"+newline+
						"\t\t\t\t\t	reportError(e);"+newline+
						"\t\t\t\t\t	consume();"+newline+
						"\t\t\t\t\t}"+newline+
 						"\t\t\t\t\tgoto tryAgain;"+newline+
 						"\t\t\t\t}";
			}
		}
		else {
			errFinish += "else {"+throwNoViable+"}";
		}
		genBlockFinish(howToFinish, errFinish);

		// at this point a valid token has been matched, undo "mark" that was done
		if ( ((LexerGrammar)grammar).filterMode && filterRule!=null ) {
			println("commit();");
		}

		// Generate literals test if desired
		// make sure _ttype is set first; note _returnToken must be
		// non-null as the rule was required to create it.
		println("if ( !_returnToken ) goto tryAgain; // found SKIP token");
		println("_ttype = _returnToken->getType();");
		if ( ((LexerGrammar)grammar).getTestLiterals()) {
			genLiteralsTest();
		}

		// return token created by rule reference in switch
		println("_returnToken->setType(_ttype);");
		println("return _returnToken;");

		// Close try block
		tabs--;
		println("}");
		println("catch ("+namespaceAntlr+"RecognitionException& e) {");
		tabs++;
		if ( ((LexerGrammar)grammar).filterMode ) {
			if ( filterRule==null ) {
				println("if ( !getCommitToPath() ) {consume(); goto tryAgain;}");
			}
			else {
				println("if ( !getCommitToPath() ) {");
				tabs++;
				println("rewind(_m);");
				println("resetText();");
				println("try {m"+filterRule+"(false);}");
				println("catch("+namespaceAntlr+"RecognitionException& ee) {");
				println("	// horrendous failure: error in filter rule");
				println("	reportError(ee);");
				println("	consume();");
				println("}");
				println("goto tryAgain;");
				tabs--;
				println("}");
			}
		}
		if ( nextTokenBlk.getDefaultErrorHandler() ) {
			println("reportError(e);");
			println("consume();");
		}
		else {
		    // pass on to invoking routine
		    println("throw "+namespaceAntlr+"TokenStreamRecognitionException(e);");
		}
		tabs--;
		println("}");

		// close CharStreamException try
		tabs--;
		println("}");
		println("catch ("+namespaceAntlr+"CharStreamIOException& csie) {");
		println("   throw "+namespaceAntlr+"TokenStreamIOException(csie.io);");
		println("}");
		println("catch ("+namespaceAntlr+"CharStreamException& cse) {");
		println("   throw "+namespaceAntlr+"TokenStreamException(cse.getMessage());");
		println("}");

		// close for-loop
		_println("tryAgain:;");
		tabs--;
		println("}");

		// close method nextToken
		tabs--;
		println("}");
		println("");
	}
	/** Gen a named rule block.
	 * ASTs are generated for each element of an alternative unless
	 * the rule or the alternative have a '!' modifier.
	 *
	 * If an alternative defeats the default tree construction, it
	 * must set <rule>_AST to the root of the returned AST.
	 *
	 * Each alternative that does automatic tree construction, builds
	 * up root and child list pointers in an ASTPair structure.
	 *
	 * A rule finishes by setting the returnAST variable from the
	 * ASTPair.
	 *
	 * @param rule The name of the rule to generate
	 * @param startSymbol true if the rule is a start symbol (i.e., not referenced elsewhere)
	*/
	public void genRule(RuleSymbol s, boolean startSymbol, int ruleNum, String prefix) {
//		tabs=1; // JavaCodeGenerator needs this
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRule("+ s.getId() +")");
		if ( !s.isDefined() ) {
			tool.error("undefined rule: "+ s.getId());
			return;
		}

		// Generate rule return type, name, arguments
		RuleBlock rblk = s.getBlock();
		currentRule = rblk;
		currentASTResult = s.getId();

		// Save the AST generation state, and set it to that of the rule
		boolean savegenAST = genAST;
		genAST = genAST && rblk.getAutoGen();

		// boolean oldsaveTest = saveText;
		saveText = rblk.getAutoGen();

		// print javadoc comment if any
		if ( s.comment!=null ) {
			_println(s.comment);
		}

		// Gen method access and final qualifier
//		print(s.access + " final ");

		// Gen method return type (note lexer return action set at rule creation)
		if (rblk.returnAction != null)
		{
			// Has specified return value
			_print(extractTypeOfAction(rblk.returnAction, rblk.getLine()) + " ");
		} else {
			// No specified return value
			_print("void ");
		}

		// Gen method name
		_print(prefix + s.getId() + "(");

		// Additional rule parameters common to all rules for this grammar
		_print(commonExtraParams);
		if (commonExtraParams.length() != 0 && rblk.argAction != null ) {
			_print(",");
		}

		// Gen arguments
		if (rblk.argAction != null)
		{
			// Has specified arguments
			_println("");
// FIXME: make argAction also a token? Hmmmmm
//			genLineNo(rblk);
			tabs++;
			println(rblk.argAction);
			tabs--;
			print(")");
//			genLineNo2();	// gcc gives error on the brace... hope it works for the others too
		} else {
			// No specified arguments
			_print(")");
		}

		// Gen throws clause and open curly
//		_print(" throws " + exceptionThrown);
//		if ( !(grammar instanceof TreeWalkerGrammar) ) {
//			_print(", IOException");
//		}
		_println(" {");
		tabs++;

		// Convert return action to variable declaration
		if (rblk.returnAction != null)
		{
			genLineNo(rblk);
			println(rblk.returnAction + ";");
			genLineNo2();
		}

		// print out definitions needed by rules for various grammar types
		if (!commonLocalVars.equals(""))
			println(commonLocalVars);

		if (grammar.traceRules) {
			if ( grammar instanceof TreeWalkerGrammar ) {
				if ( usingCustomAST )
					println("Tracer traceInOut(this,\""+ s.getId() +"\",static_cast<"+namespaceAntlr+"RefAST"+">(_t));");
				else
					println("Tracer traceInOut(this,\""+ s.getId() +"\",_t);");
			}
			else {
				println("Tracer traceInOut(this, \""+ s.getId() +"\");");
			}
		}

		if ( grammar instanceof LexerGrammar ) {
			// lexer rule default return value is the rule's token name
			// This is a horrible hack to support the built-in EOF lexer rule.
			if (s.getId().equals("mEOF"))
				println("_ttype = "+namespaceAntlr+"Token::EOF_TYPE;");
			else
				println("_ttype = "+ s.getId().substring(1)+";");
			println("int _saveIndex;");		// used for element! (so we can kill text matched for element)
/*
			println("boolean old_saveConsumedInput=saveConsumedInput;");
			if ( !rblk.getAutoGen() ) {      // turn off "save input" if ! on rule
				println("saveConsumedInput=false;");
			}
*/
		}

		// if debugging, write code to mark entry to the rule
		if ( grammar.debuggingOutput)
		    if (grammar instanceof ParserGrammar)
				println("fireEnterRule(" + ruleNum + ",0);");
			else if (grammar instanceof LexerGrammar)
				println("fireEnterRule(" + ruleNum + ",_ttype);");

		// Generate trace code if desired
//		if ( grammar.debuggingOutput || grammar.traceRules) {
//			println("try { // debugging");
//			tabs++;
//		}

		// Initialize AST variables
		if (grammar instanceof TreeWalkerGrammar) {
			// "Input" value for rule
//			println(labeledElementASTType+" " + s.getId() + "_AST_in = "+labeledElementASTType+"(_t);");
			println(labeledElementASTType+" " + s.getId() + "_AST_in = _t;");
		}
		if (grammar.buildAST) {
			// Parser member used to pass AST returns from rule invocations
			println("returnAST = "+labeledElementASTInit+";");
			// Tracks AST construction
			println(namespaceAntlr+"ASTPair currentAST;"); // = new ASTPair();");
			// User-settable return value for rule.
			println(labeledElementASTType+" " + s.getId() + "_AST = "+labeledElementASTInit+";");
		}

		genBlockPreamble(rblk);
		println("");

		// Search for an unlabeled exception specification attached to the rule
		ExceptionSpec unlabeledUserSpec = rblk.findExceptionSpec("");

		// Generate try block around the entire rule for  error handling
		if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
			println("try {      // for error handling");
			tabs++;
		}

		// Generate the alternatives
		if ( rblk.alternatives.size()==1 ) {
			// One alternative -- use simple form
			Alternative alt = rblk.getAlternativeAt(0);
			String pred = alt.semPred;
			if ( pred!=null )
				genSemPred(pred, currentRule.line);
			if (alt.synPred != null) {
				tool.warning(
					"Syntactic predicate ignored for single alternative",
					grammar.getFilename(),
					alt.synPred.getLine()
				);
			}
			genAlt(alt, rblk);
		}
		else {
			// Multiple alternatives -- generate complex form
			boolean ok = grammar.theLLkAnalyzer.deterministic(rblk);

			CppBlockFinishingInfo howToFinish = genCommonBlock(rblk, false);
			genBlockFinish(howToFinish, throwNoViable);
		}

		// Generate catch phrase for error handling
		if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
			// Close the try block
			tabs--;
			println("}");
		}

		// Generate user-defined or default catch phrases
		if (unlabeledUserSpec != null) {
			genErrorHandler(unlabeledUserSpec);
		}
		else if (rblk.getDefaultErrorHandler()) {
			// Generate default catch phrase
			println("catch (" + exceptionThrown + "& ex) {");
			tabs++;
			// Generate code to handle error if not guessing
			if (grammar.hasSyntacticPredicate) {
				println("if( inputState->guessing == 0 ) {");
				tabs++;
			}
			println("reportError(ex);");
			if ( !(grammar instanceof TreeWalkerGrammar) ) {
				// Generate code to consume until token in k==1 follow set
				Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(1, rblk.endNode);
				String followSetName = getBitsetName(markBitsetForGen(follow.fset));
				println("consume();");
				println("consumeUntil(" + followSetName + ");");
			} else {
				// Just consume one token
				println("if ( _t != "+labeledElementASTInit+" )");
				tabs++;
				println("_t = _t->getNextSibling();");
				tabs--;
			}
			if (grammar.hasSyntacticPredicate) {
				tabs--;
				// When guessing, rethrow exception
				println("} else {");
				tabs++;
				println("throw ex;");
				tabs--;
				println("}");
			}
			// Close catch phrase
			tabs--;
			println("}");
		}

		// Squirrel away the AST "return" value
		if (grammar.buildAST) {
			println("returnAST = " + s.getId() + "_AST;");
		}

		// Set return tree value for tree walkers
		if ( grammar instanceof TreeWalkerGrammar ) {
			println("_retTree = _t;");
		}

		// Generate literals test for lexer rules so marked
		if (rblk.getTestLiterals()) {
			if ( s.access.equals("protected") ) {
				genLiteralsTestForPartialToken();
			}
			else {
				genLiteralsTest();
			}
		}

		// if doing a lexer rule, dump code to create token if necessary
		if ( grammar instanceof LexerGrammar ) {
			println("if ( _createToken && _token=="+namespaceAntlr+"nullToken && _ttype!="+namespaceAntlr+"Token::SKIP ) {");
			println("   _token = makeToken(_ttype);");
			println("   _token->setText(text.substr(_begin, text.length()-_begin));");
			println("}");
			println("_returnToken = _token;");
			// It should be easy for an optimizing compiler to realize this does nothing
			// but it avoids the warning about the variable being unused.
			println("_saveIndex=0;");
		}

		// Gen the return statement if there is one (lexer has hard-wired return action)
		if (rblk.returnAction != null) {
			println("return " + extractIdOfAction(rblk.returnAction, rblk.getLine()) + ";");
		}

//		if ( grammar.debuggingOutput || grammar.traceRules) {
////			tabs--;
////			println("} finally { // debugging");
////			tabs++;
//
//			// Generate trace code if desired
//			if ( grammar.debuggingOutput)
//				if (grammar instanceof ParserGrammar)
//					println("fireExitRule(" + ruleNum + ",0);");
//				else if (grammar instanceof LexerGrammar)
//					println("fireExitRule(" + ruleNum + ",_ttype);");
//
////			if (grammar.traceRules) {
////				if ( grammar instanceof TreeWalkerGrammar ) {
////					println("traceOut(\""+ s.getId() +"\",_t);");
////				}
////				else {
////					println("traceOut(\""+ s.getId() +"\");");
////				}
////			}
////
////			tabs--;
////			println("}");
//		}

		tabs--;
		println("}");
		println("");

		// Restore the AST generation state
		genAST = savegenAST;

		// restore char save state
		// saveText = oldsaveTest;
	}
	public void genRuleHeader(RuleSymbol s, boolean startSymbol) {
		tabs=1;
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRuleHeader("+ s.getId() +")");
		if ( !s.isDefined() ) {
			tool.error("undefined rule: "+ s.getId());
			return;
		}

		// Generate rule return type, name, arguments
		RuleBlock rblk = s.getBlock();
		currentRule = rblk;
		currentASTResult = s.getId();

		// Save the AST generation state, and set it to that of the rule
		boolean savegenAST = genAST;
		genAST = genAST && rblk.getAutoGen();

		// boolean oldsaveTest = saveText;
		saveText = rblk.getAutoGen();

		// Gen method access
		print(s.access + ": ");

		// Gen method return type (note lexer return action set at rule creation)
		if (rblk.returnAction != null)
		{
			// Has specified return value
			_print(extractTypeOfAction(rblk.returnAction, rblk.getLine()) + " ");
		} else {
			// No specified return value
			_print("void ");
		}

		// Gen method name
		_print(s.getId() + "(");

		// Additional rule parameters common to all rules for this grammar
		_print(commonExtraParams);
		if (commonExtraParams.length() != 0 && rblk.argAction != null ) {
			_print(",");
		}

		// Gen arguments
		if (rblk.argAction != null)
		{
			// Has specified arguments
			_println("");
			tabs++;
			println(rblk.argAction);
			tabs--;
			print(")");
		} else {
			// No specified arguments
			_print(")");
		}
		_println(";");

		tabs--;

		// Restore the AST generation state
		genAST = savegenAST;

		// restore char save state
		// saveText = oldsaveTest;
	}
	private void GenRuleInvocation(RuleRefElement rr) {
		// dump rule name
		_print(rr.targetRule + "(");

		// lexers must tell rule if it should set _returnToken
		if ( grammar instanceof LexerGrammar ) {
			// if labeled, could access Token, so tell rule to create
			if ( rr.getLabel() != null ) {
				_print("true");
			}
			else {
				_print("false");
			}
			if (commonExtraArgs.length() != 0 || rr.args!=null ) {
				_print(",");
			}
		}

		// Extra arguments common to all rules for this grammar
		_print(commonExtraArgs);
		if (commonExtraArgs.length() != 0 && rr.args!=null ) {
			_print(",");
		}

		// Process arguments to method, if any
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
		if (rr.args != null)
		{
			// When not guessing, execute user arg action
			ActionTransInfo tInfo = new ActionTransInfo();
			String args = processActionForTreeSpecifiers(rr.args, 0, currentRule, tInfo);
			if ( tInfo.assignToRoot || tInfo.refRuleRoot!=null ) {
				tool.error("Arguments of rule reference '" + rr.targetRule + "' cannot set or ref #"+
					currentRule.getRuleName()+" on line "+rr.getLine());
			}
			_print(args);

			// Warn if the rule accepts no arguments
			if (rs.block.argAction == null)
			{
				tool.warning("Rule '" + rr.targetRule + "' accepts no arguments",
					grammar.getFilename(),
					rr.getLine());
			}
		} else {
			// For C++, no warning if rule has parameters, because there may be default
			// values for all of the parameters
			//if (rs.block.argAction != null) {
			//	tool.warning("Missing parameters on reference to rule "+rr.targetRule, rr.getLine());
			//}
		}
		_println(");");

		// move down to the first child while parsing
		if ( grammar instanceof TreeWalkerGrammar ) {
			println("_t = _retTree;");
		}
	}
	protected void genSemPred(String pred, int line) {
		// translate $ and # references
		ActionTransInfo tInfo = new ActionTransInfo();
		pred = processActionForTreeSpecifiers(pred, line, currentRule, tInfo);
		// ignore translation info...we don't need to do anything with it.
		String escapedPred = charFormatter.escapeString(pred);

		// if debugging, wrap the semantic predicate evaluation in a method
		// that can tell SemanticPredicateListeners the result
		if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) ||
			  (grammar instanceof LexerGrammar)))
			pred = "fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.VALIDATING," //FIXME
				+ addSemPred(escapedPred) + "," + pred + ")";
		println("if (!(" + pred + "))");
		tabs++;
		println("throw "+namespaceAntlr+"SemanticException(\"" + escapedPred + "\");");
		tabs--;
	}
	/** Write an array of Strings which are the semantic predicate
	 *  expressions.  The debugger will reference them by number only
	 */
	protected void genSemPredMap(String prefix) {
		Enumeration e = semPreds.elements();
		println("const char* " + prefix + "_semPredNames[] = {");
		tabs++;
		while(e.hasMoreElements())
			println("\""+e.nextElement()+"\",");
		println("0");
		tabs--;
		println("};");
	}
	protected void genSynPred(SynPredBlock blk, String lookaheadExpr) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen=>("+blk+")");

		// Dump synpred result variable
		println("bool synPredMatched" + blk.ID + " = false;");
		// Gen normal lookahead test
		println("if (" + lookaheadExpr + ") {");
		tabs++;

		// Save input state
		if ( grammar instanceof TreeWalkerGrammar ) {
			println(labeledElementType + " __t" + blk.ID + " = _t;");
		}
		else {
			println("int _m" + blk.ID + " = mark();");
		}

		// Once inside the try, assume synpred works unless exception caught
		println("synPredMatched" + blk.ID + " = true;");
		println("inputState->guessing++;");

		// if debugging, tell listeners that a synpred has started
		if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) ||
			 (grammar instanceof LexerGrammar))) {
			println("fireSyntacticPredicateStarted();");
		}

		syntacticPredLevel++;
		println("try {");
		tabs++;
		gen((AlternativeBlock)blk);		// gen code to test predicate
		tabs--;
		//println("System.out.println(\"pred "+blk+" succeeded\");");
		println("}");
		println("catch (" + exceptionThrown + "& pe) {");
		tabs++;
		println("synPredMatched"+blk.ID+" = false;");
		//println("System.out.println(\"pred "+blk+" failed\");");
		tabs--;
		println("}");

		// Restore input state
		if ( grammar instanceof TreeWalkerGrammar ) {
			println("_t = __t"+blk.ID+";");
		}
		else {
			println("rewind(_m"+blk.ID+");");
		}

		println("inputState->guessing--;");

		// if debugging, tell listeners how the synpred turned out
		if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) ||
		     (grammar instanceof LexerGrammar))) {
			println("if (synPredMatched" + blk.ID +")");
			println("  fireSyntacticPredicateSucceeded();");
			println("else");
			println("  fireSyntacticPredicateFailed();");
		}

		syntacticPredLevel--;
		tabs--;

		// Close lookahead test
		println("}");

		// Test synpred result
		println("if ( synPredMatched"+blk.ID+" ) {");
	}
	/** Generate a static array containing the names of the tokens,
	 * indexed by the token type values.  This static array is used
	 * to format error messages so that the token identifers or literal
	 * strings are displayed instead of the token numbers.
	 *
	 * If a lexical rule has a paraphrase, use it rather than the
	 * token label.
	 */
	public void genTokenStrings(String prefix) {
		// Generate a string for each token.  This creates a static
		// array of Strings indexed by token type.
//		println("");
		println("const char* " + prefix + "_tokenNames[] = {");
		tabs++;

		// Walk the token vocabulary and generate a Vector of strings
		// from the tokens.
		Vector v = grammar.tokenManager.getVocabulary();
		for (int i = 0; i < v.size(); i++)
		{
			String s = (String)v.elementAt(i);
			if (s == null)
			{
				s = "<"+String.valueOf(i)+">";
			}
			if ( !s.startsWith("\"") && !s.startsWith("<") ) {
				TokenSymbol ts = (TokenSymbol)grammar.tokenManager.getTokenSymbol(s);
				if ( ts!=null && ts.getParaphrase()!=null ) {
					s = antlr.Tool.stripFrontBack(ts.getParaphrase(), "\"", "\"");
				}
			}
			print(charFormatter.literalString(s));
			_println(",");
		}
		println("0");

		// Close the string array initailizer
		tabs--;
		println("};");
	}
	/** Generate the token types C++ file */
	protected void genTokenTypes(TokenManager tm) throws IOException {
		// Open the token output header file and set the currentOutput stream
		outputFile = tm.getName() + TokenTypesFileSuffix+".hpp";
		outputLine = 1;
		currentOutput = antlr.Tool.openOutputFile(outputFile);
		//SAS: changed for proper text file io

		tabs = 0;

		// Generate a guard wrapper
		println("#ifndef INC_"+tm.getName()+TokenTypesFileSuffix+"_hpp_");
		println("#define INC_"+tm.getName()+TokenTypesFileSuffix+"_hpp_");
		println("");

		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);

		// Generate the header common to all C++ files
		genHeader(outputFile);

		// Encapsulate the definitions in an interface.  This can be done
		// because they are all constants.
		println("struct " + tm.getName() + TokenTypesFileSuffix+" {");
		tabs++;
		println("enum {");
		tabs++;

		// Generate a definition for each token type
		Vector v = tm.getVocabulary();

		// Do special tokens manually
		println("EOF_ = " + Token.EOF_TYPE + ",");

		// Move the other special token to the end, so we can solve
		// the superfluous comma problem easily

		for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
			String s = (String)v.elementAt(i);
			if (s != null) {
				if ( s.startsWith("\"") ) {
					// a string literal
					StringLiteralSymbol sl = (StringLiteralSymbol)tm.getTokenSymbol(s);
					if ( sl==null ) {
						antlr.Tool.panic("String literal "+s+" not in symbol table");
					}
					else if ( sl.label != null ) {
						println(sl.label + " = " + i + ",");
					}
					else {
						String mangledName = mangleLiteral(s);
						if (mangledName != null) {
							// We were able to create a meaningful mangled token name
							println(mangledName + " = " + i + ",");
							// if no label specified, make the label equal to the mangled name
							sl.label = mangledName;
						}
						else {
							println("// " + s + " = " + i);
						}
					}
				}
				else if ( !s.startsWith("<") ) {
					println(s + " = " + i + ",");
				}
			}
		}

		// Moved from above
		println("NULL_TREE_LOOKAHEAD = " + Token.NULL_TREE_LOOKAHEAD);

		// Close the enum
		tabs--;
		println("};");

		// Close the interface
		tabs--;
		println("};");

		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Generate a guard wrapper
		println("#endif /*INC_"+tm.getName()+TokenTypesFileSuffix+"_hpp_*/");

		// Close the tokens output file
		currentOutput.close();
		currentOutput = null;
		exitIfError();
	}
	/** Process a string for an simple expression for use in xx/action.g
	 * it is used to cast simple tokens/references to the right type for
	 * the generated language. Basically called for every element in
	 * the vector to getASTCreateString(vector V)
	 * @param str A String.
	 */
	public String processStringForASTConstructor( String str )
	{
		if( usingCustomAST &&
			((grammar instanceof TreeWalkerGrammar) ||
			 (grammar instanceof ParserGrammar))  &&
			!(grammar.tokenManager.tokenDefined(str) ) )
		{
//			System.out.println("processStringForASTConstructor: "+str+" with cast");
			return "static_cast<"+namespaceAntlr+"RefAST>("+str+")";
		}
		else
		{
//			System.out.println("processStringForASTConstructor: "+str);
			return str;
		}
	}
	/** Get a string for an expression to generate creation of an AST subtree.
	  * @param v A Vector of String, where each element is an expression in the target language yielding an AST node.
	  */
	public String getASTCreateString(Vector v) {
		if (v.size() == 0) {
			return "";
		}
		StringBuffer buf = new StringBuffer();
		// the labeledElementASTType here can probably be a cast or nothing
		// in the case of ! usingCustomAST
		buf.append(labeledElementASTType+
					"(astFactory.make((new "+namespaceAntlr+
					  "ASTArray("+v.size()+"))");
		for (int i = 0; i < v.size(); i++) {
			buf.append("->add("+ v.elementAt(i) + ")");
		}
		buf.append("))");
		return buf.toString();
	}
	/** Get a string for an expression to generate creating of an AST node
	  * @param str The arguments to the AST constructor
	  */
	public String getASTCreateString(GrammarAtom atom, String str) {
		if ( atom!=null && atom.getASTNodeType() != null ) {
			// RK: I guess this is heterogeneous ast stuff...
			return "Ref"+atom.getASTNodeType()+"(new "+atom.getASTNodeType()+"("+str+"))";
		}
		else
		{
			// This is *SO* ugly... but it will have to do for now... 2.7.2 will
			// have better.
			boolean is_constructor = false;
			if( str.indexOf(',') != -1 )
				is_constructor = grammar.tokenManager.tokenDefined(str.substring(0,str.indexOf(',')));

//			System.out.println("getAstCreateString(as): "+str+" "+grammar.tokenManager.tokenDefined(str));
			if( usingCustomAST &&
			   (grammar instanceof TreeWalkerGrammar) &&
				!(grammar.tokenManager.tokenDefined(str) ) &&
				! is_constructor )
				return "astFactory.create(static_cast<"+namespaceAntlr+"RefAST>("+str+"))";
			else
				return "astFactory.create("+str+")";
		}
	}

	/** Get a string for an expression to generate creating of an AST node
	 * @param str The arguments to the AST constructor
	 */
	public String getASTCreateString(String str) {
//		System.out.println("getAstCreateString(str): "+str+" "+grammar.tokenManager.tokenDefined(str));
		if( usingCustomAST )
			return "static_cast<"+labeledElementASTType+">(astFactory.create(static_cast<"+namespaceAntlr+"RefAST>( /*ss*/ "+str+")))";
		else
			return "astFactory.create("+str+")";
	}

	protected String getLookaheadTestExpression(Lookahead[] look, int k) {
		StringBuffer e = new StringBuffer(100);
		boolean first = true;

		e.append("(");
		for (int i = 1; i <= k; i++) {
			BitSet p = look[i].fset;
			if (!first) {
				e.append(") && (");
			}
			first = false;

			// Syn preds can yield <end-of-syn-pred> (epsilon) lookahead.
			// There is no way to predict what that token would be.  Just
			// allow anything instead.
			if (look[i].containsEpsilon()) {
				e.append("true");
			} else {
				e.append(getLookaheadTestTerm(i, p));
			}
		}
		e.append(")");

		return e.toString();
	}
	/** Generate a lookahead test expression for an alternate.  This
	 * will be a series of tests joined by '&&' and enclosed by '()',
	 * the number of such tests being determined by the depth of the lookahead.
	 */
	protected String getLookaheadTestExpression(Alternative alt, int maxDepth) {
		int depth = alt.lookaheadDepth;
		if ( depth == GrammarAnalyzer.NONDETERMINISTIC ) {
			// if the decision is nondeterministic, do the best we can: LL(k)
			// any predicates that are around will be generated later.
			depth = grammar.maxk;
		}

		if ( maxDepth==0 ) {
			// empty lookahead can result from alt with sem pred
			// that can see end of token.  E.g., A : {pred}? ('a')? ;
			return "true";
		}

/*
boolean first = true;
		for (int i=1; i<=depth && i<=maxDepth; i++) {
			BitSet p = alt.cache[i].fset;
			if (!first) {
				e.append(") && (");
			}
			first = false;

			// Syn preds can yield <end-of-syn-pred> (epsilon) lookahead.
			// There is no way to predict what that token would be.  Just
			// allow anything instead.
			if ( alt.cache[i].containsEpsilon() ) {
				e.append("true");
			}
			else {
				e.append(getLookaheadTestTerm(i, p));
			}
		}

		e.append(")");
*/

		return "(" + getLookaheadTestExpression(alt.cache,depth) + ")";
	}
	/**Generate a depth==1 lookahead test expression given the BitSet.
	 * This may be one of:
	 * 1) a series of 'x==X||' tests
	 * 2) a range test using >= && <= where possible,
	 * 3) a bitset membership test for complex comparisons
	 * @param k The lookahead level
	 * @param p The lookahead set for level k
	 */
	protected String getLookaheadTestTerm(int k, BitSet p) {
		// Determine the name of the item to be compared
		String ts = lookaheadString(k);

		// Generate a range expression if possible
		int[] elems = p.toArray();
		if (elementsAreRange(elems)) {
			return getRangeExpression(k, elems);
		}

		// Generate a bitset membership test if possible
		StringBuffer e;
		int degree = p.degree();
		if ( degree == 0 ) {
			return "true";
		}

		if (degree >= bitsetTestThreshold) {
			int bitsetIdx = markBitsetForGen(p);
			return getBitsetName(bitsetIdx) + ".member(" + ts + ")";
		}

		// Otherwise, generate the long-winded series of "x==X||" tests
		e = new StringBuffer();
		for (int i = 0; i < elems.length; i++) {
			// Get the compared-to item (token or character value)
			String cs = getValueString(elems[i]);

			// Generate the element comparison
			if ( i>0 ) e.append("||");
			e.append(ts);
			e.append("==");
			e.append(cs);
		}
		return e.toString();
	}
	/** Return an expression for testing a contiguous renage of elements
	 * @param k The lookahead level
	 * @param elems The elements representing the set, usually from BitSet.toArray().
	 * @return String containing test expression.
	 */
	public String getRangeExpression(int k, int[] elems) {
		if (!elementsAreRange(elems)) {
			tool.panic("getRangeExpression called with non-range");
		}
		int begin = elems[0];
		int end = elems[elems.length-1];
		return
			"(" + lookaheadString(k) + " >= " + getValueString(begin) + " && " +
			lookaheadString(k) + " <= " + getValueString(end) + ")";
	}
	/** getValueString: get a string representation of a token or char value
	 * @param value The token or char value
	 */
	private String getValueString(int value) {
		String cs;
		if ( grammar instanceof LexerGrammar ) {
			cs = charFormatter.literalChar(value);
		}
		else {
			TokenSymbol ts = grammar.tokenManager.getTokenSymbolAt(value);
			if ( ts == null ) {
				return ""+value; // return token type as string
				// tool.panic("vocabulary for token type " + value + " is null");
			}
			String tId = ts.getId();
			if ( ts instanceof StringLiteralSymbol ) {
				// if string literal, use predefined label if any
				// if no predefined, try to mangle into LITERAL_xxx.
				// if can't mangle, use int value as last resort
				StringLiteralSymbol sl = (StringLiteralSymbol)ts;
				String label = sl.getLabel();
				if ( label!=null ) {
					cs = label;
				}
				else {
					cs = mangleLiteral(tId);
					if (cs == null) {
						cs = String.valueOf(value);
					}
				}
			}
			else {
				if ( tId.equals("EOF") )
					cs = namespaceAntlr+"Token::EOF_TYPE";
				else
					cs = tId;
			}
		}
		return cs;
	}
	/**Is the lookahead for this alt empty? */
	protected boolean lookaheadIsEmpty(Alternative alt, int maxDepth) {
		int depth = alt.lookaheadDepth;
		if ( depth == GrammarAnalyzer.NONDETERMINISTIC ) {
			depth = grammar.maxk;
		}
		for (int i=1; i<=depth && i<=maxDepth; i++) {
			BitSet p = alt.cache[i].fset;
			if (p.degree() != 0) {
				return false;
			}
		}
		return true;
	}
	private String lookaheadString(int k) {
		if (grammar instanceof TreeWalkerGrammar) {
			return "_t->getType()";
		}
		return "LA(" + k + ")";
	}
	/** Mangle a string literal into a meaningful token name.  This is
	  * only possible for literals that are all characters.  The resulting
	  * mangled literal name is literalsPrefix with the text of the literal
	  * appended.
	  * @return A string representing the mangled literal, or null if not possible.
	  */
	private String mangleLiteral(String s) {
		String mangled = antlr.Tool.literalsPrefix;
		for (int i = 1; i < s.length()-1; i++) {
			if (!Character.isLetter(s.charAt(i)) &&
				 s.charAt(i) != '_') {
				return null;
			}
			mangled += s.charAt(i);
		}
		if ( antlr.Tool.upperCaseMangledLiterals ) {
			mangled = mangled.toUpperCase();
		}
		return mangled;
	}
	/** Map an identifier to it's corresponding tree-node variable.
	  * This is context-sensitive, depending on the rule and alternative
	  * being generated
	  * @param idParam The identifier name to map
	  * @return The mapped id (which may be the same as the input), or null if the mapping is invalid due to duplicates
	  */
	public String mapTreeId(String idParam, ActionTransInfo transInfo) {
		// if not in an action of a rule, nothing to map.
		if ( currentRule==null ) return idParam;
//		System.out.print("mapTreeId: "+idParam+" ");

		boolean in_var = false;
		String id = idParam;
		if (grammar instanceof TreeWalkerGrammar) {
			if ( !grammar.buildAST ) {
				in_var = true;
//				System.out.println("in_var1");
			}
			// If the id ends with "_in", then map it to the input variable
			else if (id.length() > 3 && id.lastIndexOf("_in") == id.length()-3) {
				// Strip off the "_in"
				id = id.substring(0, id.length()-3);
				in_var = true;
//				System.out.println("in_var2");
			}
		}
//		System.out.print(in_var+"\t");

		// Check the rule labels.  If id is a label, then the output
		// variable is label_AST, and the input variable is plain label.
		for (int i = 0; i < currentRule.labeledElements.size(); i++) {
			AlternativeElement elt = (AlternativeElement)currentRule.labeledElements.elementAt(i);
			if (elt.getLabel().equals(id)) {
//				if( in_var )
//					System.out.println("returning (vec) "+(in_var ? id : id + "_AST"));
				return in_var ? id : id + "_AST";
			}
		}

		// Failing that, check the id-to-variable map for the alternative.
		// If the id is in the map, then output variable is the name in the
		// map, and input variable is name_in
		String s = (String)treeVariableMap.get(id);
		if (s != null) {
			if (s == NONUNIQUE) {
//				if( in_var )
//					System.out.println("returning null (nonunique)");
				// There is more than one element with this id
				return null;
			} else if (s.equals(currentRule.getRuleName())) {
				// a recursive call to the enclosing rule is
				// ambiguous with the rule itself.
//				if( in_var )
//					System.out.println("returning null (rulename)");
				return null;
			} else {
//				if( in_var )
//				System.out.println("returning "+(in_var?s+"_in":s));
				return in_var ? s + "_in" : s;
			}
		}

		// Failing that, check the rule name itself.  Output variable
		// is rule_AST; input variable is rule_AST_in (treeparsers).
		if (id.equals(currentRule.getRuleName())) {
			String r = in_var ? id + "_AST_in" : id + "_AST";
			if ( transInfo!=null ) {
				if ( !in_var ) {
					transInfo.refRuleRoot = r;
				}
			}
//			if( in_var )
//				System.out.println("returning (r) "+r);
			return r;
		} else {
//			if( in_var )
//			System.out.println("returning (last) "+id);
			// id does not map to anything -- return itself.
			return id;
		}
	}
	/** Given an element and the name of an associated AST variable,
	  * create a mapping between the element "name" and the variable name.
	  */
	private void mapTreeVariable(AlternativeElement e, String name)
	{
		// For tree elements, defer to the root
		if (e instanceof TreeElement) {
			mapTreeVariable( ((TreeElement)e).root, name);
			return;
		}

		// Determine the name of the element, if any, for mapping purposes
		String elName = null;

		// Don't map labeled items
		if (e.getLabel() == null) {
			if (e instanceof TokenRefElement) {
				// use the token id
				elName = ((TokenRefElement)e).atomText;
			}
			else if (e instanceof RuleRefElement) {
				// use the rule name
				elName = ((RuleRefElement)e).targetRule;
			}
		}
		// Add the element to the tree variable map if it has a name
		if (elName != null) {
			if (treeVariableMap.get(elName) != null) {
				// Name is already in the map -- mark it as duplicate
				treeVariableMap.remove(elName);
				treeVariableMap.put(elName, NONUNIQUE);
			}
			else {
				treeVariableMap.put(elName, name);
			}
		}
	}

	/** Lexically process tree-specifiers in the action.
	 *  This will replace #id and #(...) with the appropriate
	 *  function calls and/or variables.
	 */
	protected String processActionForTreeSpecifiers(String actionStr, int line, RuleBlock currentRule, ActionTransInfo tInfo) {
		if ( actionStr==null || actionStr.length()==0 )
			return null;

		// The action trans info tells us (at the moment) whether an
		// assignment was done to the rule's tree root.
		if (grammar==null)
			return actionStr;

		if ( (grammar.buildAST && actionStr.indexOf('#') != -1) ||
			 grammar instanceof TreeWalkerGrammar ||
			 (grammar instanceof LexerGrammar && actionStr.indexOf('$') != -1) ) {
			// Create a lexer to read an action and return the translated version
			antlr.actions.cpp.ActionLexer lexer =
				new antlr.actions.cpp.ActionLexer(actionStr, currentRule,
															 this, tInfo);
			lexer.setLineOffset(line);
			lexer.setTool(tool);

			try {
				lexer.mACTION(true);
				actionStr = lexer.getTokenObject().getText();
				// System.out.println("action translated: "+actionStr);
				// System.out.println("trans info is "+tInfo);
			}
			catch (RecognitionException ex) {
				lexer.reportError(ex);
				return actionStr;
			}
			catch (TokenStreamException tex) {
				antlr.Tool.panic("Error reading action:"+actionStr);
				return actionStr;
			}
			catch (CharStreamException io) {
				antlr.Tool.panic("Error reading action:"+actionStr);
				return actionStr;
			}
		}
		return actionStr;
	}

	private String fixNameSpaceOption( String ns )
	{
		ns = Tool.stripFrontBack(ns,"\"","\"");
		if( ns.length() > 2 &&
			 !ns.substring(ns.length()-2, ns.length()).equals("::") )
		ns += "::";
		return ns;
	}

	private void setupGrammarParameters(Grammar g) {
		if (g instanceof ParserGrammar ||
			 g instanceof LexerGrammar  ||
			 g instanceof TreeWalkerGrammar
			)
		{
			/* RK: options also have to be added to Grammar.java and for options
			 * on the file level entries have to be defined in
			 * DefineGrammarSymbols.java and passed around via 'globals' in Tool.java
			 */
			if( Tool.nameSpace != null )
				nameSpace = Tool.nameSpace;

			if( Tool.namespaceStd != null )
				namespaceStd = fixNameSpaceOption(Tool.namespaceStd);

			if( Tool.namespaceAntlr != null )
				namespaceAntlr = fixNameSpaceOption(Tool.namespaceAntlr);

			genHashLines = Tool.genHashLines;

			/* let grammar level options override filelevel ones...
			 */
			if( g.hasOption("namespace") ) {
				Token t = g.getOption("namespace");
				if( t != null ) {
					nameSpace = new NameSpace(t.getText());
				}
			}
			if( g.hasOption("namespaceAntlr") ) {
				Token t = g.getOption("namespaceAntlr");
				if( t != null ) {
					String ns = Tool.stripFrontBack(t.getText(),"\"","\"");
					if ( ns != null ) {
						if( ns.length() > 2 &&
							 !ns.substring(ns.length()-2, ns.length()).equals("::") )
							ns += "::";
						namespaceAntlr = ns;
					}
				}
			}
			if( g.hasOption("namespaceStd") ) {
				Token t = g.getOption("namespaceStd");
				if( t != null ) {
					String ns = Tool.stripFrontBack(t.getText(),"\"","\"");
					if ( ns != null ) {
						if( ns.length() > 2 &&
							 !ns.substring(ns.length()-2, ns.length()).equals("::") )
							ns += "::";
						namespaceStd = ns;
					}
				}
			}
			if( g.hasOption("genHashLines") ) {
				Token t = g.getOption("genHashLines");
				if( t != null ) {
					String val = Tool.stripFrontBack(t.getText(),"\"","\"");
					genHashLines = val.equals("true");
				}
			}
		}
		if (g instanceof ParserGrammar) {
			labeledElementASTType = namespaceAntlr+"RefAST";
			labeledElementASTInit = namespaceAntlr+"nullAST";
			if ( g.hasOption("ASTLabelType") ) {
				Token tsuffix = g.getOption("ASTLabelType");
				if ( tsuffix != null ) {
					String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
					if ( suffix != null ) {
						usingCustomAST = true;
						labeledElementASTType = suffix;
						labeledElementASTInit = "static_cast<"+suffix+">("+namespaceAntlr+"nullAST)";
					}
				}
			}
			labeledElementType = namespaceAntlr+"RefToken ";
			labeledElementInit = namespaceAntlr+"nullToken";
			commonExtraArgs = "";
			commonExtraParams = "";
			commonLocalVars = "";
			lt1Value = "LT(1)";
			exceptionThrown = namespaceAntlr+"RecognitionException";
			throwNoViable = "throw "+namespaceAntlr+"NoViableAltException(LT(1), getFilename());";
		}
		else if (g instanceof LexerGrammar) {
			labeledElementType = "char ";
			labeledElementInit = "'\\0'";
			commonExtraArgs = "";
			commonExtraParams = "bool _createToken";
			commonLocalVars = "int _ttype; "+namespaceAntlr+"RefToken _token; int _begin=text.length();";
			lt1Value = "LA(1)";
			exceptionThrown = namespaceAntlr+"RecognitionException";
			throwNoViable = "throw "+namespaceAntlr+"NoViableAltForCharException(LA(1), getFilename(), getLine());";
		}
		else if (g instanceof TreeWalkerGrammar) {
			labeledElementInit = namespaceAntlr+"nullAST";
			labeledElementASTInit = namespaceAntlr+"nullAST";
			labeledElementASTType = namespaceAntlr+"RefAST";
			labeledElementType = namespaceAntlr+"RefAST";
			commonExtraParams = namespaceAntlr+"RefAST _t";
			throwNoViable = "throw "+namespaceAntlr+"NoViableAltException(_t);";
			lt1Value = "_t";
			if ( g.hasOption("ASTLabelType") ) {
				Token tsuffix = g.getOption("ASTLabelType");
				if ( tsuffix != null ) {
					String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
					if ( suffix != null ) {
						usingCustomAST = true;
						labeledElementASTType = suffix;
						labeledElementType = suffix;
						labeledElementInit = "static_cast<"+suffix+">("+namespaceAntlr+"nullAST)";
						labeledElementASTInit = labeledElementInit;
						commonExtraParams = suffix+" _t";
						throwNoViable = "throw "+namespaceAntlr+"NoViableAltException(static_cast<"+namespaceAntlr+"RefAST>(_t));";
						lt1Value = "_t";
					}
				}
			}
			if ( !g.hasOption("ASTLabelType") ) {
				g.setOption("ASTLabelType", new Token(ANTLRTokenTypes.STRING_LITERAL,namespaceAntlr+"RefAST"));
			}
			commonExtraArgs = "_t";
			commonLocalVars = "";
			exceptionThrown = namespaceAntlr+"RecognitionException";
		}
		else {
			tool.panic("Unknown grammar type");
		}
	}
	private String textOrChar(String text) {
		// check to see if the text is a single character
		if (text.startsWith("'")) {
			// assume it also ends with '
			return charFormatter.literalChar(ANTLRLexer.tokenTypeForCharLiteral(text));
		}
		else
			return text;
	}
}

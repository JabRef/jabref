package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

//
// ANTLR C# Code Generator by Micheal Jordan
//                            Kunle Odutola       : kunle UNDERSCORE odutola AT hotmail DOT com
//                            Anthony Oguntimehin
//
// With many thanks to Eric V. Smith from the ANTLR list.
//

// HISTORY:
//
// 17-May-2002 kunle    Fixed bug in OctalToUnicode() - was processing non-Octal escape sequences
//                      Also added namespace support based on Cpp version.
// 07-Jun-2002 kunle    Added Scott Ellis's _saveIndex creation optimizations
// 09-Sep-2002 richardN Richard Ney's bug-fix for literals table construction.
//                      [ Hashtable ctor needed instance of hash code provider not it's class name. ]
// 17-Sep-2002 kunle &  Added all Token ID definitions as data member of every Lexer/Parser/TreeParser
//             AOg      [ A by-product of problem-solving phase of the hetero-AST changes below
//                        but, it breaks nothing and restores "normal" ANTLR codegen behaviour. ]
// 19-Oct-2002 kunle &  Completed the work required to support heterogenous ASTs (many changes)
//             AOg   &
//             michealj
// 14-Nov-2002 michealj Added "initializeASTFactory()" to support flexible ASTFactory initialization.
//						[ Thanks to Ric Klaren - for suggesting it and implementing it for Cpp. ]
// 18-Nov-2002 kunle    Added fix to make xx_tokenSet_xx names CLS compliant.
// 01-Dec-2002 richardN Patch to reduce "unreachable code" warnings
// 01-Dec-2002 richardN Fix to generate correct TreeParser token-type classnames.
// 12-Jan-2003 kunle  & Generated Lexers, Parsers and TreeParsers now support ANTLR's tracing option.
//             michealj
// 12-Jan-2003 kunle    Fixed issue where initializeASTFactory() was generated when "buildAST=false"
// 14-Jan-2003 AOg      initializeASTFactory(AST factory) method was modifying the Parser's "astFactory"
//                      member rather than it's own "factory" parameter. Fixed.
// 18-Jan-2003 kunle  & Fixed reported issues with ASTFactory create() calls for hetero ASTs
//             michealj - code generated for LEXER token with hetero-AST option specified does not compile
//                      - code generated for imaginary tokens with hetero-AST option specified uses 
//                        default AST type
//                      - code generated for per-TokenRef hetero-AST option specified does not compile
// 18-Jan-2003 kunle    initializeASTFactory(AST) method is now a static public member
// 18-May-2003 kunle    Changes to address outstanding reported issues::
//                      - Fixed reported issues with support for case-sensitive literals
//                      - antlr.SemanticException now imported for all Lexers.
//                        [ This exception is thrown on predicate failure. ]
// 12-Jan-2004 kunle    Added fix for reported issue with un-compileable generated lexers
//
//

import java.util.Enumeration;
import java.util.Hashtable;
import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
import java.io.PrintWriter; //SAS: changed for proper text file io
import java.io.IOException;
import java.io.FileWriter;

/** Generates MyParser.cs, MyLexer.cs and MyParserTokenTypes.cs */
public class CSharpCodeGenerator extends CodeGenerator {
    // non-zero if inside syntactic predicate generation
    protected int syntacticPredLevel = 0;

	// Are we generating ASTs (for parsers and tree parsers) right now?
	protected boolean genAST = false;

    // Are we saving the text consumed (for lexers) right now?
    protected boolean saveText = false;

    // Grammar parameters set up to handle different grammar classes.
    // These are used to get instanceof tests out of code generation
	boolean usingCustomAST = false;
	String labeledElementType;
    String labeledElementASTType;
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

    /** Mapping between the ids used in the current alt, and the
     * names of variables used to represent their AST values.
     */
    Hashtable treeVariableMap = new Hashtable();

    /** Used to keep track of which AST variables have been defined in a rule
     * (except for the #rule_name and #rule_name_in var's
     */
    Hashtable declaredASTVariables = new Hashtable();

    /* Count of unnamed generated variables */
    int astVarNumber = 1;

    /** Special value used to mark duplicate in treeVariableMap */
    protected static final String NONUNIQUE = new String();

    public static final int caseSizeThreshold = 127; // ascii is max

    private Vector semPreds;
	// Used to keep track of which (heterogeneous AST types are used)
	// which need to be set in the ASTFactory of the generated parser
	private java.util.Vector astTypes;

	private static CSharpNameSpace nameSpace = null;

	// _saveIndex creation optimization -- don't create it unless we need to use it
	boolean bSaveIndexCreated = false;


    /** Create a CSharp code-generator using the given Grammar.
     * The caller must still call setTool, setBehavior, and setAnalyzer
     * before generating code.
     */
	public CSharpCodeGenerator() {
		super();
		charFormatter = new CSharpCharFormatter();
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

	public void exitIfError()
	{
		if (antlrTool.hasError())
		{
			antlrTool.fatalError("Exiting due to errors.");
		}
	}

	/**Generate the parser, lexer, treeparser, and token types in CSharp */
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
				// To get right overloading behavior across heterogeneous grammars
				setupGrammarParameters(g);
				g.generate();
				exitIfError();
			}

			// Loop over all token managers (some of which are lexers)
			Enumeration tmIter = behavior.tokenManagers.elements();
			while (tmIter.hasMoreElements()) {
				TokenManager tm = (TokenManager)tmIter.nextElement();
				if (!tm.isReadOnly()) {
					// Write the token manager tokens as CSharp
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
			antlrTool.reportException(e, null);
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
				println("if (0==inputState.guessing)");
				println("{");
				tabs++;
			}

			ActionTransInfo tInfo = new ActionTransInfo();
			String actionStr = processActionForSpecialSymbols(action.actionText,
																			 action.getLine(),
																			 currentRule, tInfo);

			if ( tInfo.refRuleRoot!=null ) {
				// Somebody referenced "#rule", make sure translated var is valid
				// assignment to #rule is left as a ref also, meaning that assignments
				// with no other refs like "#rule = foo();" still forces this code to be
				// generated (unnecessarily).
				println(tInfo.refRuleRoot + " = ("+labeledElementASTType+")currentAST.root;");
			}

			// dump the translated action
			printAction(actionStr);

			if ( tInfo.assignToRoot ) {
				// Somebody did a "#rule=", reset internal currentAST.root
				println("currentAST.root = "+tInfo.refRuleRoot+";");
				// reset the child pointer too to be last sibling in sibling list
				println("if ( (null != "+tInfo.refRuleRoot+") && (null != "+tInfo.refRuleRoot+".getFirstChild()) )");
				tabs++;
				println("currentAST.child = "+tInfo.refRuleRoot+".getFirstChild();");
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
		tabs++;

		genBlockPreamble(blk);
		genBlockInitAction(blk);

		// Tell AST generation to build subrule result
		String saveCurrentASTResult = currentASTResult;
		if (blk.getLabel() != null) {
			currentASTResult = blk.getLabel();
		}

		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

		CSharpBlockFinishingInfo howToFinish = genCommonBlock(blk, true);
		genBlockFinish(howToFinish, throwNoViable);

		tabs--;
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
      boolean flag = ( grammar instanceof LexerGrammar &&
            (!saveText || (r.getAutoGenType() == GrammarElement.AUTO_GEN_BANG)) );
      if (flag)
          println("_saveIndex = text.Length;");

      println("matchRange("+OctalToUnicode(r.beginText)+","+OctalToUnicode(r.endText)+");");

      if (flag)
          println("text.Length = _saveIndex;");
	}
	/** Generate the lexer CSharp file */
	public  void gen(LexerGrammar g) throws IOException {
		// If debugging, create a new sempred vector for this grammar
		if (g.debuggingOutput)
			semPreds = new Vector();

		setGrammar(g);
		if (!(grammar instanceof LexerGrammar)) {
			antlrTool.panic("Internal error generating lexer");
		}
		genBody(g);
	}
	/** Generate code for the given grammar element.
	 * @param blk The (...)+ block to generate
	 */
	public void gen(OneOrMoreBlock blk) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen+("+blk+")");
		String label;
		String cnt;
		println("{ // ( ... )+");
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

		println("for (;;)");
		println("{");
		tabs++;
      // generate the init action for ()+ ()* inside the loop
      // this allows us to do usefull EOF checking...
      genBlockInitAction(blk);

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
			println("if (("+cnt+" >= 1) && "+predictExit+") goto "+label+"_breakloop;");
		}

		CSharpBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
		genBlockFinish(
			howToFinish,
			"if ("+cnt+" >= 1) { goto "+label+"_breakloop; } else { " + throwNoViable + "; }"
			);

		println(cnt+"++;");
		tabs--;
		println("}");
		_print(label + "_breakloop:");
		println(";");
		println("}    // ( ... )+");

		// Restore previous AST generation
		currentASTResult = saveCurrentASTResult;
	}
	/** Generate the parser CSharp file */
	public void gen(ParserGrammar g) throws IOException {

		// if debugging, set up a new vector to keep track of sempred
		//   strings for this grammar
		if (g.debuggingOutput)
			semPreds = new Vector();

		setGrammar(g);
		if (!(grammar instanceof ParserGrammar)) {
			antlrTool.panic("Internal error generating parser");
		}
		genBody(g);
	}
	/** Generate code for the given grammar element.
	 * @param blk The rule-reference to generate
	 */
	public void gen(RuleRefElement rr)
	{
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRR("+rr+")");
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
		if (rs == null || !rs.isDefined())
		{
			// Is this redundant???
			antlrTool.error("Rule '" + rr.targetRule + "' is not defined", grammar.getFilename(), rr.getLine(), rr.getColumn());
			return;
		}
		if (!(rs instanceof RuleSymbol))
		{
			// Is this redundant???
			antlrTool.error("'" + rr.targetRule + "' does not name a grammar rule", grammar.getFilename(), rr.getLine(), rr.getColumn());
			return;
		}

		genErrorTryForElement(rr);

		// AST value for labeled rule refs in tree walker.
		// This is not AST construction;  it is just the input tree node value.
		if ( grammar instanceof TreeWalkerGrammar &&
			rr.getLabel() != null &&
			syntacticPredLevel == 0 )
		{
			println(rr.getLabel() + " = _t==ASTNULL ? null : "+lt1Value+";");
		}

		// if in lexer and ! on rule ref or alt or rule, save buffer index to kill later
        if (grammar instanceof LexerGrammar && (!saveText || rr.getAutoGenType() == GrammarElement.AUTO_GEN_BANG))
		{
			declareSaveIndexVariableIfNeeded();
			println("_saveIndex = text.Length;");
		}

		// Process return value assignment if any
		printTabs();
		if (rr.idAssign != null)
		{
			// Warn if the rule has no return type
			if (rs.block.returnAction == null)
			{
				antlrTool.warning("Rule '" + rr.targetRule + "' has no return type", grammar.getFilename(), rr.getLine(), rr.getColumn());
			}
			_print(rr.idAssign + "=");
		} else {
			// Warn about return value if any, but not inside syntactic predicate
			if ( !(grammar instanceof LexerGrammar) && syntacticPredLevel == 0 && rs.block.returnAction != null)
			{
				antlrTool.warning("Rule '" + rr.targetRule + "' returns a value", grammar.getFilename(), rr.getLine(), rr.getColumn());
			}
		}

		// Call the rule
		GenRuleInvocation(rr);

		// if in lexer and ! on element or alt or rule, save buffer index to kill later
		if ( grammar instanceof LexerGrammar && (!saveText||rr.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			declareSaveIndexVariableIfNeeded();
			println("text.Length = _saveIndex;");
		}

		// if not in a syntactic predicate
		if (syntacticPredLevel == 0)
		{
			boolean doNoGuessTest = (
				grammar.hasSyntacticPredicate &&
				(
				grammar.buildAST && rr.getLabel() != null ||
				(genAST && rr.getAutoGenType() == GrammarElement.AUTO_GEN_NONE)
				)
				);
			if (doNoGuessTest) 	{
				println("if (0 == inputState.guessing)");
				println("{");
				tabs++;
			}

			if (grammar.buildAST && rr.getLabel() != null)
			{
				// always gen variable for rule return on labeled rules
				println(rr.getLabel() + "_AST = ("+labeledElementASTType+")returnAST;");
			}
			if (genAST)
			{
				switch (rr.getAutoGenType())
				{
				case GrammarElement.AUTO_GEN_NONE:
					if( usingCustomAST )
						println("astFactory.addASTChild(currentAST, (AST)returnAST);");
					else
						println("astFactory.addASTChild(currentAST, returnAST);");
					break;
				case GrammarElement.AUTO_GEN_CARET:
					antlrTool.error("Internal: encountered ^ after rule reference");
					break;
				default:
					break;
				}
			}

			// if a lexer and labeled, Token label defined at rule level, just set it here
			if ( grammar instanceof LexerGrammar && rr.getLabel() != null )
			{
				println(rr.getLabel()+" = returnToken_;");
			}

			if (doNoGuessTest)
			{
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
			println("_t = _t.getNextSibling();");
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
		println("matchRange("+OctalToUnicode(r.beginText)+","+OctalToUnicode(r.endText)+");");
		genErrorCatchForElement(r);
	}

	/** Generate code for the given grammar element.
	 * @param blk The token-reference to generate
	 */
	public void gen(TokenRefElement atom) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genTokenRef("+atom+")");
		if ( grammar instanceof LexerGrammar ) {
			antlrTool.panic("Token reference found in lexer");
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
			println("_t = _t.getNextSibling();");
		}
	}

	public void gen(TreeElement t) {
		// save AST cursor
		println("AST __t" + t.ID + " = _t;");

		// If there is a label on the root, then assign that to the variable
		if (t.root.getLabel() != null) {
			println(t.root.getLabel() + " = (ASTNULL == _t) ? null : ("+labeledElementASTType +")_t;");
		}

      // check for invalid modifiers ! and ^ on tree element roots
      if ( t.root.getAutoGenType() == GrammarElement.AUTO_GEN_BANG ) {
          antlrTool.error("Suffixing a root node with '!' is not implemented",
                       grammar.getFilename(), t.getLine(), t.getColumn());
          t.root.setAutoGenType(GrammarElement.AUTO_GEN_NONE);
      }
      if ( t.root.getAutoGenType() == GrammarElement.AUTO_GEN_CARET ) {
          antlrTool.warning("Suffixing a root node with '^' is redundant; already a root",
                       grammar.getFilename(), t.getLine(), t.getColumn());
          t.root.setAutoGenType(GrammarElement.AUTO_GEN_NONE);
      }

		// Generate AST variables
		genElementAST(t.root);
		if (grammar.buildAST) {
			// Save the AST construction state
			println("ASTPair __currentAST" + t.ID + " = currentAST.copy();");
			// Make the next item added a child of the TreeElement root
			println("currentAST.root = currentAST.child;");
			println("currentAST.child = null;");
		}

		// match root
        if ( t.root instanceof WildcardElement ) {
            println("if (null == _t) throw new MismatchedTokenException();");
        }
        else {
				genMatch(t.root);
		}
		// move to list of children
		println("_t = _t.getFirstChild();");

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
		println("_t = _t.getNextSibling();");
	}
	/** Generate the tree-parser CSharp file */
	public void gen(TreeWalkerGrammar g) throws IOException {
		// SAS: debugging stuff removed for now...
		setGrammar(g);
		if (!(grammar instanceof TreeWalkerGrammar)) {
			antlrTool.panic("Internal error generating tree-walker");
		}
		genBody(g);
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
			println("if (null == _t) throw new MismatchedTokenException();");
		}
		else if (grammar instanceof LexerGrammar) {
			if ( grammar instanceof LexerGrammar &&
				(!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
				declareSaveIndexVariableIfNeeded();
				println("_saveIndex = text.Length;");
			}
			println("matchNot(EOF/*_CHAR*/);");
			if ( grammar instanceof LexerGrammar &&
				(!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
				declareSaveIndexVariableIfNeeded();
				println("text.Length = _saveIndex;"); // kill text atom put in buffer
			}
		}
		else {
			println("matchNot(" + getValueString(Token.EOF_TYPE) + ");");
		}

		// tack on tree cursor motion if doing a tree walker
		if (grammar instanceof TreeWalkerGrammar) {
			println("_t = _t.getNextSibling();");
		}
	}

	/** Generate code for the given grammar element.
	 * @param blk The (...)* block to generate
	 */
	public void gen(ZeroOrMoreBlock blk) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen*("+blk+")");
		println("{    // ( ... )*");
		tabs++;
		genBlockPreamble(blk);
		String label;
		if ( blk.getLabel() != null ) {
			label = blk.getLabel();
		}
		else {
			label = "_loop" + blk.ID;
		}
		println("for (;;)");
		println("{");
		tabs++;
		// generate the init action for ()+ ()* inside the loop
        // this allows us to do usefull EOF checking...
        genBlockInitAction(blk);

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
			println("if ("+predictExit+") goto "+label+"_breakloop;");
		}

		CSharpBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
		genBlockFinish(howToFinish, "goto " + label + "_breakloop;");

			tabs--;
		println("}");
		_print(label+"_breakloop:");
		println(";");
		tabs--;
		println("}    // ( ... )*");

		// Restore previous AST generation
		currentASTResult = saveCurrentASTResult;
	}

	/** Generate an alternative.
	  * @param alt  The alternative to generate
	  * @param blk The block to which the alternative belongs
	  */
	protected void genAlt(Alternative alt, AlternativeBlock blk)
	{
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
			println("try        // for error handling");
			println("{");
			tabs++;
		}

		AlternativeElement elem = alt.head;
		while ( !(elem instanceof BlockEndElement) ) {
			elem.generate(); // alt can begin with anything. Ask target to gen.
			elem = elem.next;
		}

		if ( genAST)
		{
			if (blk instanceof RuleBlock)
			{
				// Set the AST return value for the rule
				RuleBlock rblk = (RuleBlock)blk;
				if( usingCustomAST )
				{
					println(rblk.getRuleName() + "_AST = ("+labeledElementASTType+")currentAST.root;");
				}
				else
				{
					println(rblk.getRuleName() + "_AST = currentAST.root;");
				}
			}
			else if (blk.getLabel() != null) {
				// ### future: also set AST value for labeled subrules.
				// println(blk.getLabel() + "_AST = ("+labeledElementASTType+")currentAST.root;");
            	antlrTool.warning("Labeled subrules not yet supported", grammar.getFilename(), blk.getLine(), blk.getColumn());
			}
		}

		if (alt.exceptionSpec != null)
		{
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
	 */
	protected void genBitsets( Vector bitsetList, int maxVocabulary ) {
		println("");
		for (int i = 0; i < bitsetList.size(); i++)
		{
			BitSet p = (BitSet)bitsetList.elementAt(i);
			// Ensure that generated BitSet is large enough for vocabulary
			p.growToInclude(maxVocabulary);
            genBitSet(p, i);
        }
    }

    /** Do something simple like:
     *  private static final long[] mk_tokenSet_0() {
     *    long[] data = { -2305839160922996736L, 63L, 16777216L, 0L, 0L, 0L };
     *    return data;
     *  }
     *  public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
     *
     *  Or, for large bitsets, optimize init so ranges are collapsed into loops.
     *  This is most useful for lexers using unicode.
     */
    private void genBitSet(BitSet p, int id) {
        // initialization data
        println("private static long[] mk_" + getBitsetName(id) + "()");
        println("{");
        tabs++;
        int n = p.lengthInLongWords();
        if ( n<BITSET_OPTIMIZE_INIT_THRESHOLD ) {
            println("long[] data = { " + p.toStringOfWords() + "};");
        }
        else {
            // will init manually, allocate space then set values
            println("long[] data = new long["+n+"];");
            long[] elems = p.toPackedArray();
            for (int i = 0; i < elems.length;) {
                if ( (i+1)==elems.length || elems[i]!=elems[i+1] ) {
                    // last number or no run of numbers, just dump assignment
                    println("data["+i+"]="+elems[i]+"L;");
                    i++;
                }
                else
				{
                    // scan to find end of run
                    int j;
                    for (j = i + 1; j < elems.length && elems[j]==elems[i]; j++)
                    {
						;
                    }
                    // j-1 is last member of run
                    println("for (int i = "+i+"; i<="+(j-1)+"; i++) { data[i]="+
                            elems[i]+"L; }");
                    i = j;
                }
            }
        }

        println("return data;");
        tabs--;
        println("}");
		// BitSet object
        println("public static readonly BitSet " + getBitsetName(id) + " = new BitSet(" +
            "mk_" + getBitsetName(id) + "()" + ");");
	}

    /** Given the index of a bitset in the bitset list, generate a unique name.
     * Specific code-generators may want to override this
     * if the language does not allow '_' or numerals in identifiers.
     * @param index  The index of the bitset in the bitset list.
     */
    protected String getBitsetName(int index) {
        return "tokenSet_" + index + "_";
    }

	/** Generate the finish of a block, using a combination of the info
	* returned from genCommonBlock() and the action to perform when
	* no alts were taken
	* @param howToFinish The return of genCommonBlock()
	* @param noViableAction What to generate when no alt is taken
	*/
	private void genBlockFinish(CSharpBlockFinishingInfo howToFinish, String noViableAction)
	{

		if (howToFinish.needAnErrorClause &&
			(howToFinish.generatedAnIf || howToFinish.generatedSwitch))
		{
			if ( howToFinish.generatedAnIf ) {
				println("else");
				println("{");
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
			if (howToFinish.needAnErrorClause && howToFinish.generatedSwitch &&
				!howToFinish.generatedAnIf && noViableAction != null)
			{
				// Check to make sure that noViableAction is only a throw statement
				if (noViableAction.indexOf("throw") == 0 || noViableAction.indexOf("goto") == 0) {
					// Remove the break statement since it isn't reachable with a throw exception
					int endOfBreak = howToFinish.postscript.indexOf("break;") + 6;
					String newPostScript = howToFinish.postscript.substring(endOfBreak);
					println(newPostScript);
				}
				else {
					println(howToFinish.postscript);
				}
			}
			else {
				println(howToFinish.postscript);
			}
		}
	}

    /** Generate the init action for a block, which may be a RuleBlock or a
     * plain AlternativeBLock.
     * @blk The block for which the preamble is to be generated.
     */
    protected void genBlockInitAction(AlternativeBlock blk)
	{
        // dump out init action
        if (blk.initAction != null) {
            printAction(processActionForSpecialSymbols(blk.initAction, blk.getLine(), currentRule, null));
        }
    }

	/** Generate the header for a block, which may be a RuleBlock or a
     * plain AlternativeBLock.  This generates any variable declarations
     * and syntactic-predicate-testing variables.
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
					//Variables for labeled rule refs and
					//subrules are different than variables for
					//grammar atoms.  This test is a little tricky
					//because we want to get all rule refs and ebnf,
					//but not rule blocks or syntactic predicates
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
							// Special case for inverted subrules that
							// will be inlined.  Treat these like
							// token or char literal references
							println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
							if (grammar.buildAST) {
								genASTDeclaration(a);
							}
						}
						else {
							if (grammar.buildAST) {
								// Always gen AST variables for
								// labeled elements, even if the
								// element itself is marked with !
								genASTDeclaration(a);
							}
							if ( grammar instanceof LexerGrammar ) {
								println("Token "+a.getLabel()+" = null;");
							}
							if (grammar instanceof TreeWalkerGrammar) {
								// always generate rule-ref variables
								// for tree walker
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
							//println(labeledElementASTType+" " + a.getLabel() + "_AST = null;");
							if (a instanceof GrammarAtom &&
								((GrammarAtom)a).getASTNodeType()!=null ) {
								GrammarAtom ga = (GrammarAtom)a;
								genASTDeclaration(a, ga.getASTNodeType());
							}
							else {
								genASTDeclaration(a);
							}
						}
					}
				}
			}
		}
	}

	public void genBody(LexerGrammar g) throws IOException
	{
		// SAS: moved output creation to method so a subclass can change
		//      how the output is generated (for VAJ interface)
		setupOutput(grammar.getClassName());

		genAST = false;	// no way to gen trees.
		saveText = true;	// save consumed characters.

		tabs=0;

		// Generate header common to all CSharp output files
		genHeader();
		// Do not use printAction because we assume tabs==0
		println(behavior.getHeaderAction(""));

      		// Generate the CSharp namespace declaration (if specified)
		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);
		tabs++;

		// Generate header specific to lexer CSharp file
		// println("import java.io.FileInputStream;");
		println("// Generate header specific to lexer CSharp file");
		println("using System;");
		println("using Stream                          = System.IO.Stream;");
		println("using TextReader                      = System.IO.TextReader;");
		println("using Hashtable                       = System.Collections.Hashtable;");
		println("using Comparer                        = System.Collections.Comparer;");
		if ( !(g.caseSensitiveLiterals) )
		{
			println("using CaseInsensitiveHashCodeProvider = System.Collections.CaseInsensitiveHashCodeProvider;");
			println("using CaseInsensitiveComparer         = System.Collections.CaseInsensitiveComparer;");
		}
		println("");
		println("using TokenStreamException            = antlr.TokenStreamException;");
		println("using TokenStreamIOException          = antlr.TokenStreamIOException;");
		println("using TokenStreamRecognitionException = antlr.TokenStreamRecognitionException;");
		println("using CharStreamException             = antlr.CharStreamException;");
		println("using CharStreamIOException           = antlr.CharStreamIOException;");
		println("using ANTLRException                  = antlr.ANTLRException;");
		println("using CharScanner                     = antlr.CharScanner;");
		println("using InputBuffer                     = antlr.InputBuffer;");
		println("using ByteBuffer                      = antlr.ByteBuffer;");
		println("using CharBuffer                      = antlr.CharBuffer;");
		println("using Token                           = antlr.Token;");
		println("using CommonToken                     = antlr.CommonToken;");
		println("using SemanticException               = antlr.SemanticException;");
		println("using RecognitionException            = antlr.RecognitionException;");
		println("using NoViableAltForCharException     = antlr.NoViableAltForCharException;");
		println("using MismatchedCharException         = antlr.MismatchedCharException;");
		println("using TokenStream                     = antlr.TokenStream;");
		println("using LexerSharedInputState           = antlr.LexerSharedInputState;");
		println("using BitSet                          = antlr.collections.impl.BitSet;");

		// Generate user-defined lexer file preamble
		println(grammar.preambleAction.getText());

		// Generate lexer class definition
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
		}
		else {
			sup = "antlr." + grammar.getSuperClass();
		}

		// print javadoc comment if any
		if ( grammar.comment!=null )
		{
			_println(grammar.comment);
		}

        Token tprefix = (Token)grammar.options.get("classHeaderPrefix");
		if (tprefix == null) {
			print("public ");
		}
        else {
            String p = StringUtils.stripFrontBack(tprefix.getText(), "\"", "\"");
            if (p == null) {
				print("public ");
			}
			else {
                print(p+" ");
            }
        }

		print("class " + grammar.getClassName() + " : "+sup);
		println(", TokenStream");
		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null )
		{
			String suffix = StringUtils.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null )
			{
				print(", "+suffix);	// must be an interface name for CSharp
			}
		}
		println(" {");
		tabs++;

		// Generate 'const' definitions for Token IDs
		genTokenDefinitions(grammar.tokenManager);

		// Generate user-defined lexer class members
		print(
			processActionForSpecialSymbols(grammar.classMemberAction.getText(), grammar.classMemberAction.getLine(), currentRule, null)
			);

		//
		// Generate the constructor from InputStream, which in turn
		// calls the ByteBuffer constructor
		//
		println("public " + grammar.getClassName() + "(Stream ins) : this(new ByteBuffer(ins))");
		println("{");
		println("}");
		println("");

		//
		// Generate the constructor from Reader, which in turn
		// calls the CharBuffer constructor
		//
		println("public " + grammar.getClassName() + "(TextReader r) : this(new CharBuffer(r))");
		println("{");
		println("}");
		println("");

		print("public " + grammar.getClassName() + "(InputBuffer ib)");
		// if debugging, wrap the input buffer in a debugger
		if (grammar.debuggingOutput)
			println(" : this(new LexerSharedInputState(new antlr.debug.DebuggingInputBuffer(ib)))");
		else
			println(" : this(new LexerSharedInputState(ib))");
		println("{");
		println("}");
		println("");

		//
		// Generate the constructor from InputBuffer (char or byte)
		//
		println("public " + grammar.getClassName() + "(LexerSharedInputState state) : base(state)");
		println("{");
		tabs++;
		println("initialize();");
		tabs--;
		println("}");

		// Generate the initialize function
		println("private void initialize()");
		println("{");
		tabs++;

		// if debugging, set up array variables and call user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("ruleNames  = _ruleNames;");
			println("semPredNames = _semPredNames;");
			println("setupDebugging();");
		}

	      // Generate the setting of various generated options.
	      // These need to be before the literals since ANTLRHashString depends on
	      // the casesensitive stuff.
	      println("caseSensitiveLiterals = " + g.caseSensitiveLiterals + ";");
	      println("setCaseSensitive(" + g.caseSensitive + ");");

		// Generate the initialization of a hashtable
		// containing the string literals used in the lexer
		// The literals variable itself is in CharScanner
		if (g.caseSensitiveLiterals)
			println("literals = new Hashtable(100, (float) 0.4, null, Comparer.Default);");
		else
			println("literals = new Hashtable(100, (float) 0.4, CaseInsensitiveHashCodeProvider.Default, CaseInsensitiveComparer.Default);");
		Enumeration keys = grammar.tokenManager.getTokenSymbolKeys();
		while ( keys.hasMoreElements() ) {
			String key = (String)keys.nextElement();
			if ( key.charAt(0) != '"' ) {
				continue;
			}
			TokenSymbol sym = grammar.tokenManager.getTokenSymbol(key);
			if ( sym instanceof StringLiteralSymbol ) {
				StringLiteralSymbol s = (StringLiteralSymbol)sym;
				println("literals.Add(" + s.getId() + ", " + s.getTokenType() + ");");
			}
		}

		Enumeration ids;
		tabs--;
		println("}");

		// generate the rule name array for debugging
		if (grammar.debuggingOutput) {
			println("private const string[] _ruleNames = {");

			ids = grammar.rules.elements();
			int ruleNum=0;
			while ( ids.hasMoreElements() ) {
				GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
				if ( sym instanceof RuleSymbol)
					println("  \""+((RuleSymbol)sym).getId()+"\",");
			}
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
				genRule(sym, false, ruleNum++, grammar.tokenManager);
			}
			exitIfError();
		}

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			genSemPredMap();

		// Generate the bitsets used throughout the lexer
		genBitsets(bitsetsUsed, ((LexerGrammar)grammar).charVocabulary.size());

		println("");
		tabs--;
		println("}");

		tabs--;
		// Generate the CSharp namespace closures (if required)
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the lexer output stream
		currentOutput.close();
		currentOutput = null;
	}

	public void genInitFactory( Grammar g ) {
		if( g.buildAST )
		{
			// Generate the method to initialize an ASTFactory when we're
			// building AST's
			println("static public void initializeASTFactory( ASTFactory factory )");
			println("{");
			tabs++;

			println("factory.setMaxNodeType("+g.tokenManager.maxTokenType()+");");

	        // Walk the token vocabulary and generate code to register every TokenID->ASTNodeType
	        // mapping specified in the  tokens {...} section with the ASTFactory.
			Vector v = g.tokenManager.getVocabulary();
			for (int i = 0; i < v.size(); i++) {
				String s = (String)v.elementAt(i);
				if (s != null) {
					TokenSymbol ts = g.tokenManager.getTokenSymbol(s);
					if (ts != null && ts.getASTNodeType() != null) {
						println("factory.setTokenTypeASTNodeType(" + s + ", \"" + ts.getASTNodeType() + "\");");
					}
				}
			}

			tabs--;
			println("}");
		}
	}

	public void genBody(ParserGrammar g) throws IOException
	{
		// Open the output stream for the parser and set the currentOutput
		// SAS: moved file setup so subclass could do it (for VAJ interface)
		setupOutput(grammar.getClassName());

		genAST = grammar.buildAST;

		tabs = 0;

		// Generate the header common to all output files.
		genHeader();
		// Do not use printAction because we assume tabs==0
		println(behavior.getHeaderAction(""));

      		// Generate the CSharp namespace declaration (if specified)
		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);
		tabs++;

		// Generate header for the parser
		println("// Generate the header common to all output files.");
		println("using System;");
		println("");
		println("using TokenBuffer              = antlr.TokenBuffer;");
		println("using TokenStreamException     = antlr.TokenStreamException;");
		println("using TokenStreamIOException   = antlr.TokenStreamIOException;");
		println("using ANTLRException           = antlr.ANTLRException;");
		println("using " + grammar.getSuperClass() + " = antlr." + grammar.getSuperClass() + ";");
		println("using Token                    = antlr.Token;");
		println("using TokenStream              = antlr.TokenStream;");
		println("using RecognitionException     = antlr.RecognitionException;");
		println("using NoViableAltException     = antlr.NoViableAltException;");
		println("using MismatchedTokenException = antlr.MismatchedTokenException;");
		println("using SemanticException        = antlr.SemanticException;");
		println("using ParserSharedInputState   = antlr.ParserSharedInputState;");
		println("using BitSet                   = antlr.collections.impl.BitSet;");
		if ( genAST ) {
			println("using AST                      = antlr.collections.AST;");
			println("using ASTPair                  = antlr.ASTPair;");
			println("using ASTFactory               = antlr.ASTFactory;");
			println("using ASTArray                 = antlr.collections.impl.ASTArray;");
		}

		// Output the user-defined parser preamble
		println(grammar.preambleAction.getText());

		// Generate parser class definition
		String sup=null;
		if ( grammar.superClass != null )
			sup = grammar.superClass;
		else
			sup = "antlr." + grammar.getSuperClass();

		// print javadoc comment if any
		if ( grammar.comment!=null ) {
			_println(grammar.comment);
		}

        Token tprefix = (Token)grammar.options.get("classHeaderPrefix");
		if (tprefix == null) {
			print("public ");
		}
        else {
            String p = StringUtils.stripFrontBack(tprefix.getText(), "\"", "\"");
            if (p == null) {
				print("public ");
			}
			else {
                print(p+" ");
            }
        }

		println("class " + grammar.getClassName() + " : "+sup);

		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null ) {
			String suffix = StringUtils.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null )
				print("              , "+suffix);	// must be an interface name for CSharp
		}
		println("{");
		tabs++;

		// Generate 'const' definitions for Token IDs
		genTokenDefinitions(grammar.tokenManager);

		// set up an array of all the rule names so the debugger can
		// keep track of them only by number -- less to store in tree...
		if (grammar.debuggingOutput) {
			println("private const string[] _ruleNames = {");
			tabs++;

			Enumeration ids = grammar.rules.elements();
			int ruleNum=0;
			while ( ids.hasMoreElements() ) {
				GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
				if ( sym instanceof RuleSymbol)
					println("  \""+((RuleSymbol)sym).getId()+"\",");
			}
			tabs--;
			println("};");
		}

		// Generate user-defined parser class members
		print(
			processActionForSpecialSymbols(grammar.classMemberAction.getText(), grammar.classMemberAction.getLine(), currentRule, null)
			);

		// Generate parser class constructor from TokenBuffer
		println("");
		println("protected void initialize()");
		println("{");
		tabs++;
		println("tokenNames = tokenNames_;");

		if( grammar.buildAST )
			println("initializeFactory();");

		// if debugging, set up arrays and call the user-overridable
		//   debugging setup method
		if ( grammar.debuggingOutput ) {
			println("ruleNames  = _ruleNames;");
			println("semPredNames = _semPredNames;");
			println("setupDebugging(tokenBuf);");
		}
		tabs--;
		println("}");
		println("");

		println("");
		println("protected " + grammar.getClassName() + "(TokenBuffer tokenBuf, int k) : base(tokenBuf, k)");
		println("{");
		tabs++;
		println("initialize();");
		tabs--;
		println("}");
		println("");

		println("public " + grammar.getClassName() + "(TokenBuffer tokenBuf) : this(tokenBuf," + grammar.maxk + ")");
		println("{");
		println("}");
		println("");

		// Generate parser class constructor from TokenStream
		println("protected " + grammar.getClassName()+"(TokenStream lexer, int k) : base(lexer,k)");
		println("{");
		tabs++;
		println("initialize();");
		tabs--;
		println("}");
		println("");

		println("public " + grammar.getClassName()+"(TokenStream lexer) : this(lexer," + grammar.maxk + ")");
		println("{");
		println("}");
		println("");

		println("public " + grammar.getClassName()+"(ParserSharedInputState state) : base(state," + grammar.maxk + ")");
		println("{");
		tabs++;
		println("initialize();");
		tabs--;
		println("}");
		println("");

		astTypes = new java.util.Vector(100);

		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		int ruleNum=0;
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRule(rs, rs.references.size()==0, ruleNum++, grammar.tokenManager);
			}
			exitIfError();
		}
		if ( usingCustomAST )
		{
			// when we are using a custom AST, overload Parser.getAST() to return the
			// custom AST type
			println("public new " + labeledElementASTType + " getAST()");
			println("{");
			tabs++;
			println("return (" + labeledElementASTType + ") returnAST;");
			tabs--;
			println("}");
			println("");
		}

		// Generate the method that initializes the ASTFactory when we're
		// building AST's
		println("private void initializeFactory()");
		println("{");
		tabs++;
		if( grammar.buildAST ) {
			println("if (astFactory == null)");
			println("{");
			tabs++;
			if( usingCustomAST )
			{
				println("astFactory = new ASTFactory(\"" + labeledElementASTType + "\");");
			}
			else
				println("astFactory = new ASTFactory();");
			tabs--;
			println("}");
			println("initializeASTFactory( astFactory );");
		}
		tabs--;
		println("}");
		genInitFactory( g );

		// Generate the token names
		genTokenStrings();

		// Generate the bitsets used throughout the grammar
		genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType());

		// Generate the semantic predicate map for debugging
		if (grammar.debuggingOutput)
			genSemPredMap();

		// Close class definition
		println("");
		tabs--;
		println("}");

		tabs--;
		// Generate the CSharp namespace closures (if required)
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	public void genBody(TreeWalkerGrammar g) throws IOException
	{
		// Open the output stream for the parser and set the currentOutput
		// SAS: move file open to method so subclass can override it
		//      (mainly for VAJ interface)
		setupOutput(grammar.getClassName());

		genAST = grammar.buildAST;
		tabs = 0;

		// Generate the header common to all output files.
		genHeader();
		// Do not use printAction because we assume tabs==0
		println(behavior.getHeaderAction(""));

      // Generate the CSharp namespace declaration (if specified)
		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);
		tabs++;

		// Generate header specific to the tree-parser CSharp file
		println("// Generate header specific to the tree-parser CSharp file");
		println("using System;");
		println("");
		println("using " + grammar.getSuperClass() + " = antlr." + grammar.getSuperClass() + ";");
		println("using Token                    = antlr.Token;");
		println("using AST                      = antlr.collections.AST;");
		println("using RecognitionException     = antlr.RecognitionException;");
		println("using ANTLRException           = antlr.ANTLRException;");
		println("using NoViableAltException     = antlr.NoViableAltException;");
		println("using MismatchedTokenException = antlr.MismatchedTokenException;");
		println("using SemanticException        = antlr.SemanticException;");
		println("using BitSet                   = antlr.collections.impl.BitSet;");
		println("using ASTPair                  = antlr.ASTPair;");
		println("using ASTFactory               = antlr.ASTFactory;");
		println("using ASTArray                 = antlr.collections.impl.ASTArray;");

		// Output the user-defined parser premamble
		println(grammar.preambleAction.getText());

		// Generate parser class definition
		String sup=null;
		if ( grammar.superClass!=null ) {
			sup = grammar.superClass;
		}
		else {
			sup = "antlr." + grammar.getSuperClass();
		}
		println("");

		// print javadoc comment if any
		if ( grammar.comment!=null ) {
			_println(grammar.comment);
		}

        Token tprefix = (Token)grammar.options.get("classHeaderPrefix");
		if (tprefix == null) {
			print("public ");
		}
        else {
            String p = StringUtils.stripFrontBack(tprefix.getText(), "\"", "\"");
            if (p == null) {
				print("public ");
			}
			else {
                print(p+" ");
            }
        }

		println("class " + grammar.getClassName() + " : "+sup);
		Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
		if ( tsuffix != null ) {
			String suffix = StringUtils.stripFrontBack(tsuffix.getText(),"\"","\"");
			if ( suffix != null ) {
				print("              , "+suffix);	// must be an interface name for CSharp
			}
		}
		println("{");
		tabs++;

		// Generate 'const' definitions for Token IDs
		genTokenDefinitions(grammar.tokenManager);

		// Generate user-defined parser class members
		print(
			processActionForSpecialSymbols(grammar.classMemberAction.getText(), grammar.classMemberAction.getLine(), currentRule, null)
			);

		// Generate default parser class constructor
		println("public " + grammar.getClassName() + "()");
		println("{");
		tabs++;
		println("tokenNames = tokenNames_;");
		tabs--;
		println("}");
		println("");

		astTypes = new java.util.Vector();
		// Generate code for each rule in the grammar
		Enumeration ids = grammar.rules.elements();
		int ruleNum=0;
		String ruleNameInits = "";
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
			if ( sym instanceof RuleSymbol) {
				RuleSymbol rs = (RuleSymbol)sym;
				genRule(rs, rs.references.size()==0, ruleNum++, grammar.tokenManager);
			}
			exitIfError();
		}

		if ( usingCustomAST )
		{
			// when we are using a custom ast override Parser.getAST to return the
			// custom AST type
			println("public new " + labeledElementASTType + " getAST()");
			println("{");
			tabs++;
			println("return (" + labeledElementASTType + ") returnAST;");
			tabs--;
			println("}");
			println("");
		}

		// Generate the ASTFactory initialization function
		genInitFactory( grammar );

		// Generate the token names
		genTokenStrings();

		// Generate the bitsets used throughout the grammar
		genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType());

		// Close class definition
		tabs--;
		println("}");
		println("");

		tabs--;
		// Generate the CSharp namespace closures (if required)
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
		int wrap = (grammar instanceof LexerGrammar) ? 4 : 1;
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

	/**Generate common code for a block of alternatives; return a
	* postscript that needs to be generated at the end of the
	* block.  Other routines may append else-clauses and such for
	* error checking before the postfix is generated.  If the
	* grammar is a lexer, then generate alternatives in an order
	* where alternatives requiring deeper lookahead are generated
	* first, and EOF in the lookahead set reduces the depth of
	* the lookahead.  @param blk The block to generate @param
	* noTestForSingle If true, then it does not generate a test
	* for a single alternative.
	*/
	public CSharpBlockFinishingInfo genCommonBlock(AlternativeBlock blk,
		boolean noTestForSingle)
	{
		int nIF=0;
		boolean createdLL1Switch = false;
		int closingBracesOfIFSequence = 0;
		CSharpBlockFinishingInfo finishingInfo = new CSharpBlockFinishingInfo();
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genCommonBlock("+blk+")");

		// Save the AST generation state, and set it to that of the block
		boolean savegenAST = genAST;
		genAST = genAST && blk.getAutoGen();

			boolean oldsaveTest = saveText;
		saveText = saveText && blk.getAutoGen();

		// Is this block inverted?  If so, generate special-case code
		if ( blk.not &&
			analyzer.subruleCanBeInverted(blk, grammar instanceof LexerGrammar) )
		{
			if ( DEBUG_CODE_GENERATOR ) System.out.println("special case: ~(subrule)");
			Lookahead p = analyzer.look(1, blk);
			// Variable assignment for labeled elements
			if (blk.getLabel() != null && syntacticPredLevel == 0) {
				println(blk.getLabel() + " = " + lt1Value + ";");
			}

			// AST
			genElementAST(blk);

			String astArgs="";
			if (grammar instanceof TreeWalkerGrammar) {
				if ( usingCustomAST )
					astArgs = "(AST)_t,";
				else
					astArgs = "_t,";
			}

			// match the bitset for the alternative
			println("match(" + astArgs + getBitsetName(markBitsetForGen(p.fset)) + ");");

			// tack on tree cursor motion if doing a tree walker
			if (grammar instanceof TreeWalkerGrammar)
			{
				println("_t = _t.getNextSibling();");
			}
			return finishingInfo;
		}

		// Special handling for single alt
		if (blk.getAlternatives().size() == 1)
		{
			Alternative alt = blk.getAlternativeAt(0);
			// Generate a warning if there is a synPred for single alt.
			if (alt.synPred != null)
			{
				antlrTool.warning(
					"Syntactic predicate superfluous for single alternative",
					grammar.getFilename(),
               blk.getAlternativeAt(0).synPred.getLine(),
               blk.getAlternativeAt(0).synPred.getColumn()
					);
			}
			if (noTestForSingle)
			{
				if (alt.semPred != null)
				{
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
		for (int i=0; i<blk.getAlternatives().size(); i++)
		{
			Alternative a = blk.getAlternativeAt(i);
			if ( suitableForCaseExpression(a) ) {
				nLL1++;
			}
		}

		// do LL(1) cases
		if ( nLL1 >= makeSwitchThreshold)
		{
			// Determine the name of the item to be compared
			String testExpr = lookaheadString(1);
			createdLL1Switch = true;
			// when parsing trees, convert null to valid tree node with NULL lookahead
			if ( grammar instanceof TreeWalkerGrammar )
			{
				println("if (null == _t)");
				tabs++;
				println("_t = ASTNULL;");
				tabs--;
			}
			println("switch ( " + testExpr+" )");
			println("{");
			//tabs++;
			for (int i=0; i<blk.alternatives.size(); i++)
			{
				Alternative alt = blk.getAlternativeAt(i);
				// ignore any non-LL(1) alts, predicated alts,
				// or end-of-token alts for case expressions
				bSaveIndexCreated = false;
				if ( !suitableForCaseExpression(alt) )
				{
					continue;
				}
				Lookahead p = alt.cache[1];
				if (p.fset.degree() == 0 && !p.containsEpsilon())
				{
					antlrTool.warning("Alternate omitted due to empty prediction set",
						grammar.getFilename(),
						alt.head.getLine(), alt.head.getColumn());
				}
				else
				{
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

		// do non-LL(1) and nondeterministic cases This is tricky in
		// the lexer, because of cases like: STAR : '*' ; ASSIGN_STAR
		// : "*="; Since nextToken is generated without a loop, then
		// the STAR will have end-of-token as it's lookahead set for
		// LA(2).  So, we must generate the alternatives containing
		// trailing end-of-token in their lookahead sets *after* the
		// alternatives without end-of-token.  This implements the
		// usual lexer convention that longer matches come before
		// shorter ones, e.g.  "*=" matches ASSIGN_STAR not STAR
		//
		// For non-lexer grammars, this does not sort the alternates
		// by depth Note that alts whose lookahead is purely
		// end-of-token at k=1 end up as default or else clauses.
		int startDepth = (grammar instanceof LexerGrammar) ? grammar.maxk : 0;
		for (int altDepth = startDepth; altDepth >= 0; altDepth--) {
			if ( DEBUG_CODE_GENERATOR ) System.out.println("checking depth "+altDepth);
			for (int i=0; i<blk.alternatives.size(); i++) {
				Alternative alt = blk.getAlternativeAt(i);
				if ( DEBUG_CODE_GENERATOR ) System.out.println("genAlt: "+i);
				// if we made a switch above, ignore what we already took care
				// of.  Specifically, LL(1) alts with no preds
				// that do not have end-of-token in their prediction set
				// and that are not giant unicode sets.
				if ( createdLL1Switch && suitableForCaseExpression(alt) )
				{
					if ( DEBUG_CODE_GENERATOR ) System.out.println("ignoring alt because it was in the switch");
					continue;
				}
				String e;

				boolean unpredicted = false;

				if (grammar instanceof LexerGrammar) {
					// Calculate the "effective depth" of the alt,
					// which is the max depth at which
					// cache[depth]!=end-of-token
					int effectiveDepth = alt.lookaheadDepth;
					if (effectiveDepth == GrammarAnalyzer.NONDETERMINISTIC)
					{
						// use maximum lookahead
						effectiveDepth = grammar.maxk;
					}
					while ( effectiveDepth >= 1 &&
						alt.cache[effectiveDepth].containsEpsilon() )
					{
						effectiveDepth--;
					}
					// Ignore alts whose effective depth is other than
					// the ones we are generating for this iteration.
					if (effectiveDepth != altDepth)
					{
						if ( DEBUG_CODE_GENERATOR )
							System.out.println("ignoring alt because effectiveDepth!=altDepth;"+effectiveDepth+"!="+altDepth);
						continue;
					}
					unpredicted = lookaheadIsEmpty(alt, effectiveDepth);
					e = getLookaheadTestExpression(alt, effectiveDepth);
				}
				else
				{
					unpredicted = lookaheadIsEmpty(alt, grammar.maxk);
					e = getLookaheadTestExpression(alt, grammar.maxk);
				}

				// Was it a big unicode range that forced unsuitability
				// for a case expression?
            if (alt.cache[1].fset.degree() > caseSizeThreshold &&
                suitableForCaseExpression(alt))
				{
					if ( nIF==0 )
					{
						println("if " + e);
						println("{");
					}
					else {
						println("else if " + e);
						println("{");
					}
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
						String actionStr = processActionForSpecialSymbols(alt.semPred,
							blk.line,
							currentRule,
							tInfo);
						// ignore translation info...we don't need to
						// do anything with it.  call that will inform
						// SemanticPredicateListeners of the result
						if (((grammar instanceof ParserGrammar) || (grammar instanceof LexerGrammar)) &&
								grammar.debuggingOutput) {
							e = "("+e+"&& fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.PREDICTING,"+ //FIXME
								addSemPred(charFormatter.escapeString(actionStr))+","+actionStr+"))";
						}
						else {
							e = "("+e+"&&("+actionStr +"))";
						}
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
								println("if (_t == null)");
								tabs++;
								println("_t = ASTNULL;");
								tabs--;
							}
							println("if " + e);
							println("{");
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
			ps+="}";
		}

		// Restore the AST generation state
		genAST = savegenAST;

		// restore save text state
		saveText=oldsaveTest;

		// Return the finishing info.
		if ( createdLL1Switch ) {
			tabs--;
			finishingInfo.postscript = ps+"break; }";
			finishingInfo.generatedSwitch = true;
			finishingInfo.generatedAnIf = nIF>0;
			//return new CSharpBlockFinishingInfo(ps+"}",true,nIF>0); // close up switch statement

		}
		else {
			finishingInfo.postscript = ps;
			finishingInfo.generatedSwitch = false;
			finishingInfo.generatedAnIf = nIF>0;
			// return new CSharpBlockFinishingInfo(ps, false,nIF>0);
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
		if ( grammar instanceof TreeWalkerGrammar && !grammar.buildAST )
		{
			String elementRef;
			String astName;

			// Generate names and declarations of the AST variable(s)
			if (el.getLabel() == null)
			{
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

		if (grammar.buildAST && syntacticPredLevel == 0)
		{
      		boolean needASTDecl =
      			(genAST &&
      			(el.getLabel() != null || (el.getAutoGenType() != GrammarElement.AUTO_GEN_BANG)));

      		// RK: if we have a grammar element always generate the decl
      		// since some guy can access it from an action and we can't
      		// peek ahead (well not without making a mess).
      		// I'd prefer taking this out.
      		if (el.getAutoGenType() != GrammarElement.AUTO_GEN_BANG &&
				(el instanceof TokenRefElement))
      			needASTDecl = true;

      		boolean doNoGuessTest = (grammar.hasSyntacticPredicate && needASTDecl);

			String elementRef;
			String astNameBase;

			// Generate names and declarations of the AST variable(s)
			if (el.getLabel() != null)
			{
				// if the element is labeled use that name...
				elementRef = el.getLabel();
				astNameBase = el.getLabel();
			}
			else
			{
				// else generate a temporary name...
				elementRef = lt1Value;
				// Generate AST variables for unlabeled stuff
				astNameBase = "tmp" + astVarNumber;
				astVarNumber++;
			}

      		// Generate the declaration if required.
      		if (needASTDecl)
      		{
				// Generate the declaration
				if ( el instanceof GrammarAtom )
				{
					GrammarAtom ga = (GrammarAtom)el;
					if ( ga.getASTNodeType()!=null )
					{
						genASTDeclaration(el, astNameBase, ga.getASTNodeType());
						//println(ga.getASTNodeType()+" " + astName+" = null;");
					}
					else
					{
						genASTDeclaration(el, astNameBase, labeledElementASTType);
						//println(labeledElementASTType+" " + astName + " = null;");
					}
				}
				else
				{
					genASTDeclaration(el, astNameBase, labeledElementASTType);
					//println(labeledElementASTType+" " + astName + " = null;");
				}
			}

	      	// for convenience..
    		String astName = astNameBase + "_AST";

			// Map the generated AST variable in the alternate
			mapTreeVariable(el, astName);
			if (grammar instanceof TreeWalkerGrammar)
			{
				// Generate an "input" AST variable also
				println(labeledElementASTType+" " + astName + "_in = null;");
			}


			// Enclose actions with !guessing
			if (doNoGuessTest) {
				//println("if (0 == inputState.guessing)");
				//println("{");
				//tabs++;
			}

			// if something has a label assume it will be used
        	// so we must initialize the RefAST
			if (el.getLabel() != null)
			{
				if ( el instanceof GrammarAtom )
				{
					println(astName + " = "+ getASTCreateString((GrammarAtom)el, elementRef) + ";");
				}
				else
				{
					println(astName + " = "+ getASTCreateString(elementRef) + ";");
				}
			}

			// if it has no label but a declaration exists initialize it.
        	if (el.getLabel() == null && needASTDecl)
			{
				elementRef = lt1Value;
				if ( el instanceof GrammarAtom )
				{
					println(astName + " = "+ getASTCreateString((GrammarAtom)el, elementRef) + ";");
				}
				else
				{
					println(astName + " = "+ getASTCreateString(elementRef) + ";");
				}
				// Map the generated AST variable in the alternate
				if (grammar instanceof TreeWalkerGrammar)
				{
					// set "input" AST variable also
					println(astName + "_in = " + elementRef + ";");
				}
			}

			if (genAST)
			{
				switch (el.getAutoGenType())
				{
				case GrammarElement.AUTO_GEN_NONE:
					if ( usingCustomAST ||
						 ( (el instanceof GrammarAtom) &&
                           (((GrammarAtom)el).getASTNodeType() != null) ) )
						println("astFactory.addASTChild(currentAST, (AST)" + astName + ");");
					else
						println("astFactory.addASTChild(currentAST, " + astName + ");");
					break;
				case GrammarElement.AUTO_GEN_CARET:
					if ( usingCustomAST ||
						 ( (el instanceof GrammarAtom) &&
                           (((GrammarAtom)el).getASTNodeType() != null) ) )
						println("astFactory.makeASTRoot(currentAST, (AST)" + astName + ");");
					else
						println("astFactory.makeASTRoot(currentAST, " + astName + ");");
					break;
				default:
					break;
				}
			}
			if (doNoGuessTest)
			{
				//tabs--;
				//println("}");
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
			r = CodeGenerator.encodeLexerRuleName(el.enclosingRuleName);
		}
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
		if (rs == null) {
			antlrTool.panic("Enclosing rule not found!");
		}
		ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
		if (ex != null) {
			tabs--;
			println("}");
			genErrorHandler(ex);
		}
	}

	/** Generate the catch phrases for a user-specified error handler */
	private void genErrorHandler(ExceptionSpec ex)
	{
		// Each ExceptionHandler in the ExceptionSpec is a separate catch
		for (int i = 0; i < ex.handlers.size(); i++)
		{
			ExceptionHandler handler = (ExceptionHandler)ex.handlers.elementAt(i);
			// Generate catch phrase
			println("catch (" + handler.exceptionTypeAndName.getText() + ")");
			println("{");
			tabs++;
			if (grammar.hasSyntacticPredicate) {
				println("if (0 == inputState.guessing)");
				println("{");
				tabs++;
			}

		// When not guessing, execute user handler action
		ActionTransInfo tInfo = new ActionTransInfo();
        printAction(processActionForSpecialSymbols(handler.action.getText(),
         					handler.action.getLine(), currentRule, tInfo));

			if (grammar.hasSyntacticPredicate)
			{
				tabs--;
				println("}");
				println("else");
				println("{");
				tabs++;
				// When guessing, rethrow exception
				//println("throw " + extractIdOfAction(handler.exceptionTypeAndName) + ";");
				println("throw;");
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
			r = CodeGenerator.encodeLexerRuleName(el.enclosingRuleName);
		}
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
		if (rs == null) {
			antlrTool.panic("Enclosing rule not found!");
		}
		ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
		if (ex != null) {
			println("try   // for error handling");
			println("{");
			tabs++;
		}
	}

    protected void genASTDeclaration(AlternativeElement el)
    {
        genASTDeclaration(el, labeledElementASTType);
    }

    protected void genASTDeclaration(AlternativeElement el, String node_type)
    {
        genASTDeclaration(el, el.getLabel(), node_type);
    }

    protected void genASTDeclaration(AlternativeElement el, String var_name, String node_type)
    {
        // already declared?
        if (declaredASTVariables.contains(el))
            return;

        // emit code
        //String s = StringUtils.stripFrontBack(node_type, "\"", "\"");
        //println(s + " " + var_name + "_AST = null;");
        println(node_type + " " + var_name + "_AST = null;");

        // mark as declared
        declaredASTVariables.put(el,el);
    }

	/** Generate a header that is common to all CSharp files */
	protected void genHeader()
	{
		println("// $ANTLR "+Tool.version+": "+
			"\"" + antlrTool.fileMinusPath(antlrTool.grammarFile) + "\"" +
			" -> "+
			"\""+grammar.getClassName()+".cs\"$");
	}

	private void genLiteralsTest() {
		println("_ttype = testLiteralsTable(_ttype);");
	}

	private void genLiteralsTestForPartialToken() {
		println("_ttype = testLiteralsTable(text.ToString(_begin, text.Length-_begin), _ttype);");
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
				antlrTool.error("cannot ref character literals in grammar: "+atom);
			}
		}
		else if ( atom instanceof TokenRefElement ) {
			genMatchUsingAtomText(atom);
		} else if (atom instanceof WildcardElement) {
          gen((WildcardElement)atom);
      }
	}
	protected void genMatchUsingAtomText(GrammarAtom atom) {
		// match() for trees needs the _t cursor
		String astArgs="";
		if (grammar instanceof TreeWalkerGrammar) {
			if ( usingCustomAST )
				astArgs="(AST)_t,";
			else
				astArgs="_t,";
		}

		// if in lexer and ! on element, save buffer index to kill later
		if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			declareSaveIndexVariableIfNeeded();
			println("_saveIndex = text.Length;");
		}

		print(atom.not ? "matchNot(" : "match(");
		_print(astArgs);

		// print out what to match
		if (atom.atomText.equals("EOF")) {
			// horrible hack to handle EOF case
			_print("Token.EOF_TYPE");
		}
		else {
				_print(atom.atomText);
		}
		_println(");");

		if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
			declareSaveIndexVariableIfNeeded();
			println("text.Length = _saveIndex;");		// kill text atom put in buffer
		}
	}

	protected void genMatchUsingAtomTokenType(GrammarAtom atom) {
		// match() for trees needs the _t cursor
		String astArgs="";
		if (grammar instanceof TreeWalkerGrammar) {
			if( usingCustomAST )
				astArgs="(AST)_t,";
			else
				astArgs="_t,";
		}

		// If the literal can be mangled, generate the symbolic constant instead
		String mangledName = null;
		String s = astArgs + getValueString(atom.getType());

		// matching
		println( (atom.not ? "matchNot(" : "match(") + s + ");");
	}

	/** Generate the nextToken() rule.  nextToken() is a synthetic
	* lexer rule that is the implicit OR of all user-defined
	* lexer rules.
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
			println("override public Token nextToken()\t\t\t//throws TokenStreamException");
			println("{");
			tabs++;
			println("try");
			println("{");
			tabs++;
			println("uponEOF();");
			tabs--;
			println("}");
			println("catch(CharStreamIOException csioe)");
			println("{");
			tabs++;
			println("throw new TokenStreamIOException(csioe.io);");
			tabs--;
			println("}");
			println("catch(CharStreamException cse)");
			println("{");
			tabs++;
			println("throw new TokenStreamException(cse.Message);");
			tabs--;
			println("}");
			println("return new CommonToken(Token.EOF_TYPE, \"\");");
			tabs--;
			println("}");
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
		println("override public Token nextToken()\t\t\t//throws TokenStreamException");
		println("{");
		tabs++;
		println("Token theRetToken = null;");
		_println("tryAgain:");
		println("for (;;)");
		println("{");
		tabs++;
		println("Token _token = null;");
		println("int _ttype = Token.INVALID_TYPE;");
		if ( ((LexerGrammar)grammar).filterMode ) {
			println("setCommitToPath(false);");
			if ( filterRule!=null ) {
				// Here's a good place to ensure that the filter rule actually exists
            if (!grammar.isDefined(CodeGenerator.encodeLexerRuleName(filterRule))) {
            	grammar.antlrTool.error("Filter rule " + filterRule + " does not exist in this lexer");
				}
				else {
					RuleSymbol rs = (RuleSymbol)grammar.getSymbol(CodeGenerator.encodeLexerRuleName(filterRule));
					if ( !rs.isDefined() ) {
						grammar.antlrTool.error("Filter rule " + filterRule + " does not exist in this lexer");
					}
					else if ( rs.access.equals("public") ) {
						grammar.antlrTool.error("Filter rule " + filterRule + " must be protected");
					}
				}
				println("int _m;");
				println("_m = mark();");
			}
		}
		println("resetText();");

		println("try     // for char stream error handling");
		println("{");
		tabs++;

		// Generate try around whole thing to trap scanner errors
		println("try     // for lexical error handling");
		println("{");
		tabs++;

		// Test for public lexical rules with empty paths
		for (int i=0; i<nextTokenBlk.getAlternatives().size(); i++) {
			Alternative a = nextTokenBlk.getAlternativeAt(i);
			if ( a.cache[1].containsEpsilon() ) {
				//String r = a.head.toString();
            RuleRefElement rr = (RuleRefElement)a.head;
            String r = CodeGenerator.decodeLexerRuleName(rr.targetRule);
            antlrTool.warning("public lexical rule "+r+" is optional (can match \"nothing\")");
			}
		}

		// Generate the block
		String newline = System.getProperty("line.separator");
		CSharpBlockFinishingInfo howToFinish = genCommonBlock(nextTokenBlk, false);
		String errFinish = "if (LA(1)==EOF_CHAR) { uponEOF(); returnToken_ = makeToken(Token.EOF_TYPE); }";
		errFinish += newline+"\t\t\t\t";
		if ( ((LexerGrammar)grammar).filterMode ) {
			if ( filterRule==null ) {
			//kunle: errFinish += "else { consume(); continue tryAgain; }";
			errFinish += "\t\t\t\telse";
			errFinish += "\t\t\t\t{";
			errFinish += "\t\t\t\t\tconsume();";
			errFinish += "\t\t\t\t\tgoto tryAgain;";
			errFinish += "\t\t\t\t}";
			}
			else {
				errFinish += "\t\t\t\t\telse"+newline+
					"\t\t\t\t\t{"+newline+
					"\t\t\t\t\tcommit();"+newline+
					"\t\t\t\t\ttry {m"+filterRule+"(false);}"+newline+
					"\t\t\t\t\tcatch(RecognitionException e)"+newline+
					"\t\t\t\t\t{"+newline+
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
		// make sure _ttype is set first; note returnToken_ must be
		// non-null as the rule was required to create it.
		println("if ( null==returnToken_ ) goto tryAgain; // found SKIP token");
		println("_ttype = returnToken_.Type;");
		if ( ((LexerGrammar)grammar).getTestLiterals()) {
			genLiteralsTest();
		}

		// return token created by rule reference in switch
		println("returnToken_.Type = _ttype;");
		println("return returnToken_;");

		// Close try block
		tabs--;
		println("}");
		println("catch (RecognitionException e) {");
		tabs++;
		if ( ((LexerGrammar)grammar).filterMode ) {
			if ( filterRule==null ) {
				println("if (!getCommitToPath())");
				println("{");
				tabs++;
				println("consume();");
				println("goto tryAgain;");
				tabs--;
				println("}");
			}
			else {
				println("if (!getCommitToPath())");
				println("{");
				tabs++;
				println("rewind(_m);");
				println("resetText();");
				println("try {m"+filterRule+"(false);}");
				println("catch(RecognitionException ee) {");
				println("	// horrendous failure: error in filter rule");
				println("	reportError(ee);");
				println("	consume();");
				println("}");
				//println("goto tryAgain;");
				tabs--;
				println("}");
				println("else");
			}
		}
		if ( nextTokenBlk.getDefaultErrorHandler() ) {
			println("{");
			tabs++;
			println("reportError(e);");
			println("consume();");
			tabs--;
			println("}");
		}
		else {
			// pass on to invoking routine
			tabs++;
			println("throw new TokenStreamRecognitionException(e);");
			tabs--;
		}
		tabs--;
		println("}");

		// close CharStreamException try
		tabs--;
		println("}");
		println("catch (CharStreamException cse) {");
		println("	if ( cse is CharStreamIOException ) {");
		println("		throw new TokenStreamIOException(((CharStreamIOException)cse).io);");
		println("	}");
		println("	else {");
		println("		throw new TokenStreamException(cse.Message);");
		println("	}");
		println("}");

		// close for-loop
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
	public void genRule(RuleSymbol s, boolean startSymbol, int ruleNum, TokenManager tm) {
		tabs=1;
		if ( DEBUG_CODE_GENERATOR ) System.out.println("genRule("+ s.getId() +")");
		if ( !s.isDefined() ) {
			antlrTool.error("undefined rule: "+ s.getId());
			return;
		}

		// Generate rule return type, name, arguments
		RuleBlock rblk = s.getBlock();
		currentRule = rblk;
		currentASTResult = s.getId();

      // clear list of declared ast variables..
      declaredASTVariables.clear();

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
		//print(s.access + " final ");
		print(s.access + " ");

		// Gen method return type (note lexer return action set at rule creation)
		if (rblk.returnAction != null)
		{
			// Has specified return value
			_print(extractTypeOfAction(rblk.returnAction, rblk.getLine(), rblk.getColumn()) + " ");
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
		}
		else {
			// No specified arguments
			_print(")");
		}

		// Gen throws clause and open curly
		_print(" //throws " + exceptionThrown);
		if ( grammar instanceof ParserGrammar ) {
			_print(", TokenStreamException");
		}
		else if ( grammar instanceof LexerGrammar ) {
			_print(", CharStreamException, TokenStreamException");
		}
		// Add user-defined exceptions unless lexer (for now)
		if ( rblk.throwsSpec!=null ) {
			if ( grammar instanceof LexerGrammar ) {
				antlrTool.error("user-defined throws spec not allowed (yet) for lexer rule "+rblk.ruleName);
			}
			else {
				_print(", "+rblk.throwsSpec);
			}
		}

		_println("");
		_println("{");
		tabs++;

		// Convert return action to variable declaration
		if (rblk.returnAction != null)
			println(rblk.returnAction + ";");

		// print out definitions needed by rules for various grammar types
		println(commonLocalVars);

		if (grammar.traceRules) {
			if ( grammar instanceof TreeWalkerGrammar ) {
				if ( usingCustomAST )
					println("traceIn(\""+ s.getId() +"\",(AST)_t);");
				else
					println("traceIn(\""+ s.getId() +"\",_t);");
			}
			else {
				println("traceIn(\""+ s.getId() +"\");");
			}
		}

		if ( grammar instanceof LexerGrammar ) {
			// lexer rule default return value is the rule's token name
			// This is a horrible hack to support the built-in EOF lexer rule.
			if (s.getId().equals("mEOF"))
				println("_ttype = Token.EOF_TYPE;");
			else
				println("_ttype = " + s.getId().substring(1)+";");

			// delay creation of _saveIndex until we need it OK?
			bSaveIndexCreated = false;

			/*
			      println("boolean old_saveConsumedInput=saveConsumedInput;");
			      if ( !rblk.getAutoGen() ) {		// turn off "save input" if ! on rule
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
		if ( grammar.debuggingOutput || grammar.traceRules) {
			println("try { // debugging");
			tabs++;
		}

		// Initialize AST variables
		if (grammar instanceof TreeWalkerGrammar) {
			// "Input" value for rule
			println(labeledElementASTType+" " + s.getId() + "_AST_in = ("+labeledElementASTType+")_t;");
		}
		if (grammar.buildAST) {
			// Parser member used to pass AST returns from rule invocations
			println("returnAST = null;");
			// Tracks AST construction
			// println("ASTPair currentAST = (inputState.guessing==0) ? new ASTPair() : null;");
			println("ASTPair currentAST = new ASTPair();");
			// User-settable return value for rule.
			println(labeledElementASTType+" " + s.getId() + "_AST = null;");
		}

		genBlockPreamble(rblk);
		genBlockInitAction(rblk);
		println("");

		// Search for an unlabeled exception specification attached to the rule
		ExceptionSpec unlabeledUserSpec = rblk.findExceptionSpec("");

		// Generate try block around the entire rule for  error handling
		if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
			println("try {      // for error handling");
			tabs++;
		}

		// Generate the alternatives
		if ( rblk.alternatives.size()==1 )
		{
			// One alternative -- use simple form
			Alternative alt = rblk.getAlternativeAt(0);
			String pred = alt.semPred;
			if ( pred!=null )
				genSemPred(pred, currentRule.line);
			if (alt.synPred != null) {
				antlrTool.warning(
					"Syntactic predicate ignored for single alternative",
					grammar.getFilename(), alt.synPred.getLine(), alt.synPred.getColumn()
					);
			}
			genAlt(alt, rblk);
		}
		else
		{
			// Multiple alternatives -- generate complex form
			boolean ok = grammar.theLLkAnalyzer.deterministic(rblk);

			CSharpBlockFinishingInfo howToFinish = genCommonBlock(rblk, false);
			genBlockFinish(howToFinish, throwNoViable);
		}

		// Generate catch phrase for error handling
		if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
			// Close the try block
			tabs--;
			println("}");
		}

		// Generate user-defined or default catch phrases
		if (unlabeledUserSpec != null)
		{
			genErrorHandler(unlabeledUserSpec);
		}
		else if (rblk.getDefaultErrorHandler())
		{
			// Generate default catch phrase
			println("catch (" + exceptionThrown + " ex)");
			println("{");
			tabs++;
			// Generate code to handle error if not guessing
			if (grammar.hasSyntacticPredicate) {
				println("if (0 == inputState.guessing)");
				println("{");
				tabs++;
			}
			println("reportError(ex);");
			if ( !(grammar instanceof TreeWalkerGrammar) )
			{
				// Generate code to consume until token in k==1 follow set
				Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(1, rblk.endNode);
				String followSetName = getBitsetName(markBitsetForGen(follow.fset));
				println("consume();");
				println("consumeUntil(" + followSetName + ");");
			}
			else
			{
				// Just consume one token
			println("if (null != _t)");
			println("{");
			tabs++;
			println("_t = _t.getNextSibling();");
			tabs--;
			println("}");
			}
			if (grammar.hasSyntacticPredicate)
			{
				tabs--;
				// When guessing, rethrow exception
				println("}");
				println("else");
				println("{");
				tabs++;
				//println("throw ex;");
				println("throw;");
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
			println("retTree_ = _t;");
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
			println("if (_createToken && (null == _token) && (_ttype != Token.SKIP))");
			println("{");
			tabs++;
			println("_token = makeToken(_ttype);");
			println("_token.setText(text.ToString(_begin, text.Length-_begin));");
			tabs--;
			println("}");
			println("returnToken_ = _token;");
		}

		// Gen the return statement if there is one (lexer has hard-wired return action)
		if (rblk.returnAction != null) {
			println("return " + extractIdOfAction(rblk.returnAction, rblk.getLine(), rblk.getColumn()) + ";");
		}

		if ( grammar.debuggingOutput || grammar.traceRules) {
			tabs--;
			println("}");
			println("finally");
			println("{ // debugging");
			tabs++;

			// If debugging, generate calls to mark exit of rule
			if ( grammar.debuggingOutput)
				if (grammar instanceof ParserGrammar)
					println("fireExitRule(" + ruleNum + ",0);");
				else if (grammar instanceof LexerGrammar)
				println("fireExitRule(" + ruleNum + ",_ttype);");

			if (grammar.traceRules) {
				if ( grammar instanceof TreeWalkerGrammar ) {
					println("traceOut(\""+ s.getId() +"\",_t);");
				}
				else {
					println("traceOut(\""+ s.getId() +"\");");
				}
			}

			tabs--;
			println("}");
		}

		tabs--;
		println("}");
		println("");

		// Restore the AST generation state
		genAST = savegenAST;

		// restore char save state
		// saveText = oldsaveTest;
	}
	private void GenRuleInvocation(RuleRefElement rr) {
		// dump rule name
		_print(rr.targetRule + "(");

		// lexers must tell rule if it should set returnToken_
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
			String args = processActionForSpecialSymbols(rr.args, 0, currentRule, tInfo);
			if ( tInfo.assignToRoot || tInfo.refRuleRoot!=null )
			{
            antlrTool.error("Arguments of rule reference '" + rr.targetRule + "' cannot set or ref #" +
                 currentRule.getRuleName(), grammar.getFilename(), rr.getLine(), rr.getColumn());
			}
			_print(args);

			// Warn if the rule accepts no arguments
			if (rs.block.argAction == null)
			{
				antlrTool.warning("Rule '" + rr.targetRule + "' accepts no arguments", grammar.getFilename(), rr.getLine(), rr.getColumn());
			}
		}
		else
		{
			// For C++, no warning if rule has parameters, because there may be default
			// values for all of the parameters
			if (rs.block.argAction != null)
			{
				antlrTool.warning("Missing parameters on reference to rule " + rr.targetRule, grammar.getFilename(), rr.getLine(), rr.getColumn());
			}
		}
		_println(");");

		// move down to the first child while parsing
		if ( grammar instanceof TreeWalkerGrammar ) {
			println("_t = retTree_;");
		}
	}
	protected void genSemPred(String pred, int line) {
		// translate $ and # references
		ActionTransInfo tInfo = new ActionTransInfo();
		pred = processActionForSpecialSymbols(pred, line, currentRule, tInfo);
		// ignore translation info...we don't need to do anything with it.
		String escapedPred = charFormatter.escapeString(pred);

		// if debugging, wrap the semantic predicate evaluation in a method
		// that can tell SemanticPredicateListeners the result
		if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) || (grammar instanceof LexerGrammar)))
			pred = "fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.VALIDATING,"
			+ addSemPred(escapedPred) + "," + pred + ")";
		println("if (!(" + pred + "))");
		println("  throw new SemanticException(\"" + escapedPred + "\");");
	}
	/** Write an array of Strings which are the semantic predicate
	 *  expressions.  The debugger will reference them by number only
	 */
	protected void genSemPredMap() {
		Enumeration e = semPreds.elements();
		println("private string[] _semPredNames = {");
		tabs++;
		while(e.hasMoreElements())
			println("\""+e.nextElement()+"\",");
		tabs--;
		println("};");
	}
	protected void genSynPred(SynPredBlock blk, String lookaheadExpr) {
		if ( DEBUG_CODE_GENERATOR ) System.out.println("gen=>("+blk+")");

		// Dump synpred result variable
		println("bool synPredMatched" + blk.ID + " = false;");
		// Gen normal lookahead test
		println("if (" + lookaheadExpr + ")");
		println("{");
		tabs++;

		// Save input state
		if ( grammar instanceof TreeWalkerGrammar ) {
			println("AST __t" + blk.ID + " = _t;");
		}
		else {
			println("int _m" + blk.ID + " = mark();");
		}

		// Once inside the try, assume synpred works unless exception caught
		println("synPredMatched" + blk.ID + " = true;");
		println("inputState.guessing++;");

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
		//kunle: lose a few warnings cheaply
		//  println("catch (" + exceptionThrown + " pe)");
		println("catch (" + exceptionThrown + ")");
		println("{");
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

		println("inputState.guessing--;");

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

		// Test synred result
		println("if ( synPredMatched"+blk.ID+" )");
		println("{");
	}
	/** Generate a static array containing the names of the tokens,
	 * indexed by the token type values.  This static array is used
	 * to format error messages so that the token identifers or literal
	 * strings are displayed instead of the token numbers.
	 *
	 * If a lexical rule has a paraphrase, use it rather than the
	 * token label.
	 */
	public void genTokenStrings() {
		// Generate a string for each token.  This creates a static
		// array of Strings indexed by token type.
		println("");
		println("public static readonly string[] tokenNames_ = new string[] {");
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
					s = StringUtils.stripFrontBack(ts.getParaphrase(), "\"", "\"");
				}
			}
			else if (s.startsWith("\"")) {
				s = StringUtils.stripFrontBack(s, "\"", "\"");
			}
			print(charFormatter.literalString(s));
			if (i != v.size()-1) {
				_print(",");
			}
			_println("");
		}

		// Close the string array initailizer
		tabs--;
		println("};");
	}
	/** Generate the token types CSharp file */
	protected void genTokenTypes(TokenManager tm) throws IOException {
		// Open the token output CSharp file and set the currentOutput stream
		// SAS: file open was moved to a method so a subclass can override
		//      This was mainly for the VAJ interface
		setupOutput(tm.getName() + TokenTypesFileSuffix);

		tabs = 0;

		// Generate the header common to all CSharp files
		genHeader();
		// Do not use printAction because we assume tabs==0
		println(behavior.getHeaderAction(""));

	      // Generate the CSharp namespace declaration (if specified)
		if (nameSpace != null)
			nameSpace.emitDeclarations(currentOutput);
		tabs++;

		// Encapsulate the definitions in a class.  This has to be done as a class because
		// they are all constants and CSharp inteface  types cannot contain constants.
		println("public class " + tm.getName() + TokenTypesFileSuffix);
		//println("public class " + getTokenTypesClassName());
		println("{");
		tabs++;

		genTokenDefinitions(tm);

		// Close the interface
		tabs--;
		println("}");

		tabs--;
		// Generate the CSharp namespace closures (if required)
		if (nameSpace != null)
			nameSpace.emitClosures(currentOutput);

		// Close the tokens output file
		currentOutput.close();
		currentOutput = null;
		exitIfError();
	}
	protected void genTokenDefinitions(TokenManager tm) throws IOException {
		// Generate a definition for each token type
		Vector v = tm.getVocabulary();

		// Do special tokens manually
		println("public const int EOF = " + Token.EOF_TYPE + ";");
		println("public const int NULL_TREE_LOOKAHEAD = " + Token.NULL_TREE_LOOKAHEAD + ";");

		for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
			String s = (String)v.elementAt(i);
			if (s != null) {
				if ( s.startsWith("\"") ) {
					// a string literal
					StringLiteralSymbol sl = (StringLiteralSymbol)tm.getTokenSymbol(s);
					if ( sl==null ) {
						antlrTool.panic("String literal " + s + " not in symbol table");
					}
					else if ( sl.label != null ) {
						println("public const int " + sl.label + " = " + i + ";");
					}
					else {
						String mangledName = mangleLiteral(s);
						if (mangledName != null) {
							// We were able to create a meaningful mangled token name
							println("public const int " + mangledName + " = " + i + ";");
							// if no label specified, make the label equal to the mangled name
							sl.label = mangledName;
						}
						else {
							println("// " + s + " = " + i);
						}
					}
				}
				else if ( !s.startsWith("<") ) {
					println("public const int " + s + " = " + i + ";");
				}
			}
		}
		println("");
	}
	/** Process a string for an simple expression for use in xx/action.g
	 * it is used to cast simple tokens/references to the right type for
	 * the generated language. Basically called for every element in
	 * the vector to getASTCreateString(vector V)
	 * @param str A String.
	 */
	public String processStringForASTConstructor( String str )
	{
		/*
		System.out.println("processStringForASTConstructor: str = "+str+
		                   ", custom = "+(new Boolean(usingCustomAST)).toString()+
		                   ", tree = "+(new Boolean((grammar instanceof TreeWalkerGrammar))).toString()+
		                   ", parser = "+(new Boolean((grammar instanceof ParserGrammar))).toString()+
		                   ", notDefined = "+(new Boolean((!(grammar.tokenManager.tokenDefined(str))))).toString()
		                   );
		*/
		if( usingCustomAST &&
			( (grammar instanceof TreeWalkerGrammar)	||
			  (grammar instanceof ParserGrammar) )		&&
			!(grammar.tokenManager.tokenDefined(str)) )
		{
			//System.out.println("processStringForASTConstructor: "+str+" with cast");
			return "(AST)"+str;
		}
		else
		{
			//System.out.println("processStringForASTConstructor: "+str);
			return str;
		}
	}
	/** Get a string for an expression to generate creation of an AST subtree.
	  * @param v A Vector of String, where each element is an expression
	  *          in the target language yielding an AST node.
	  */
	public String getASTCreateString(Vector v) {
		if (v.size() == 0) {
			return "";
		}
		StringBuffer buf = new StringBuffer();
		buf.append("("+labeledElementASTType+
			")astFactory.make( (new ASTArray(" + v.size() +
			"))");
		for (int i = 0; i < v.size(); i++) {
			buf.append(".add(" + v.elementAt(i) + ")");
		}
		buf.append(")");
		return buf.toString();
	}

	/** Get a string for an expression to generate creating of an AST node
	 * @param atom The grammar node for which you are creating the node
	 * @param str The arguments to the AST constructor
	 */
	public String getASTCreateString(GrammarAtom atom, String astCtorArgs) {
		String astCreateString = "astFactory.create(" + astCtorArgs + ")";

		if (atom == null)
			return getASTCreateString(astCtorArgs);
		else {
			if ( atom.getASTNodeType() != null ) {
				// this Atom was instantiated from a Token that had an "AST" option - associating
				// it with a specific heterogeneous AST type - applied to either:
				// 1) it's underlying TokenSymbol (in the "tokens {} section" or,
                // 2) a particular token reference in the grammar
                //
				// For option (1), we simply generate a cast to hetero-AST type
				// For option (2), we generate a call to factory.create(Token, ASTNodeType) and cast it too
                TokenSymbol ts = grammar.tokenManager.getTokenSymbol(atom.getText());
                if ( (ts == null) || (ts.getASTNodeType() != atom.getASTNodeType()) )
				    astCreateString = "(" + atom.getASTNodeType() + ") astFactory.create(" + astCtorArgs + ", \"" + atom.getASTNodeType() + "\")";
                else if ( (ts != null) && (ts.getASTNodeType() != null) )
                    astCreateString = "(" + ts.getASTNodeType() + ") " + astCreateString;
			}
			else if ( usingCustomAST )
				astCreateString = "(" + labeledElementASTType + ") " + astCreateString;
		}
		return astCreateString;
	}

    /** Returns a string expression that creates an AST node using the specified
     *  AST constructor argument string.
	 *  Parses the first (possibly only) argument in the supplied AST ctor argument
	 *	string to obtain the token type -- ctorID.
	 *
	 *  IF the token type is a valid token symbol AND
	 *	   it has an associated AST node type     AND
	 *	   this is not a #[ID, "T", "ASTType"] constructor
	 *	THEN
	 *	   generate a call to factory.create(ID, Text, token.ASTNodeType())
	 *
	 *  #[ID, "T", "ASTType"] constructors are mapped to astFactory.create(ID, "T", "ASTType")
	 *
	 *  The supported AST constructor forms are:
	 *		#[ID]
	 *		#[ID, "text"]
	 *  	#[ID, "text", ASTclassname]	-- introduced in 2.7.2
	 *
     * @param astCtorArgs The arguments to the AST constructor
     */
	public String getASTCreateString(String astCtorArgs) {
		// kunle: 19-Aug-2002
		// This AST creation string is almost certainly[*1] a manual tree construction request.
		// From the manual [I couldn't read ALL of the code ;-)], this can only be one of:
		// 1) #[ID]                     -- 'astCtorArgs' contains: 'ID'                     (without quotes)    or,
		// 2) #[ID, "T"]                -- 'astCtorArgs' contains: 'ID, "Text"'             (without single quotes) or,
		// kunle: 08-Dec-2002 - 2.7.2a6
		// 3) #[ID, "T", "ASTTypeName"] -- 'astCtorArgs' contains: 'ID, "T", "ASTTypeName"' (without single quotes)
		//
		// [*1]  In my tests, 'atom' was '== null' only for manual tree construction requests

		if ( astCtorArgs==null ) {
			astCtorArgs = "";
		}
		String astCreateString 	= "astFactory.create(" + astCtorArgs + ")";
		String  ctorID   	 	= astCtorArgs;
		String	ctorText 	 	= null;
		int		commaIndex;
		boolean	ctorIncludesCustomType = false;		// Is this a #[ID, "t", "ASTType"] constructor?

		commaIndex = astCtorArgs.indexOf(',');
		if ( commaIndex != -1 ) {
			ctorID   = astCtorArgs.substring(0, commaIndex);					// the 'ID'   portion of #[ID, "Text"]
			ctorText = astCtorArgs.substring(commaIndex+1, astCtorArgs.length());	// the 'Text' portion of #[ID, "Text"]
			commaIndex = ctorText.indexOf(',');
			if (commaIndex != -1 ) {
				// This is an AST creation of the form: #[ID, "Text", "ASTTypename"]
				// Support for this was introduced with 2.7.2a6
				// create default type or (since 2.7.2) 3rd arg is classname
				ctorIncludesCustomType = true;
			}
		}
		TokenSymbol ts = grammar.tokenManager.getTokenSymbol(ctorID);
		if ( (null != ts) && (null != ts.getASTNodeType()) )
			astCreateString = "(" + ts.getASTNodeType() + ") " + astCreateString;
		else if ( usingCustomAST )
			astCreateString = "(" + labeledElementASTType + ") " + astCreateString;

		return astCreateString;
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

	/**Generate a lookahead test expression for an alternate.  This
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
			return "( true )";
		}
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
			antlrTool.panic("getRangeExpression called with non-range");
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
		else
		{
			TokenSymbol ts = grammar.tokenManager.getTokenSymbolAt(value);
			if ( ts == null ) {
				return ""+value; // return token type as string
				// antlrTool.panic("vocabulary for token type " + value + " is null");
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
			return "_t.Type";
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
		String mangled = antlrTool.literalsPrefix;
		for (int i = 1; i < s.length()-1; i++) {
			if (!Character.isLetter(s.charAt(i)) &&
				s.charAt(i) != '_') {
				return null;
			}
			mangled += s.charAt(i);
		}
		if ( antlrTool.upperCaseMangledLiterals ) {
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

		boolean in_var = false;
		String id = idParam;
		if (grammar instanceof TreeWalkerGrammar)
		{
			if ( !grammar.buildAST )
			{
				in_var = true;
			}
			// If the id ends with "_in", then map it to the input variable
			else if (id.length() > 3 && id.lastIndexOf("_in") == id.length()-3)
			{
				// Strip off the "_in"
				id = id.substring(0, id.length()-3);
				in_var = true;
			}
		}

		// Check the rule labels.  If id is a label, then the output
		// variable is label_AST, and the input variable is plain label.
		for (int i = 0; i < currentRule.labeledElements.size(); i++)
		{
			AlternativeElement elt = (AlternativeElement)currentRule.labeledElements.elementAt(i);
			if (elt.getLabel().equals(id))
			{
				return in_var ? id : id + "_AST";
			}
		}

		// Failing that, check the id-to-variable map for the alternative.
		// If the id is in the map, then output variable is the name in the
		// map, and input variable is name_in
		String s = (String)treeVariableMap.get(id);
		if (s != null)
		{
			if (s == NONUNIQUE)
			{
				// There is more than one element with this id
				antlrTool.error("Ambiguous reference to AST element "+id+
								" in rule "+currentRule.getRuleName());
				return null;
			}
			else if (s.equals(currentRule.getRuleName()))
			{
				// a recursive call to the enclosing rule is
				// ambiguous with the rule itself.
//				if( in_var )
//					System.out.println("returning null (rulename)");
				antlrTool.error("Ambiguous reference to AST element "+id+
								" in rule "+currentRule.getRuleName());
				return null;
			}
			else
			{
				return in_var ? s + "_in" : s;
			}
		}

		// Failing that, check the rule name itself.  Output variable
		// is rule_AST; input variable is rule_AST_in (treeparsers).
		if( id.equals(currentRule.getRuleName()) )
		{
			String r = in_var ? id + "_AST_in" : id + "_AST";
			if ( transInfo!=null ) {
				if ( !in_var ) {
					transInfo.refRuleRoot = r;
				}
			}
			return r;
		}
		else
		{
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
    protected String processActionForSpecialSymbols(String actionStr,
                                                    int line,
                                                    RuleBlock currentRule,
                                                    ActionTransInfo tInfo)
	{
		if ( actionStr==null || actionStr.length()==0 )
			return null;

        // The action trans info tells us (at the moment) whether an
        // assignment was done to the rule's tree root.
        if (grammar==null)
            return actionStr;

        // see if we have anything to do...
        if ((grammar.buildAST && actionStr.indexOf('#') != -1) ||
            grammar instanceof TreeWalkerGrammar ||
            ((grammar instanceof LexerGrammar ||
            grammar instanceof ParserGrammar)
			  	&& actionStr.indexOf('$') != -1) )
		{
            // Create a lexer to read an action and return the translated version
            antlr.actions.csharp.ActionLexer lexer = new antlr.actions.csharp.ActionLexer(actionStr, currentRule, this, tInfo);

            lexer.setLineOffset(line);
            lexer.setFilename(grammar.getFilename());
            lexer.setTool(antlrTool);

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
                antlrTool.panic("Error reading action:"+actionStr);
                return actionStr;
            }
            catch (CharStreamException io) {
                antlrTool.panic("Error reading action:"+actionStr);
                return actionStr;
            }
        }
        return actionStr;
    }

	private void setupGrammarParameters(Grammar g) {
		if (g instanceof ParserGrammar ||
			 g instanceof LexerGrammar  ||
			 g instanceof TreeWalkerGrammar
			)
		{
			/* RK: options also have to be added to Grammar.java and for options
			 * on the file level entries have to be defined in
			 * DefineGrammarSymbols.java and passed around via 'globals' in antlrTool.java
			 */
			if( antlrTool.nameSpace != null )
				nameSpace = new CSharpNameSpace( antlrTool.nameSpace.getName() );
			//genHashLines = antlrTool.genHashLines;

			/* let grammar level options override filelevel ones...
			 */
			if( g.hasOption("namespace") ) {
				Token t = g.getOption("namespace");
				if( t != null ) {
					nameSpace = new CSharpNameSpace(t.getText());
				}
			}
			/*
			if( g.hasOption("genHashLines") ) {
				Token t = g.getOption("genHashLines");
				if( t != null )  {
					String val = StringUtils.stripFrontBack(t.getText(),"\"","\"");
					genHashLines = val.equals("true");
				}
			}
			*/
		}

		if (g instanceof ParserGrammar) {
			labeledElementASTType = "AST";
			if ( g.hasOption("ASTLabelType") ) {
				Token tsuffix = g.getOption("ASTLabelType");
				if ( tsuffix != null ) {
					String suffix = StringUtils.stripFrontBack(tsuffix.getText(), "\"", "\"");
					if ( suffix != null ) {
						usingCustomAST = true;
						labeledElementASTType = suffix;
					}
				}
			}
			labeledElementType = "Token ";
			labeledElementInit = "null";
			commonExtraArgs = "";
			commonExtraParams = "";
			commonLocalVars = "";
			lt1Value = "LT(1)";
			exceptionThrown = "RecognitionException";
			throwNoViable = "throw new NoViableAltException(LT(1), getFilename());";
		}
		else if (g instanceof LexerGrammar) {
			labeledElementType = "char ";
			labeledElementInit = "'\\0'";
			commonExtraArgs = "";
			commonExtraParams = "bool _createToken";
			commonLocalVars = "int _ttype; Token _token=null; int _begin=text.Length;";
			lt1Value = "LA(1)";
			exceptionThrown = "RecognitionException";
			throwNoViable = "throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());";
		}
		else if (g instanceof TreeWalkerGrammar) {
			labeledElementASTType = "AST";
			labeledElementType = "AST";
			if ( g.hasOption("ASTLabelType") ) {
				Token tsuffix = g.getOption("ASTLabelType");
				if ( tsuffix != null ) {
					String suffix = StringUtils.stripFrontBack(tsuffix.getText(), "\"", "\"");
					if ( suffix != null ) {
						usingCustomAST = true;
						labeledElementASTType = suffix;
						labeledElementType = suffix;
					}
				}
			}
			if ( !g.hasOption("ASTLabelType") ) {
				g.setOption("ASTLabelType", new Token(ANTLRTokenTypes.STRING_LITERAL,"AST"));
			}
			labeledElementInit = "null";
			commonExtraArgs = "_t";
			commonExtraParams = "AST _t";
			commonLocalVars = "";
            if (usingCustomAST)
            	lt1Value = "(_t==ASTNULL) ? null : (" + labeledElementASTType + ")_t";
            else
            	lt1Value = "_t";
			exceptionThrown = "RecognitionException";
			throwNoViable = "throw new NoViableAltException(_t);";
		}
		else {
			antlrTool.panic("Unknown grammar type");
		}
	}

	/** This method exists so a subclass, namely VAJCodeGenerator,
	 *  can open the file in its own evil way.  JavaCodeGenerator
	 *  simply opens a text file...
	 */
	public void setupOutput(String className) throws IOException
	{
		currentOutput = antlrTool.openOutputFile(className + ".cs");
	}

	/** Helper method from Eric Smith's version of CSharpCodeGenerator.*/
	private static String OctalToUnicode(String str)
	{
		// only do any conversion if the string looks like "'\003'"
		if ( (4 <= str.length()) &&
 	        ('\'' == str.charAt(0)) &&
 	        ('\\' == str.charAt(1)) &&
 	        (('0' <= str.charAt(2)) && ('7' >= str.charAt(2))) &&
 	        ('\'' == str.charAt(str.length()-1)) )
		{
			// convert octal representation to decimal, then to hex
			Integer x = Integer.valueOf(str.substring(2, str.length()-1), 8);

			return "'\\x" + Integer.toHexString(x.intValue()) + "'";
		}
		else {
			return str;
		}
	}

	/** Helper method that returns the name of the interface/class/enum type for
	    token type constants.
	 */
	public String getTokenTypesClassName()
	{
		TokenManager tm = grammar.tokenManager;
		return new String(tm.getName() + TokenTypesFileSuffix);
	}

	private void declareSaveIndexVariableIfNeeded()
	{
		if (!bSaveIndexCreated)
		{
			println("int _saveIndex = 0;");
			bSaveIndexCreated = true;
		}
	}
}

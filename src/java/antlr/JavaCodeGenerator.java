package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import java.util.Enumeration;
import java.util.Hashtable;
import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
import java.io.PrintWriter; //SAS: changed for proper text file io
import java.io.IOException;
import java.io.FileWriter;

/**Generate MyParser.java, MyLexer.java and MyParserTokenTypes.java */
public class JavaCodeGenerator extends CodeGenerator {
    // non-zero if inside syntactic predicate generation
    protected int syntacticPredLevel = 0;
	
    // Are we generating ASTs (for parsers and tree parsers) right now?
    protected boolean genAST = false;

    // Are we saving the text consumed (for lexers) right now?
    protected boolean saveText = false;

    // Grammar parameters set up to handle different grammar classes.
    // These are used to get instanceof tests out of code generation
    String labeledElementType;
    String labeledElementASTType;
    String labeledElementInit;
    String commonExtraArgs;
    String commonExtraParams;
    String commonLocalVars;
    String lt1Value;
    String exceptionThrown;
    String throwNoViable;

    /** Tracks the rule being generated.  Used for mapTreeId */
    RuleBlock currentRule;

    /** Tracks the rule or labeled subrule being generated.  Used for
        AST generation. */
    String currentASTResult;

    /** Mapping between the ids used in the current alt, and the
     * names of variables used to represent their AST values.
     */
    Hashtable treeVariableMap = new Hashtable();

    /* Count of unnamed generated variables */
    int astVarNumber = 1;

    /** Special value used to mark duplicate in treeVariableMap */
    protected static final String NONUNIQUE = new String();

    public static final int caseSizeThreshold = 127; // ascii is max

    private Vector semPreds;

    /** Create a Java code-generator using the given Grammar.
	 * The caller must still call setTool, setBehavior, and setAnalyzer
	 * before generating code.
	 */
    public JavaCodeGenerator() {
	super();
	charFormatter = new JavaCharFormatter();
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
    /**Generate the parser, lexer, treeparser, and token types in Java */
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
				// print out the grammar with lookahead sets (and FOLLOWs)
				// System.out.print(g.toString());
		exitIfError();
	    }

	    // Loop over all token managers (some of which are lexers)
	    Enumeration tmIter = behavior.tokenManagers.elements();
	    while (tmIter.hasMoreElements()) {
		TokenManager tm = (TokenManager)tmIter.nextElement();
		if (!tm.isReadOnly()) {
		    // Write the token manager tokens as Java
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
		println("if ( inputState.guessing==0 ) {");
		tabs++;
	    }

	    ActionTransInfo tInfo = new ActionTransInfo();
	    String actionStr = processActionForTreeSpecifiers(action.actionText, action.getLine(), currentRule, tInfo);
			
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
		println("currentAST.child = "+tInfo.refRuleRoot+"!=null &&"+tInfo.refRuleRoot+".getFirstChild()!=null ?");
		tabs++;
		println(tInfo.refRuleRoot+".getFirstChild() : "+tInfo.refRuleRoot+";");				
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
		
	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, true);
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
	println("matchRange("+r.beginText+","+r.endText+");");
    }
    /** Generate the lexer Java file */
    public  void gen(LexerGrammar g) throws IOException {
	// If debugging, create a new sempred vector for this grammar
	if (g.debuggingOutput)
	    semPreds = new Vector();
			
	setGrammar(g);
	if (!(grammar instanceof LexerGrammar)) {
	    tool.panic("Internal error generating lexer");
	}

	// SAS: moved output creation to method so a subclass can change
	//      how the output is generated (for VAJ interface)
	setupOutput(grammar.getClassName());

	genAST = false;	// no way to gen trees.
	saveText = true;	// save consumed characters.

	tabs=0;

	// Generate header common to all Java output files
	genHeader();
	// Do not use printAction because we assume tabs==0
	println(behavior.getHeaderAction(""));

	// Generate header specific to lexer Java file
	// println("import java.io.FileInputStream;");
	println("import java.io.InputStream;");
	println("import antlr.TokenStreamException;");
	println("import antlr.TokenStreamIOException;");
	println("import antlr.TokenStreamRecognitionException;");
	println("import antlr.CharStreamException;");
	println("import antlr.CharStreamIOException;");
	println("import antlr.ANTLRException;");
	println("import java.io.Reader;");
	println("import java.util.Hashtable;");
	println("import antlr." + grammar.getSuperClass() + ";");
	println("import antlr.InputBuffer;");
	println("import antlr.ByteBuffer;");
	println("import antlr.CharBuffer;");
	println("import antlr.Token;");
	println("import antlr.CommonToken;");
	println("import antlr.RecognitionException;");
	println("import antlr.NoViableAltForCharException;");
	println("import antlr.MismatchedCharException;");
	println("import antlr.TokenStream;");
	println("import antlr.ANTLRHashString;");
	println("import antlr.LexerSharedInputState;");
	println("import antlr.collections.impl.BitSet;");
	println("import antlr.SemanticException;");

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
	if ( grammar.comment!=null ) {
	    _println(grammar.comment);
	}
		
	print("public class " + grammar.getClassName() + " extends "+sup);
	println(" implements " + grammar.tokenManager.getName() + TokenTypesFileSuffix+", TokenStream");
	Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	if ( tsuffix != null ) {
	    String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	    if ( suffix != null ) {
		print(", "+suffix);	// must be an interface name for Java
	    }
	}
	println(" {");

	// Generate user-defined lexer class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	//
	// Generate the constructor from InputStream, which in turn
	// calls the ByteBuffer constructor
	//
	println("public " + grammar.getClassName() + "(InputStream in) {");
	tabs++;
	println("this(new ByteBuffer(in));");
	tabs--;
	println("}");

	//
	// Generate the constructor from Reader, which in turn
	// calls the CharBuffer constructor
	//
	println("public " + grammar.getClassName() + "(Reader in) {");
	tabs++;
	println("this(new CharBuffer(in));");
	tabs--;
	println("}");

	println("public " + grammar.getClassName() + "(InputBuffer ib) {");
	tabs++;
	// if debugging, wrap the input buffer in a debugger
	if (grammar.debuggingOutput)
	    println("this(new LexerSharedInputState(new antlr.debug.DebuggingInputBuffer(ib)));");
	else
	    println("this(new LexerSharedInputState(ib));");
	tabs--;
	println("}");

	//
	// Generate the constructor from InputBuffer (char or byte)
	//
	println("public " + grammar.getClassName() + "(LexerSharedInputState state) {");
	tabs++;

	println("super(state);");
	// if debugging, set up array variables and call user-overridable
	//   debugging setup method
	if ( grammar.debuggingOutput ) {
	    println("  ruleNames  = _ruleNames;");
	    println("  semPredNames = _semPredNames;");
	    println("  setupDebugging();");
	}	

	// Generate the initialization of a hashtable
	// containing the string literals used in the lexer
	// The literals variable itself is in CharScanner
	println("literals = new Hashtable();");
	Enumeration keys = grammar.tokenManager.getTokenSymbolKeys();
	while ( keys.hasMoreElements() ) {
	    String key = (String)keys.nextElement();
	    if ( key.charAt(0) != '"' ) {
		continue;
	    }
	    TokenSymbol sym = grammar.tokenManager.getTokenSymbol(key);
	    if ( sym instanceof StringLiteralSymbol ) {
		StringLiteralSymbol s = (StringLiteralSymbol)sym;
		println("literals.put(new ANTLRHashString(" + s.getId() + ", this), new Integer(" + s.getTokenType() + "));");
	    }
	}
	tabs--;
		
	Enumeration ids;
	// Generate the setting of various generated options.
	println("caseSensitiveLiterals = " + g.caseSensitiveLiterals + ";");
	println("setCaseSensitive("+g.caseSensitive+");");
	println("}");

	// generate the rule name array for debugging
	if (grammar.debuggingOutput) {
	    println("private static final String _ruleNames[] = {");

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
		genRule(sym, false, ruleNum++);
	    }
	    exitIfError();
	}

	// Generate the semantic predicate map for debugging
	if (grammar.debuggingOutput)
	    genSemPredMap();

	// Generate the bitsets used throughout the lexer
	genBitsets(bitsetsUsed, ((LexerGrammar)grammar).charVocabulary.size());

	println("");
	println("}");
		
	// Close the lexer output stream
	currentOutput.close();
	currentOutput = null;
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
	println(label+":");
	println("do {");
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
	    println("if ( "+cnt+">=1 && "+predictExit+") break "+label+";");
	}
	
	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
	genBlockFinish(
		       howToFinish, 
		       "if ( "+cnt+">=1 ) { break "+label+"; } else {" + throwNoViable + "}"
		       );

	println(cnt+"++;");
	tabs--;
	println("} while (true);");
	println("}");

	// Restore previous AST generation
	currentASTResult = saveCurrentASTResult;
    }
    /** Generate the parser Java file */
    public void gen(ParserGrammar g) throws IOException {

	// if debugging, set up a new vector to keep track of sempred
	//   strings for this grammar
	if (g.debuggingOutput)
	    semPreds = new Vector();

	setGrammar(g);
	if (!(grammar instanceof ParserGrammar)) {
	    tool.panic("Internal error generating parser");
	}

	// Open the output stream for the parser and set the currentOutput
	// SAS: moved file setup so subclass could do it (for VAJ interface)
	setupOutput(grammar.getClassName());

	genAST = grammar.buildAST;

	tabs = 0;

	// Generate the header common to all output files.
	genHeader();
	// Do not use printAction because we assume tabs==0
	println(behavior.getHeaderAction(""));
		
	// Generate header for the parser
	println("import antlr.TokenBuffer;");
	println("import antlr.TokenStreamException;");
	println("import antlr.TokenStreamIOException;");
	println("import antlr.ANTLRException;");
	println("import antlr." + grammar.getSuperClass() + ";");
	println("import antlr.Token;");
	println("import antlr.TokenStream;");
	println("import antlr.RecognitionException;");
	println("import antlr.NoViableAltException;");
	println("import antlr.MismatchedTokenException;");
	println("import antlr.SemanticException;");
	println("import antlr.ParserSharedInputState;");
	println("import antlr.collections.impl.BitSet;");
	println("import antlr.collections.AST;");
	println("import antlr.ASTPair;");
	println("import antlr.collections.impl.ASTArray;");
		
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
		
	println("public class " + grammar.getClassName() + " extends "+sup);
	println("       implements " + grammar.tokenManager.getName() + TokenTypesFileSuffix);

	Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	if ( tsuffix != null ) {
	    String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	    if ( suffix != null )
		print(", "+suffix);	// must be an interface name for Java
	}
	println(" {");

	// set up an array of all the rule names so the debugger can
	// keep track of them only by number -- less to store in tree...
	if (grammar.debuggingOutput) {
	    println("private static final String _ruleNames[] = {");

	    Enumeration ids = grammar.rules.elements();
	    int ruleNum=0;
	    while ( ids.hasMoreElements() ) {
		GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
		if ( sym instanceof RuleSymbol)
		    println("  \""+((RuleSymbol)sym).getId()+"\",");
	    }
	    println("};");
	}
		
	// Generate user-defined parser class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	// Generate parser class constructor from TokenBuffer
	println("");
	println("protected " + grammar.getClassName() + "(TokenBuffer tokenBuf, int k) {");
	println("  super(tokenBuf,k);");
	println("  tokenNames = _tokenNames;");
	// if debugging, set up arrays and call the user-overridable
	//   debugging setup method
	if ( grammar.debuggingOutput ) {
	    println("  ruleNames  = _ruleNames;");
	    println("  semPredNames = _semPredNames;");
	    println("  setupDebugging(tokenBuf);");
	}	
	println("}");
	println("");

	println("public " + grammar.getClassName() + "(TokenBuffer tokenBuf) {");
	println("  this(tokenBuf," + grammar.maxk + ");");
	println("}");
	println("");

	// Generate parser class constructor from TokenStream
	println("protected " + grammar.getClassName()+"(TokenStream lexer, int k) {");
	println("  super(lexer,k);");
	println("  tokenNames = _tokenNames;");

	// if debugging, set up arrays and call the user-overridable
	//   debugging setup method
	if ( grammar.debuggingOutput ) {
	    println("  ruleNames  = _ruleNames;");
	    println("  semPredNames = _semPredNames;");
	    println("  setupDebugging(lexer);");
	}
	println("}");
	println("");

	println("public " + grammar.getClassName()+"(TokenStream lexer) {");
	println("  this(lexer," + grammar.maxk + ");");
	println("}");
	println("");

	println("public " + grammar.getClassName()+"(ParserSharedInputState state) {");
	println("  super(state," + grammar.maxk + ");");
	println("  tokenNames = _tokenNames;");
	println("}");
	println("");

	// Generate code for each rule in the grammar
	Enumeration ids = grammar.rules.elements();
	int ruleNum=0;
	while ( ids.hasMoreElements() ) {
	    GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
	    if ( sym instanceof RuleSymbol) {
		RuleSymbol rs = (RuleSymbol)sym;
		genRule(rs, rs.references.size()==0, ruleNum++);
	    }
	    exitIfError();
	}

	// Generate the token names
	genTokenStrings();

	// Generate the bitsets used throughout the grammar
	genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType());

	// Generate the semantic predicate map for debugging
	if (grammar.debuggingOutput)
	    genSemPredMap();

	// Close class definition
	println("");
	println("}");

	// Close the parser output stream
	currentOutput.close();
	currentOutput = null;
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
		println(rr.getLabel() + " = _t==ASTNULL ? null : "+lt1Value+";");
	    }
		
	// if in lexer and ! on rule ref or alt or rule, save buffer index to kill later
	if ( grammar instanceof LexerGrammar && (!saveText||rr.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
	    println("_saveIndex=text.length();");
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
	    println("text.setLength(_saveIndex);");
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
		println("if (inputState.guessing==0) {"); 
		tabs++;
	    }

	    if (grammar.buildAST && rr.getLabel() != null) {
				// always gen variable for rule return on labeled rules
		println(rr.getLabel() + "_AST = ("+labeledElementASTType+")returnAST;");
	    }
	    if (genAST) {
		switch (rr.getAutoGenType()) {
		case GrammarElement.AUTO_GEN_NONE:
		    // println("theASTFactory.addASTChild(currentAST, returnAST);");
		    println("astFactory.addASTChild(currentAST, returnAST);");
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
	    println("_t = _t.getNextSibling();");
	}
    }

    public void gen(TreeElement t) {
	// save AST cursor
	println("AST __t" + t.ID + " = _t;");

		// If there is a label on the root, then assign that to the variable
	if (t.root.getLabel() != null) {
	    println(t.root.getLabel() + " = _t==ASTNULL ? null :("+labeledElementASTType +")_t;");
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
	genMatch(t.root);
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
    /** Generate the tree-parser Java file */
    public void gen(TreeWalkerGrammar g) throws IOException {
	// SAS: debugging stuff removed for now...
	setGrammar(g);
	if (!(grammar instanceof TreeWalkerGrammar)) {
	    tool.panic("Internal error generating tree-walker");
	}
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
		
	// Generate header for the parser
	println("import antlr." + grammar.getSuperClass() + ";");
	println("import antlr.Token;");
	println("import antlr.collections.AST;");
	println("import antlr.RecognitionException;");
	println("import antlr.ANTLRException;");
	println("import antlr.NoViableAltException;");
	println("import antlr.MismatchedTokenException;");
	println("import antlr.SemanticException;");
	println("import antlr.collections.impl.BitSet;");
	println("import antlr.ASTPair;");
	println("import antlr.collections.impl.ASTArray;");
	
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
		
	println("public class " + grammar.getClassName() + " extends "+sup);
	println("       implements " + grammar.tokenManager.getName() + TokenTypesFileSuffix);
	Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	if ( tsuffix != null ) {
	    String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	    if ( suffix != null ) {
		print(", "+suffix);	// must be an interface name for Java
	    }
	}
	println(" {");

	// Generate user-defined parser class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	// Generate default parser class constructor
	println("public " + grammar.getClassName() + "() {");
	tabs++;
	println("tokenNames = _tokenNames;");
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
		genRule(rs, rs.references.size()==0, ruleNum++);
	    }
	    exitIfError();
	}

	// Generate the token names
	genTokenStrings();

	// Generate the bitsets used throughout the grammar
	genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType());

	// Close class definition
	println("}");
	println("");

	// Close the parser output stream
	currentOutput.close();
	currentOutput = null;
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
	    println("if ( _t==null ) throw new MismatchedTokenException();");
	}
	else if (grammar instanceof LexerGrammar) {
	    if ( grammar instanceof LexerGrammar &&
		 (!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
		println("_saveIndex=text.length();");
	    }
	    println("matchNot(EOF_CHAR);");
	    if ( grammar instanceof LexerGrammar &&
		 (!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
		println("text.setLength(_saveIndex);"); // kill text atom put in buffer
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
	println("{");
	genBlockPreamble(blk);
	String label;
	if ( blk.getLabel() != null ) {
	    label = blk.getLabel();
	}
	else {
	    label = "_loop" + blk.ID;
	}
	println(label+":");
	println("do {");
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
	    println("if ("+predictExit+") break "+label+";");
	}
	
	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
	genBlockFinish(howToFinish, "break " + label + ";");
		
	tabs--;
	println("} while (true);");
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
		println(rblk.getRuleName() + "_AST = ("+labeledElementASTType+")currentAST.root;");
	    } 
	    else if (blk.getLabel() != null) {
				// ### future: also set AST value for labeled subrules.
				// println(blk.getLabel() + "_AST = ("+labeledElementASTType+")currentAST.root;");
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
	 */
    protected void genBitsets(
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
		println(
			"private static final long " + getBitsetName(i) + "_data_" + "[] = { " +
			p.toStringOfWords() + 
			" };"
			);
				// BitSet object
		println(
			"public static final BitSet " + getBitsetName(i) + " = new BitSet(" +
			getBitsetName(i) + "_data_" + 
			");"
			);
	    }
    }

    /** Generate the finish of a block, using a combination of the info
     * returned from genCommonBlock() and the action to perform when
     * no alts were taken
     * @param howToFinish The return of genCommonBlock()
     * @param noViableAction What to generate when no alt is taken
     */
    private void genBlockFinish(JavaBlockFinishingInfo howToFinish, String noViableAction)
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
				println(labeledElementASTType+" " + a.getLabel() + "_AST = null;");
			    }
			}
			else {
			    if (grammar.buildAST) {
				// Always gen AST variables for
				// labeled elements, even if the
				// element itself is marked with !
				println(labeledElementASTType+" " + a.getLabel() + "_AST = null;");
			    }
			    if ( grammar instanceof LexerGrammar ) {
				println("Token "+a.getLabel()+"=null;");
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
			// In addition, generate *_AST variables if
			// building ASTs
			if (grammar.buildAST) {
			    if (a instanceof GrammarAtom &&
				((GrammarAtom)a).getASTNodeType()!=null ) {
				GrammarAtom ga = (GrammarAtom)a;
				println(ga.getASTNodeType()+" " +
					a.getLabel() +
					"_AST = null;");
			    }
			    else {
				println(labeledElementASTType+" " +
					a.getLabel() +
					"_AST = null;");
			    }
			}
		    }
		}
	    }
	}

	// dump out init action
	if ( blk.initAction!=null ) {
	    printAction(
			processActionForTreeSpecifiers(blk.initAction, 0, currentRule, null)
			);
	}
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
    public JavaBlockFinishingInfo genCommonBlock(AlternativeBlock blk, 
						 boolean noTestForSingle)
    {
	int nIF=0;
	boolean createdLL1Switch = false;
	int closingBracesOfIFSequence = 0;
	JavaBlockFinishingInfo finishingInfo = new JavaBlockFinishingInfo();
	if ( DEBUG_CODE_GENERATOR ) System.out.println("genCommonBlock("+blk+")");

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
		astArgs="_t,";
	    }

	    // match the bitset for the alternative
	    println("match(" + astArgs + getBitsetName(markBitsetForGen(p.fset)) + ");");

	    // tack on tree cursor motion if doing a tree walker
	    if (grammar instanceof TreeWalkerGrammar) {
		println("_t = _t.getNextSibling();");
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
		println("if (_t==null) _t=ASTNULL;");
	    }
	    println("switch ( "+testExpr+") {");
	    for (int i=0; i<blk.alternatives.size(); i++) {
		Alternative alt = blk.getAlternativeAt(i);
		// ignore any non-LL(1) alts, predicated alts,
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
		if ( createdLL1Switch && suitableForCaseExpression(alt) ) {
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
		    if (effectiveDepth == GrammarAnalyzer.NONDETERMINISTIC) {
			// use maximum lookahead
			effectiveDepth = grammar.maxk;
		    }
		    while ( effectiveDepth >= 1 &&
			    alt.cache[effectiveDepth].containsEpsilon() ) {
			effectiveDepth--;
		    }
		    // Ignore alts whose effective depth is other than
		    // the ones we are generating for this iteration.
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
		if ( alt.cache[1].fset.degree()>caseSizeThreshold ) {
		    if ( nIF==0 ) {
			println("if " + e + " {");
		    }
		    else {
			println("else if " + e + " {");
		    }
		}
		else if (unpredicted &&
			 alt.semPred==null &&
			 alt.synPred==null) {
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
		else { // check for sem and syn preds

		    // Add any semantic predicate expression to the
		    // lookahead test
		    if ( alt.semPred != null ) {
			// if debugging, wrap the evaluation of the
			// predicate in a method translate $ and #
			// references
			ActionTransInfo tInfo = new ActionTransInfo();
			String actionStr =
			    processActionForTreeSpecifiers(alt.semPred,
							   blk.line,
							   currentRule,
							   tInfo);
			// ignore translation info...we don't need to
			// do anything with it.  call that will inform
			// SemanticPredicateListeners of the result
			if (((grammar instanceof ParserGrammar) ||
			     (grammar instanceof LexerGrammar)) &&
			    grammar.debuggingOutput) {
			    e = "("+e+"&& fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.PREDICTING,"+
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
			    // when parsing trees, convert null to
			    // valid tree node with NULL lookahead.
			    if ( grammar instanceof TreeWalkerGrammar ) {
				println("if (_t==null) _t=ASTNULL;");
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
	    //return new JavaBlockFinishingInfo(ps+"}",true,nIF>0); // close up switch statement
			
	}
	else {
	    finishingInfo.postscript = ps;
	    finishingInfo.generatedSwitch = false;
	    finishingInfo.generatedAnIf = nIF>0;
	    // return new JavaBlockFinishingInfo(ps, false,nIF>0);
	}	
	return finishingInfo;
    }

    private static boolean suitableForCaseExpression(Alternative a) {
	return
	    a.lookaheadDepth == 1 &&
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
			println(ga.getASTNodeType()+" " + astName+" = null;");
		    }
		    else {
			println(labeledElementASTType+" " + astName + " = null;");
		    }
		}
		else {
		    println(labeledElementASTType+" " + astName + " = null;");
		}
				// Map the generated AST variable in the alternate
		mapTreeVariable(el, astName);
		if (grammar instanceof TreeWalkerGrammar) {
		    // Generate an "input" AST variable also
		    println(labeledElementASTType+" " + astName + "_in = null;");
		}
	    }

	    // Enclose actions with !guessing
	    if (doNoGuessTest) {
		println("if (inputState.guessing==0) {"); 
		tabs++;
	    }

	    if (el.getLabel() != null) {
		if ( el instanceof GrammarAtom ) {
		    println(astName + " = "+ getASTCreateString((GrammarAtom)el, elementRef) + ";");
		}
		else {
		    println(astName + " = "+ getASTCreateString(elementRef) + ";");
		}
	    } else {
		elementRef = lt1Value;
		if ( el instanceof GrammarAtom ) {
		    println(astName + " = "+ getASTCreateString((GrammarAtom)el, elementRef) + ";");
		}
		else {
		    println(astName + " = "+ getASTCreateString(elementRef) + ";");
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
		    println("astFactory.addASTChild(currentAST, " + astName + ");");
		    break;
		case GrammarElement.AUTO_GEN_CARET:
		    println("astFactory.makeASTRoot(currentAST, " + astName + ");");
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
		    println("if (inputState.guessing==0) {");
		    tabs++;
		}
			
				// When not guessing, execute user handler action
		printAction(
			    processActionForTreeSpecifiers(handler.action.getText(), 0, currentRule, null)
			    );
				
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

    /** Generate a header that is common to all Java files */
    protected void genHeader() {
	println("// $ANTLR "+Tool.version+": "+
		"\""+Tool.fileMinusPath(tool.grammarFile)+"\""+
		" -> "+
		"\""+grammar.getClassName()+".java\"$");
    }

    private void genLiteralsTest() {
	println("_ttype = testLiteralsTable(_ttype);");
    }

    private void genLiteralsTestForPartialToken() {
	println("_ttype = testLiteralsTable(new String(text.getBuffer(),_begin,text.length()-_begin),_ttype);");
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
	    _print("Token.EOF_TYPE");
	} 
	else {
	    _print(atom.atomText);
	}
	_println(");");

	if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
	    println("text.setLength(_saveIndex);");		// kill text atom put in buffer
	}
    }
    protected void genMatchUsingAtomTokenType(GrammarAtom atom) {
	// match() for trees needs the _t cursor
	String astArgs="";
	if (grammar instanceof TreeWalkerGrammar) {
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
	    println("public Token nextToken() throws TokenStreamException {");
	    println("\ttry {uponEOF();}");
	    println("\tcatch(CharStreamIOException csioe) {");
	    println("\t\tthrow new TokenStreamIOException(csioe.io);");
	    println("\t}");
	    println("\tcatch(CharStreamException cse) {");
	    println("\t\tthrow new TokenStreamException(cse.getMessage());");
	    println("\t}");
	    println("\treturn new CommonToken(Token.EOF_TYPE, \"\");");
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
	println("public Token nextToken() throws TokenStreamException {");
	tabs++;
	println("Token theRetToken=null;");
	_println("tryAgain:");
	println("for (;;) {");
	tabs++;
	println("Token _token = null;");
	println("int _ttype = Token.INVALID_TYPE;");
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
	JavaBlockFinishingInfo howToFinish = genCommonBlock(nextTokenBlk, false);
	String errFinish = "if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}";
	errFinish += newline+"\t\t\t\t";
	if ( ((LexerGrammar)grammar).filterMode ) {
	    if ( filterRule==null ) {
		errFinish += "else {consume(); continue tryAgain;}";
	    }
	    else {
		errFinish += "else {"+newline+
		    "\t\t\t\t\tcommit();"+newline+
		    "\t\t\t\t\ttry {m"+filterRule+"(false);}"+newline+
		    "\t\t\t\t\tcatch(RecognitionException e) {"+newline+
		    "\t\t\t\t\t	// catastrophic failure"+newline+
		    "\t\t\t\t\t	reportError(e);"+newline+
		    "\t\t\t\t\t	consume();"+newline+
		    "\t\t\t\t\t}"+newline+
		    "\t\t\t\t\tcontinue tryAgain;"+newline+
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
	println("if ( _returnToken==null ) continue tryAgain; // found SKIP token");
	println("_ttype = _returnToken.getType();");
	if ( ((LexerGrammar)grammar).getTestLiterals()) {
	    genLiteralsTest();
	}

	// return token created by rule reference in switch
	println("_returnToken.setType(_ttype);");
	println("return _returnToken;");
		
	// Close try block
	tabs--;
	println("}");
	println("catch (RecognitionException e) {");
	tabs++;
	if ( ((LexerGrammar)grammar).filterMode ) {
	    if ( filterRule==null ) {
		println("if ( !getCommitToPath() ) {consume(); continue tryAgain;}");
	    }
	    else {
		println("if ( !getCommitToPath() ) {");
		tabs++;
		println("rewind(_m);");
		println("resetText();");
		println("try {m"+filterRule+"(false);}");
		println("catch(RecognitionException ee) {");
		println("	// horrendous failure: error in filter rule");
		println("	reportError(ee);");
		println("	consume();");
		println("}");
		println("continue tryAgain;");
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
	    println("throw new TokenStreamRecognitionException(e);");
	}
	tabs--;
	println("}");

	// close CharStreamException try
	tabs--;
	println("}");
	println("catch (CharStreamException cse) {");
	println("	if ( cse instanceof CharStreamIOException ) {");
	println("		throw new TokenStreamIOException(((CharStreamIOException)cse).io);");
	println("	}");
	println("	else {");
	println("		throw new TokenStreamException(cse.getMessage());");
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
    public void genRule(RuleSymbol s, boolean startSymbol, int ruleNum) {
	tabs=1;
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
	print(s.access + " final ");

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

	// Gen throws clause and open curly
	_print(" throws " + exceptionThrown);
	if ( grammar instanceof ParserGrammar ) {
	    _print(", TokenStreamException");
	}
	else if ( grammar instanceof LexerGrammar ) {
	    _print(", CharStreamException, TokenStreamException");
	}
	// Add user-defined exceptions unless lexer (for now)
	if ( rblk.throwsSpec!=null ) {
	    if ( grammar instanceof LexerGrammar ) {
		tool.error("user-defined throws spec not allowed (yet) for lexer rule "+rblk.ruleName);
	    }
	    else {
		_print(", "+rblk.throwsSpec);
	    }
	}

	_println(" {");
	tabs++;

	// Convert return action to variable declaration
	if (rblk.returnAction != null)
	    println(rblk.returnAction + ";");
		
	// print out definitions needed by rules for various grammar types
	println(commonLocalVars);
		
	if (grammar.traceRules) {
	    if ( grammar instanceof TreeWalkerGrammar ) {
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
		println("_ttype = "+ s.getId().substring(1)+";");
	    println("int _saveIndex;");		// used for element! (so we can kill text matched for element)
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
	    println("ASTPair currentAST = new ASTPair();");
	    // User-settable return value for rule.
	    println(labeledElementASTType+" " + s.getId() + "_AST = null;");
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
			     grammar.getFilename(), alt.synPred.getLine()
			     );
	    }
	    genAlt(alt, rblk);
	}
	else {
	    // Multiple alternatives -- generate complex form
	    boolean ok = grammar.theLLkAnalyzer.deterministic(rblk);
		
	    JavaBlockFinishingInfo howToFinish = genCommonBlock(rblk, false);
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
	    println("catch (" + exceptionThrown + " ex) {");
	    tabs++;
	    // Generate code to handle error if not guessing
	    if (grammar.hasSyntacticPredicate) {
		println("if (inputState.guessing==0) {");
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
		println("if (_t!=null) {_t = _t.getNextSibling();}");
	    }
	    if (grammar.hasSyntacticPredicate) {
		tabs--;
				// When guessing, rethrow exception
		println("} else {");
		println("  throw ex;");
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
	    println("if ( _createToken && _token==null && _ttype!=Token.SKIP ) {");
	    println("	_token = makeToken(_ttype);");
	    println("	_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));");
	    println("}");
	    println("_returnToken = _token;");
	}	

	// Gen the return statement if there is one (lexer has hard-wired return action)
	if (rblk.returnAction != null) {
	    println("return " + extractIdOfAction(rblk.returnAction, rblk.getLine()) + ";");
	}
		
	if ( grammar.debuggingOutput || grammar.traceRules) {
	    tabs--;
	    println("} finally { // debugging");		
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
		if (rs.block.argAction == null) {
		    tool.warning("Rule '" + rr.targetRule + "' accepts no arguments", grammar.getFilename(), rr.getLine());
		}
	    } else {
				// For C++, no warning if rule has parameters, because there may be default
				// values for all of the parameters
		if (rs.block.argAction != null) {
		    tool.warning("Missing parameters on reference to rule "+rr.targetRule, grammar.getFilename(), rr.getLine());
		}
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
	println("private String _semPredNames[] = {");
	while(e.hasMoreElements())
	    println("\""+e.nextElement()+"\",");
	println("};");			
    }
    protected void genSynPred(SynPredBlock blk, String lookaheadExpr) {
	if ( DEBUG_CODE_GENERATOR ) System.out.println("gen=>("+blk+")");

	// Dump synpred result variable
	println("boolean synPredMatched" + blk.ID + " = false;");
	// Gen normal lookahead test
	println("if (" + lookaheadExpr + ") {");
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
	println("catch (" + exceptionThrown + " pe) {");
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
    public void genTokenStrings() {
	// Generate a string for each token.  This creates a static
	// array of Strings indexed by token type.
	println("");
	println("public static final String[] _tokenNames = {");
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
		if (i != v.size()-1) {
		    _print(",");
		}
		_println("");
	    }

	// Close the string array initailizer
	tabs--;
	println("};");
    }
    /** Generate the token types Java file */
    protected void genTokenTypes(TokenManager tm) throws IOException {
	// Open the token output Java file and set the currentOutput stream
	// SAS: file open was moved to a method so a subclass can override
	//      This was mainly for the VAJ interface
	setupOutput(tm.getName() + TokenTypesFileSuffix);

	tabs = 0;

	// Generate the header common to all Java files
	genHeader();
	// Do not use printAction because we assume tabs==0
	println(behavior.getHeaderAction(""));

	// Encapsulate the definitions in an interface.  This can be done
	// because they are all constants.
	println("public interface " + tm.getName() + TokenTypesFileSuffix+" {");
	tabs++;

		
	// Generate a definition for each token type
	Vector v = tm.getVocabulary();
		
	// Do special tokens manually
	println("int EOF = " + Token.EOF_TYPE + ";");
	println("int NULL_TREE_LOOKAHEAD = " + Token.NULL_TREE_LOOKAHEAD + ";");
		
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
			println("int " + sl.label + " = " + i + ";");
		    }
		    else {	
			String mangledName = mangleLiteral(s);
			if (mangledName != null) {
			    // We were able to create a meaningful mangled token name
			    println("int " + mangledName + " = " + i + ";");
			    // if no label specified, make the label equal to the mangled name
			    sl.label = mangledName;
			}
			else {
			    println("// " + s + " = " + i);
			}
		    }	
		}
		else if ( !s.startsWith("<") ) {
		    println("int " + s + " = " + i + ";");
		}
	    }
	}

	// Close the interface
	tabs--;
	println("}");

	// Close the tokens output file
	currentOutput.close();
	currentOutput = null;
	exitIfError();
    }

    /** Get a string for an expression to generate creation of an AST subtree.
	  * @param v A Vector of String, where each element is an expression in the target language yielding an AST node.
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
    public String getASTCreateString(GrammarAtom atom, String str) {
	// System.out.println("ASTNodeType for "+atom+" is "+atom.getASTNodeType());
	if ( atom!=null && atom.getASTNodeType() != null ) {
	    return "new "+atom.getASTNodeType()+"("+str+")";
	}
	else {
	    return "("+labeledElementASTType+")astFactory.create(" + str + ")";
	}
    }

    /** Get a string for an expression to generate creating of an AST node
	 * @param str The arguments to the AST constructor
	 */
    public String getASTCreateString(String str) {
	// System.out.println("ASTNodeType for "+atom+" is "+atom.getASTNodeType());
	return "("+labeledElementASTType+")astFactory.create(" + str + ")";
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
	    return "_t.getType()";
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

	boolean in_var = false;
	String id = idParam;
	if (grammar instanceof TreeWalkerGrammar) {
	    if ( !grammar.buildAST ) {
		in_var = true;
	    }
	    // If the id ends with "_in", then map it to the input variable
	    else if (id.length() > 3 && id.lastIndexOf("_in") == id.length()-3) {
				// Strip off the "_in"
		id = id.substring(0, id.length()-3);
		in_var = true;
	    }
	}

	// Check the rule labels.  If id is a label, then the output
	// variable is label_AST, and the input variable is plain label.
	for (int i = 0; i < currentRule.labeledElements.size(); i++) {
	    AlternativeElement elt = (AlternativeElement)currentRule.labeledElements.elementAt(i);
	    if (elt.getLabel().equals(id)) {
		return in_var ? id : id + "_AST";
	    }
	}

	// Failing that, check the id-to-variable map for the alternative.
	// If the id is in the map, then output variable is the name in the
	// map, and input variable is name_in
	String s = (String)treeVariableMap.get(id);
	if (s != null) {
	    if (s == NONUNIQUE) {
				// There is more than one element with this id
		return null;
	    } else if (s.equals(currentRule.getRuleName())) {
				// a recursive call to the enclosing rule is 
				// ambiguous with the rule itself.
		return null;
	    } else {
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
	    return r;
	} else {
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

    private void setupGrammarParameters(Grammar g) {
	if (g instanceof ParserGrammar) {
	    labeledElementASTType = "AST";
	    if ( g.hasOption("ASTLabelType") ) {
		Token tsuffix = g.getOption("ASTLabelType");
		if ( tsuffix != null ) {
		    String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
		    if ( suffix != null ) {
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
	    commonExtraParams = "boolean _createToken";
	    commonLocalVars = "int _ttype; Token _token=null; int _begin=text.length();";
	    lt1Value = "LA(1)";
	    exceptionThrown = "RecognitionException";
	    throwNoViable = "throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());";
	}
	else if (g instanceof TreeWalkerGrammar) {
	    labeledElementASTType = "AST";
	    labeledElementType = "AST";
	    if ( g.hasOption("ASTLabelType") ) {
		Token tsuffix = g.getOption("ASTLabelType");
		if ( tsuffix != null ) {
		    String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
		    if ( suffix != null ) {
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
	    lt1Value = "("+labeledElementASTType+")_t";
	    exceptionThrown = "RecognitionException";
	    throwNoViable = "throw new NoViableAltException(_t);";
	}
	else {
	    tool.panic("Unknown grammar type");
	}
    }

    /** This method exists so a subclass, namely VAJCodeGenerator,
	 *  can open the file in its own evil way.  JavaCodeGenerator
	 *  simply opens a text file...
	 */
    public void setupOutput(String className) throws IOException {
	currentOutput = antlr.Tool.openOutputFile(className + ".java");
    }
}

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

/**Generate MY_PARSER.sa, MY_LEXER.sa and MY_PARSER_TOKENTYPES.sa */

public class SatherCodeGenerator extends CodeGenerator 
{
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

    /** Create a Java code-generator using the given Grammar.
	 * The caller must still call setTool, setBehavior, and setAnalyzer
	 * before generating code.
	 */
    public SatherCodeGenerator() {
	super();
	charFormatter = new SatherCharFormatter();
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
		println("if ( input_state.guessing = 0 ) then");
		tabs++;
	    }

	    ActionTransInfo tInfo = new ActionTransInfo();
	    String actionStr = processActionForTreeSpecifiers(action.actionText, action.getLine(), currentRule, tInfo);
			
	    if ( tInfo.refRuleRoot!=null ) {
				// Somebody referenced "#rule", make sure translated var is valid
				// assignment to #rule is left as a ref also, meaning that assignments
				// with no other refs like "#rule = foo();" still forces this code to be
				// generated (unnecessarily).
		println(tInfo.refRuleRoot + " := current_ast.root;");
	    }
			
	    // dump the translated action
	    printAction(actionStr);
			
	    if ( tInfo.assignToRoot ) {
				// Somebody did a "#rule=", reset internal currentAST.root
		println("current_ast.root := "+ tInfo.refRuleRoot + ";");
				// reset the child pointer too to be last sibling in sibling list
		println("if ( ~void( " + tInfo.refRuleRoot + " ) and ~void( "
			+ tInfo.refRuleRoot + ".first_child ) ) then" );
		tabs++;
		println("current_ast.child := " + tInfo.refRuleRoot + ".first_child");
		tabs--;
		println("else");
		tabs++;
		println("current_ast.child := " + tInfo.refRuleRoot + ";");
		tabs--;
		println("end; -- if");
		println("current_ast.advance_child_to_end;");
	    }
		
	    if ( grammar.hasSyntacticPredicate ) {
		tabs--;
		println("end; -- if");
	    }
	}
    }
    /** Generate code for the given grammar element.
	 * @param blk The "x|y|z|..." block to generate
	 */
    public void gen(AlternativeBlock blk) {
	if ( DEBUG_CODE_GENERATOR ) System.out.println("gen("+blk+")");
	//		println("{");
	genBlockPreamble(blk);

	// Tell AST generation to build subrule result
	String saveCurrentASTResult = currentASTResult;
	if (blk.getLabel() != null) {
	    currentASTResult = blk.getLabel();
	}

	boolean ok = grammar.theLLkAnalyzer.deterministic(blk);
		
	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, true);
	genBlockFinish(howToFinish, throwNoViable);
	println("");

	//		println("}");

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
	    println(atom.getLabel() + " := " + lt1Value + ";");
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
	    println(r.getLabel() + " := " + lt1Value + ";");
	}
	println("match_range( " + r.beginText+ ", " + r.endText+ " );");
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

	// Generate user-defined lexer file preamble
	println(grammar.preambleAction.getText());

	// Generate lexer class definition
	String sup=null;
	if ( grammar.superClass!=null ) {
	    sup = grammar.superClass;
	}
	else {
	    sup = "ANTLR_CHAR_SCANNER{TOKEN}";
	}	

	// print javadoc comment if any
	if ( grammar.comment!=null ) {
	    _println(grammar.comment);
	}
		
	println("class " + grammar.getClassName() + 
		"{TOKEN} < $ANTLR_TOKEN_STREAM{TOKEN} , $ANTLR_FILE_CURSOR is " );
	tabs++;
	println("include " + sup + " create -> private char_scanner_create;");
	println("include " + grammar.tokenManager.getName() + "_TOKENTYPES;");

	/*
	  Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	  if ( tsuffix != null ) {
	  String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	  if ( suffix != null ) {
	  print(", "+suffix);	// must be an interface name for Java
	  }
	  }
	*/

	println("");

	// Generate user-defined lexer class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	//
	// Generate the constructor from ISTREAM, which in turn
	// calls the ByteBuffer constructor
	//
	println("create ( istr : $ISTREAM ) : SAME is");
	tabs++;
	println("inp : ANTLR_BYTE_BUFFER := #ANTLR_BYTE_BUFFER( istr );");
	println("res : SAME := #SAME( inp );");
	println("res.EOF_CHAR := istr.eof_char;");
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	//
	// Generate the constructor from Reader, which in turn
	// calls the CharBuffer constructor
	//
	// 		println("public " + grammar.getClassName() + "(Reader in) {");
	// 		tabs++;
	// 		println("this(new CharBuffer(in));");
	// 		tabs--;
	// 		println("}");

	println("create ( bb : ANTLR_BYTE_BUFFER ) : SAME is");
	tabs++;
	// if debugging, wrap the input buffer in a debugger
	//  		if (grammar.debuggingOutput)
	//  			println("this(new LexerSharedInputState(new antlr.debug.DebuggingInputBuffer(ib)));");
	//  		else
	println("state : ANTLR_LEXER_SHARED_INPUT_STATE := #ANTLR_LEXER_SHARED_INPUT_STATE( bb );");
	println("res: SAME := #SAME( state );");
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	//
	// Generate the constructor from InputBuffer (char or byte)
	//
	println("create ( state : ANTLR_LEXER_SHARED_INPUT_STATE ) : SAME is ");
	tabs++;

	println("res : SAME := char_scanner_create( state );");
	// if debugging, set up array variables and call user-overridable
	//   debugging setup method
	//		if ( grammar.debuggingOutput ) {
	//			println("rule_names  := sa_rule_names;");
	//                      println("sem_pred_names := sa_sem_pred_names;");
	//                      println("setup_debugging;");
	//		}	

	// Generate the initialization of a hashtable
	// containing the string literals used in the lexer
	// The literals variable itself is in CharScanner
	println("res.literals := #MAP{STR,INT};");
	Enumeration keys = grammar.tokenManager.getTokenSymbolKeys();
	while ( keys.hasMoreElements() ) {
	    String key = (String)keys.nextElement();
	    if ( key.charAt(0) != '"' ) {
		continue;
	    }
	    TokenSymbol sym = grammar.tokenManager.getTokenSymbol(key);
	    if ( sym instanceof StringLiteralSymbol ) {
		StringLiteralSymbol s = (StringLiteralSymbol)sym;
		println("res.literals[ " + s.getId() + " ] := " + s.getTokenType() + ";");
	    }
	}
	Enumeration ids;
	// Generate the setting of various generated options.
	println("res.case_sensitive_literals := " + g.caseSensitiveLiterals + ";");
	println("res.case_sensitive := " + g.caseSensitive + ";");
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	// generate the rule name array for debugging
	if (grammar.debuggingOutput) {
	    println("private const sa_rule_names : ARRAY{STR} := |");

	    ids = grammar.rules.elements();
	    int ruleNum=0;
	    while ( ids.hasMoreElements() ) {
		GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
		if ( sym instanceof RuleSymbol)
		    println("  \""+((RuleSymbol)sym).getId()+"\",");
	    }
	    println("|;");
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
	tabs--;
	println("end; -- class");
		
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
	//              println("{"); // cannot nest Sather scopes within a routine
	genBlockPreamble(blk);
	if ( blk.getLabel() != null ) {
	    cnt = getNextSatherPrefix() + "_cnt_"+ blk.getLabel();
	}
	else {
	    cnt = getNextSatherPrefix() + "_cnt" + blk.ID;
	}
	println( cnt + " : INT := 0;");
	//  		if ( blk.getLabel() != null ) {
	//  			label = blk.getLabel();
	//  		}
	//  		else {
	//  			label = "_loop" + blk.ID;
	//  		}
	//		println(label+":"); 
	println("loop");
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
	    println("-- nongreedy exit test");
	    println("if ( " + cnt + " >= 1 and " + predictExit + " ) then break! end; -- if");
	}
	
	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
	genBlockFinish(
		       howToFinish, 
		       "if ( " + cnt + " >= 1 ) then break! else " + throwNoViable + " end; -- if"
		       );

	println( cnt + " := " + cnt + " + 1;" );
	tabs--;
	println("end; -- loop");
	//		println("}"); // cannot nest Sather scopes within a routine

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
		
	// Output the user-defined parser preamble
	println(grammar.preambleAction.getText());

	// Generate parser class definition
	String sup=null;
	if ( grammar.superClass != null )
	    sup = grammar.superClass.toUpperCase();
	else
	    sup = "ANTLR_" + grammar.getSuperClass().toUpperCase();

	// print javadoc comment if any
	if ( grammar.comment!=null ) {
	    _println(grammar.comment);
	}
		
	println("class " + grammar.getClassName() + "{ TOKEN < $ANTLR_TOKEN, AST < $ANTLR_AST{AST} } is");
	tabs++;
	println("include " + sup + "{ TOKEN, " + labeledElementASTType + " } create -> super_create;" );
	println("include " + grammar.tokenManager.getName() + "_" 
		+ TokenTypesFileSuffix.toUpperCase() + ";" );

	println("");
		
	/*
	  Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	  if ( tsuffix != null ) {
	  String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	  if ( suffix != null )
	  print(", "+suffix);	// must be an interface name for Java
	  }
	*/

	// set up an array of all the rule names so the debugger can
	// keep track of them only by number -- less to store in tree...
	if (grammar.debuggingOutput) {
	    println("const sa_rule_names : ARRAY{STR} := |");

	    Enumeration ids = grammar.rules.elements();
	    int ruleNum=0;
	    while ( ids.hasMoreElements() ) {
		GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
		if ( sym instanceof RuleSymbol)
		    println("  \""+((RuleSymbol)sym).getId()+"\",");
	    }
	    println("|;");
	}
		
	// Generate user-defined parser class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	// Generate parser class constructor from TokenBuffer
	println("");
	println( "create ( token_buf : ANTLR_TOKEN_BUFFER{TOKEN} , k : INT ) : SAME is");
	tabs++;
	println("res : SAME := super_create( token_buf, k );");
	println("res.token_names := sa_token_names;");
	// println("res.ast_factory := #ANTLR_COMMON_AST_FACTORY;");

	// if debugging, set up arrays and call the user-overridable
	//   debugging setup method
	if ( grammar.debuggingOutput ) {
	    println("res.rule_names  := sa_rule_names;");
	    println("res.sem_pred_names := sa_sem_pred_names;");
	    println("res.setup_debugging( token_buf );");
	}	
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	println( "create ( token_buf : ANTLR_TOKEN_BUFFER{TOKEN} ) : SAME is");
	tabs++;
	println("return #SAME( token_buf, " + grammar.maxk + ");");
	tabs--;
	println("end; -- create");
	println("");

	// Generate parser class constructor from TokenStream
	println( "create ( lexer : $ANTLR_TOKEN_STREAM{TOKEN} , k : INT ) : SAME is");
	tabs++;
	println("res : SAME := super_create( lexer, k );");
	println("res.token_names := sa_token_names;");
	// println("res.ast_factory := #ANTLR_COMMON_AST_FACTORY;");

	// if debugging, set up arrays and call the user-overridable
	//   debugging setup method
	if ( grammar.debuggingOutput ) {
	    println("res.rule_names := sa_rule_names;");
	    println("res.sem_pred_names := sa_sem_pred_names;");
	    println("res.setup_debugging( lexer );");
	}
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	println( "create( lexer : $ANTLR_TOKEN_STREAM{TOKEN} ) : SAME is");
	tabs++;
	println("res : SAME := #SAME( lexer, " + grammar.maxk + ");");
	println("return res;");
	tabs--;
	println("end; -- create");
	println("");

	println( "create ( state : ANTLR_PARSER_SHARED_INPUT_STATE{TOKEN} ) : SAME is ");
	tabs++;
	println("res : SAME := super_create( state," + grammar.maxk + ");");
	println("res.token_names := sa_token_names;");
	// println("res.ast_factory := #ANTLR_COMMON_AST_FACTORY;");
	println("return res;");
	tabs--;
	println("end; -- create");
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
	tabs--;
	println("end; -- class");

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
		println("if ( SYS::is_eq( sa_t , " + labeledElementASTType + "::ASTNULL ) ) then");
		tabs++;
		println( rr.getLabel() + " := void;");
		tabs--;
		println("else");
		println(rr.getLabel() + " := " + lt1Value + ";" );
		println("end; -- if");
	    }
		
	// if in lexer and ! on rule ref or alt or rule, save buffer index to kill later
	if ( grammar instanceof LexerGrammar && (!saveText||rr.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
	    println("sa_save_index := text.length;");
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
		_print(rr.idAssign + ":=");
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
	    println("text := text.substring( 0, sa_save_index); -- truncate");
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
		println("if ( input_state.guessing = 0 ) then"); 
		tabs++;
	    }

	    if (grammar.buildAST && rr.getLabel() != null) {
				// always gen variable for rule return on labeled rules
		println( rr.getLabel() + "_ast := return_ast;");
	    }
	    if (genAST) {
		switch (rr.getAutoGenType()) {
		case GrammarElement.AUTO_GEN_NONE:
		    println("current_ast.add_child( return_ast );");
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
		println(rr.getLabel()+" := sa_return_token;");
	    }	

	    if (doNoGuessTest) {
		tabs--;
		println("end;"); 
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
	    println(atom.getLabel() + " := " + lt1Value + ";");
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
	    println("sa_t := sa_t.next_sibling;");
	}
    }
    /** Generate code for the given grammar element.
	 * @param blk The token-range reference to generate
	 */
    public void gen(TokenRangeElement r) {
	genErrorTryForElement(r);
	if ( r.getLabel()!=null  && syntacticPredLevel == 0) {
	    println(r.getLabel() + " := " + lt1Value + ";");
	}

	// AST
	genElementAST(r);

	// match
	println("match_range( " + r.beginText + ", " + r.endText + " );");
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
	    println(atom.getLabel() + " := " + lt1Value + ";");
	}

	// AST
	genElementAST(atom);

	// matching
	genMatch(atom);
	genErrorCatchForElement(atom);

	// tack on tree cursor motion if doing a tree walker
	if (grammar instanceof TreeWalkerGrammar) {
	    println("sa_t := sa_t.next_sibling;");
	}
    }
    public void gen(TreeElement t) {
	// save AST cursor
	println("sa__t" + t.ID + " : " + labeledElementASTType + " := sa_t;");

		// If there is a label on the root, then assign that to the variable
	if (t.root.getLabel() != null) {
	    println("if ( SYS::is_eq( sa_t , AST::ASTNULL ) ) then");
	    tabs++;
	    println(t.root.getLabel() + " := void;");
	    println("else");
	    println(t.root.getLabel() + " := sa_t;");
	    println("end; -- if");
	}

	// Generate AST variables
	genElementAST(t.root);
	if (grammar.buildAST) {
	    // Save the AST construction state
	    println("sa__current_ast" + t.ID + " : ANTLR_AST_PAIR{AST} := current_ast.copy;");
	    // Make the next item added a child of the TreeElement root
	    println("current_ast.root := current_ast.child;");
	    println("current_ast.child := void;");
	}

	// match root
	genMatch(t.root);
	// move to list of children
	println("sa_t := sa_t.first_child;"); 
		
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
	    println("current_ast := sa__current_ast" + t.ID + ";");
	}
	// restore AST cursor
	println("sa_t := sa__t" + t.ID + ";");
	// move cursor to sibling of tree just parsed
	println("sa_t := sa_t.next_sibling;");
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
		
	// Output the user-defined parser premamble
	println(grammar.preambleAction.getText());

	// Generate parser class definition
	String sup=null;
	if ( grammar.superClass!=null ) {
	    sup = grammar.superClass.toUpperCase();
	}
	else {
	    sup = "ANTLR_TREE_PARSER";
	}	

	// print javadoc comment if any
	if ( grammar.comment!=null ) {
	    _println(grammar.comment);
	}
		
	println("class " + grammar.getClassName() + "{AST < $ANTLR_AST{AST} } is" );
	println("");
	tabs++;

	println("include " + sup + "{" + labeledElementASTType + "} create -> tree_parser_create;" );
	println("include " + grammar.tokenManager.getName() + "_" 
		+ TokenTypesFileSuffix.toUpperCase() + ";" );
	println("");

	/*
	  Token tsuffix = (Token)grammar.options.get("classHeaderSuffix");
	  if ( tsuffix != null ) {
	  String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	  if ( suffix != null ) {
	  print(", "+suffix);	// must be an interface name for Java
	  }
	  }
	*/

	// Generate user-defined parser class members
	print(
	      processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule, null)
	      );

	println("attr token_names : ARRAY{STR};");
	println("");

	// Generate default parser class constructor
	println("create : SAME is" );
	tabs++;
	println("res : SAME := tree_parser_create;");
	println("res.token_names := sa_token_names;");
	println("return res;");
	tabs--;
	println("end; -- create");
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
	tabs--;
	println("end; -- class");
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
	    println(wc.getLabel() + " := " + lt1Value + ";");
	}

	// AST
	genElementAST(wc);
	// Match anything but EOF
	if (grammar instanceof TreeWalkerGrammar) {
	    println("if ( void(sa_t) ) then");
	    tabs++;
	    println("raise #ANTLR_MISMATCHED_TOKEN_EXCEPTION;");
	    tabs--;
	    println("end;");
	}
	else if (grammar instanceof LexerGrammar) {
	    if ( grammar instanceof LexerGrammar &&
		 (!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
		println("sa_save_index := text.length;");
	    }
	    println("match_not(EOF_CHAR);");
	    if ( grammar instanceof LexerGrammar &&
		 (!saveText||wc.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
		// kill text atom put in buffer
		println("text := text.substring( 0 , sa_save_index);");
	    }
	    
	}
	else {
	    println("match_not(" + getValueString(Token.EOF_TYPE) + ");");
	}
		
	// tack on tree cursor motion if doing a tree walker
	if (grammar instanceof TreeWalkerGrammar) {
	    println("sa_t := sa_t.next_sibling;");
	}
    }
    /** Generate code for the given grammar element.
	 * @param blk The (...)* block to generate
	 */
    public void gen(ZeroOrMoreBlock blk) {
	if ( DEBUG_CODE_GENERATOR ) System.out.println("gen*("+blk+")");
	//		println("{");
	genBlockPreamble(blk);
	String label;
	if ( blk.getLabel() != null ) {
	    label = blk.getLabel();
	}
	else {
	    label = "_loop" + blk.ID;
	}
	//		println(label+":");
	println("loop");
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
	    println("-- nongreedy exit test");
	    println("if ( " + predictExit + " ) then break! end; -- if");
	}

	JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
	genBlockFinish(howToFinish, "break!;" ); 

	tabs--;
	println("end; -- loop ");

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
	    println("protect -- for error handling");
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
		println(rblk.getRuleName() + "_ast := current_ast.root;");
	    } 
	    else if (blk.getLabel() != null) {
				// ### future: also set AST value for labeled subrules.
				// println(blk.getLabel() + "_ast = ("+labeledElementASTType+")currentAST.root;");
	    }
	}

	if (alt.exceptionSpec != null) {
	    // close try block
	    tabs--;
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
	    
	SatherCharFormatter satherCharFormatter = new SatherCharFormatter();

	println("");

	// here, I differ from the Java code generator.  Lexer's bitsets are implemented as 
	// Sather sets of CHAR's, Parser bitesets as Sather sets INT's

	if ( grammar instanceof LexerGrammar ) {

	    for ( int i = 0 ; i < bitsetList.size() ; i++)	{
		BitSet p = (BitSet)bitsetList.elementAt(i);
		// Ensure that generated BitSet is large enough for vocabulary
		p.growToInclude(maxVocabulary);
		String boolList = satherCharFormatter.BitSet2BoolList( p, ", " );

		String bitsetName = "sa" + getBitsetName(i);
		String bitsetData = bitsetName + "_data_";

		// initialization data
		println(
			"const " + bitsetData + 
			" : ARRAY{BOOL} := " +
			"| " +
			boolList + 
			" |;"
			);
		println( "const " + bitsetName + " : CHAR_SET := bitset( " +
			 bitsetData + " );" );
			    
	    }
	}
	else {
	    for ( int i = 0 ; i < bitsetList.size() ; i++)	{
		BitSet p = (BitSet)bitsetList.elementAt(i);
		// Ensure that generated BitSet is large enough for vocabulary
		p.growToInclude(maxVocabulary);
		String charList = satherCharFormatter.BitSet2IntList( p, ", " );

		String bitsetName = "sa" + getBitsetName(i);
		String bitsetData = bitsetName + "_data_";

		// initialization data
		println(
			"const " + bitsetData + 
			" : ARRAY{INT} := " +
			"| " +
			charList + 
			" |;"
			);
		println( "const " + bitsetName + " : INT_SET := int_set( " +
			 bitsetData + " );" );
			    
	    }
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
		tabs++;
		println("else");
	    }
	    else {
  				// println("{");
	    }
	    tabs++;
	    println(noViableAction);
	    tabs--;
	    if ( howToFinish.generatedAnIf ) {
		println("end; -- if");
		tabs--;
	    }

	    if ( howToFinish.generatedSwitch )
		println("end; -- case");

	}

	if ( !howToFinish.needAnErrorClause && howToFinish.generatedSwitch )
	    println("end; -- case");

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
			    println( a.getLabel() + " : " + labeledElementType + " := " + labeledElementInit + ";");
			    if (grammar.buildAST) {
				println( a.getLabel() + "_ast : " + labeledElementASTType+ ";" );
			    }
			}
			else {
			    if (grammar.buildAST) {
				// Always gen AST variables for labeled elements, even if the
				// element itself is marked with !
				println( a.getLabel() + "_ast : " + labeledElementASTType+ ";" );
			    }
			    if ( grammar instanceof LexerGrammar ) {
				println( a.getLabel() + " : $ANTLR_TOKEN; " );
			    }	
			    if (grammar instanceof TreeWalkerGrammar) {
				// always generate rule-ref variables for tree walker
				println(  a.getLabel() + " : " + labeledElementType + " := " + labeledElementInit + ";");
			    }
			}
		    }
		    else {
			// It is a token or literal reference.  Generate the
			// correct variable type for this grammar
			println( a.getLabel() + " : " + labeledElementType + " := " + labeledElementInit + ";");
			// In addition, generate *_AST variables if building ASTs
			if (grammar.buildAST) {
			    println( a.getLabel() + "_ast : " + labeledElementASTType + ";");
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
	    if ( i == 0 )
		_print("when " + getValueString(elems[i]) );
	    else
		_print(", " + getValueString(elems[i]) );

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
    public JavaBlockFinishingInfo genCommonBlock(
						 AlternativeBlock blk, 
						 boolean noTestForSingle)
    {
	int nIF=0;
	boolean createdLL1Switch = false;
	int closingBracesOfIFSequence = 0;
	JavaBlockFinishingInfo finishingInfo = new JavaBlockFinishingInfo();
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
		println(blk.getLabel() + " := " + lt1Value + ";");
	    }

	    // AST

	    genElementAST(blk);

	    String astArgs="";
	    if (grammar instanceof TreeWalkerGrammar) {
		astArgs="sa_t,";
	    }

	    // match the bitset for the alternative
	    println("match( sa" + astArgs + getBitsetName(markBitsetForGen(p.fset)) + ");");

	    // tack on tree cursor motion if doing a tree walker
	    if (grammar instanceof TreeWalkerGrammar) {
		println("sa_t := sa_t.next_sibling;");
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
		println("if ( void(sa_t) ) then");
		tabs++;
		println("sa_t := AST::ASTNULL;");
		tabs--;
		println("end; -- if");
	    }
	    println("case ( " + testExpr + " )" );
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
		    println("then");
		    tabs++;
		    genAlt(alt, blk);
		    //					println("break;");
		    tabs--;
		    //					println("}");
		}
	    }
	    println("else -- default");
	    tabs++;
	}

	// do non-LL(1) and nondeterministic cases
	// This is tricky in the lexer, because of cases like:
	//     STAR : '*' ;
	//     ASSIGN_STAR : "*=";
	// Since nextToken is generated without a loop, then the STAR
	// will have end-of-token as it's lookahead set for LA(2).
	// So, we must generate the alternatives containing trailing
	// end-of-token in their lookahead sets *after* the
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
		if ( DEBUG_CODE_GENERATOR ) 
		    System.out.println("genAlt: "+i);
		// if we made a switch above, ignore what we already
		// took care of.  Specifically, LL(1) alts with no
		// preds that do not have end-of-token in their
		// prediction set
		if ( createdLL1Switch && suitableForCaseExpression(alt) ) {
		    if ( DEBUG_CODE_GENERATOR ) 
			System.out.println("ignoring alt because it was in the switch");
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
			   alt.cache[effectiveDepth].containsEpsilon()
			    ) 
			{
			    effectiveDepth--;
			}
		    // Ignore alts whose effective depth is other than
		    // the ones we are generating for this iteration.
		    if (effectiveDepth != altDepth) {
			if ( DEBUG_CODE_GENERATOR )
			    System.out.println("ignoring alt because effectiveDepth!=altDepth;"+effectiveDepth+"/="+altDepth);
			continue;
		    }
		    unpredicted = lookaheadIsEmpty(alt, effectiveDepth);
		    e = getLookaheadTestExpression(alt, effectiveDepth);
		} else {
		    unpredicted = lookaheadIsEmpty(alt, grammar.maxk);
		    e = getLookaheadTestExpression(alt, grammar.maxk);
		}

		boolean defaultBlock = true;

		// Was it a big unicode range that forced
		// unsuitability for a case expression?
		if ( alt.cache[1].fset.degree()>caseSizeThreshold ) {
		    if ( nIF==0 ) {
			println("if " + e + " then");
		    }
		    else {
			println("elsif " + e + " then");
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
			// println("{");
		    }
		    else {
			println("else ");
			defaultBlock = false;  // else is for an if statement, not a case statement.
		    }			
		    finishingInfo.needAnErrorClause = false;

		}
		else { // check for sem and syn preds

		    // Add any semantic predicate expression to the
		    // lookahead test

		    if ( alt.semPred != null ) {

			// if debugging, wrap the evaluation of the
			// predicate in a method
						
			// translate $ and # references
			ActionTransInfo tInfo = new ActionTransInfo();
			String actionStr = processActionForTreeSpecifiers(alt.semPred, 
									  blk.line, 
									  currentRule, 
									  tInfo);
			// ignore translation info...we don't need to
			// do anything with it.  call that will inform
			// SemanticPredicateListeners of the result

			if (((grammar instanceof ParserGrammar) || 
			     (grammar instanceof LexerGrammar)) && 
			    grammar.debuggingOutput )
			    e = "("+e+" and fireSemanticPredicateEvaluated(antlr.debug.SemanticPredicateEvent.PREDICTING,"+
				addSemPred(charFormatter.escapeString(actionStr))+","+actionStr+"))";
			else {
			    e = "("+e+" and ("+actionStr +"))";
			}
		    }

		    // Generate any syntactic predicates
		    if ( nIF>0 ) {
			if ( alt.synPred != null ) {
			    println("else");
			    tabs++;
			    genSynPred( alt.synPred, e );
			    closingBracesOfIFSequence++;
			}
			else {
			    println("elsif " + e + " then" );
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
				println("if ( void(sa_t) ) then");
				tabs++;
				println("sa_t := AST::ASTNULL;");
				tabs--;
				println("end; -- if");
			    }
			    println("if " + e + " then");
			}
		    }
					
		}	

		nIF++;
		tabs++;
		genAlt(alt, blk);
		tabs--;
		if ( !defaultBlock )
		    println("end; -- if");
	    }
	}

	String ps = "";
	for (int i=1; i<=closingBracesOfIFSequence; i++) {
	    ps+="end;";
	}

	// Restore the AST generation state
	genAST = savegenAST;
		
	// restore save text state
	saveText=oldsaveTest;

	// Return the finishing info.
	if ( createdLL1Switch ) {
	    tabs--;
	    finishingInfo.postscript = ps; // + "end; -- case";
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
		astName = "tmp" + astVarNumber + "_ast";
		astVarNumber++;
				// Map the generated AST variable in the alternate
		mapTreeVariable(el, astName);
				// Generate an "input" AST variable also
		println( astName + "_in : " + labeledElementASTType + " := " + elementRef + ";");
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
		astName = el.getLabel() + "_ast";
	    } else {
		elementRef = lt1Value;
				// Generate AST variables for unlabeled stuff
		astName = "tmp" + astVarNumber + "_ast";
		astVarNumber++;
		// Generate the declaration (can only build AST's for atoms
		GrammarAtom ga = (GrammarAtom)el;
		if ( ga.getASTNodeType()!=null ) {
		    println( astName + " : " + ga.getASTNodeType() + " := void;");
		}
		else {
		    println( astName + " : " + labeledElementASTType + ";" );
		}

		// Map the generated AST variable in the alternate
		mapTreeVariable(el, astName);
		if (grammar instanceof TreeWalkerGrammar) {
		    // Generate an "input" AST variable also
		    println(astName + "_in : " + labeledElementASTType + ";" );
		}
	    }

	    // Enclose actions with !guessing
	    if (doNoGuessTest) {
		println("if ( input_state.guessing = 0 ) then"); 
		tabs++;
	    }

	    // we need to find out the type of argument of
	    // toke_factory::create.  Sather cannot
	    // overload function unless their signatures
	    // can clearly be disambiguated.  Therefore the 
	    // AST::create( $ANTLR_AST ) : $ANTLR_AST 
	    //   and
	    // AST::create( $ANTLR_TOKEN ) : $ANTLR_AST
	    // cannot coexist.  Therefore we rename them
	    // create_ast_from_ast and create_ast_from_token, respectively.

	    String astType = labeledElementASTType;
	    GrammarAtom atom = (GrammarAtom)el;
	    if ( atom != null && atom.getASTNodeType() != null ) {
		astType = atom.getASTNodeType();
		// make this the current AST type, used
		// for supsequent AST create's, even though
		// the may be temporary AST's
		labeledElementASTType = astType; 
	    }		

	    String astCreateString;
	    if ( grammar instanceof TreeWalkerGrammar )
		astCreateString = astType + "::create_from_ast( " + elementRef + " )";
	    else
		astCreateString = astType + "::create_from_token( " + elementRef + " )";

	    if (el.getLabel() != null) {
		println(astName + " := " + astCreateString + ";" );
	    } else {
		elementRef = lt1Value;
		println(astName + " := " + astCreateString + ";" );
		// Map the generated AST variable in the alternate
		if (grammar instanceof TreeWalkerGrammar) {
		    // set "input" AST variable also
		    println(astName + "_in := " + elementRef + ";");
		}
	    }

	    if (genAST) {
		switch (el.getAutoGenType()) {
		case GrammarElement.AUTO_GEN_NONE:
		    println("current_ast.add_child( " + astName + " );");
		    break;
		case GrammarElement.AUTO_GEN_CARET:
		    println("current_ast.make_root( " + astName + " );");
		    break;
		default:
		    break;
		}
	    }
	    if (doNoGuessTest) {
		tabs--;
		println("end; -- if?");
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
	    println("}*28");
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
		println("when " + extractTypeOfAction(handler.exceptionTypeAndName) + " then");
		tabs++;
		if (grammar.hasSyntacticPredicate) {
		    println("if ( input_state.guessing = 0 ) then");
		    tabs++;
		}
			
		// When not guessing, execute user handler action
		printAction(
			    processActionForTreeSpecifiers(handler.action.getText(), 0, currentRule, null)
			    );
				
		if (grammar.hasSyntacticPredicate) {
		    tabs--;
		    println("else");
		    tabs++;
				// When guessing, rethrow exception
		    println("raise exception");
				// extractIdOfAction(handler.exceptionTypeAndName) + ";"
		    tabs--;
		    println("end; -- if");
		}
		tabs--;
	    }
	// Close catch phrase
	println("end; -- protect");


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

    /** Generate a header that is common to all Sather files */
    protected void genHeader() {
	println("-- $ANTLR "+Tool.version+": "+
		"\""+Tool.fileMinusPath(tool.grammarFile)+"\""+
		" -> "+
		"\""+grammar.getClassName()+".sa\"$");
    }

    private void genLiteralsTest() {
	println("sa_ttype := test_literals_table(sa_ttype);");
    }

    private void genLiteralsTestForPartialToken() {
	println("sa_ttype := test_literals_table( text.substring( sa_begin, text.length - sa_begin ), sa_ttype );");
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
	    astArgs="sa_t,";
	}
		
	// if in lexer and ! on element, save buffer index to kill later
	if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
	    println("sa_save_index := text.length;");
	}
		
	print(atom.not ? "match_not(" : "match(");
	_print(astArgs);
		
	// print out what to match
	if (atom.atomText.equals("EOF")) {
	    // horrible hack to handle EOF case
	    _print("ANTLR_COMMON_TOKEN::EOF_TYPE");
	} 
	else {
	    _print(atom.atomText);
	}
	_println(");");

	if ( grammar instanceof LexerGrammar && (!saveText||atom.getAutoGenType()==GrammarElement.AUTO_GEN_BANG) ) {
	    println("text := text.substring( 0 , sa_save_index);");		// kill text atom put in buffer
	}
    }
    protected void genMatchUsingAtomTokenType(GrammarAtom atom) {
	// match() for trees needs the _t cursor
	String astArgs="";
	if (grammar instanceof TreeWalkerGrammar) {
	    astArgs="sa_t,";
	}

	// If the literal can be mangled, generate the symbolic constant instead
	String mangledName = null;
	String s = astArgs + getValueString(atom.getType());

		// matching
	println( (atom.not ? "match_not(" : "match(") + s + ");");
    }

    /** Generate the nextToken() rule.  nextToken() is a synthetic
      * lexer rule that is the implicit OR of all user-defined lexer
      * rules.  
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
	    println("next_token : TOKEN is " );
	    tabs++;
	    println("protect");
	    tabs++;
	    println("upon_eof;");
	    tabs--;
	    println("when $ANTLR_CHAR_STREAM_EXCEPTION then");
	    tabs++;
	    println("raise #ANTLR_TOKEN_STREAM_EXCEPTION( exception.str );");
	    tabs--;
	    println("end; -- protect");
	    println("return #ANTLR_COMMON_TOKEN( ANTLR_COMMON_TOKEN::EOF_TYPE, \"\");");
	    tabs--;
	    println("end;");
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
	println("next_token : TOKEN is");
	tabs++;
	println("theRetToken : TOKEN;");
	println("continue : BOOL := true;");
	//		_println("tryAgain:");
	println("loop");
	tabs++;
	//		println("Token _token = null;");
	println("sa_ttype : INT := ANTLR_COMMON_TOKEN::INVALID_TYPE;");
	if ( ((LexerGrammar)grammar).filterMode ) {
	    println("commit_to_path := false;");
	    println("continue := true;");
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
		println("sa_m : INT := mark;");
	    }
	}
	println("reset_text;");

	println("protect   -- for char stream error handling");
	tabs++;

	// Generate try around whole thing to trap scanner errors
	println("protect   -- for lexical error handling");
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
	String errFinish = "if ( LA(1) = EOF_CHAR ) then upon_eof; sa_return_token := make_token( ANTLR_COMMON_TOKEN::EOF_TYPE);";
	errFinish += newline+"\t\t\t\t";
	if ( ((LexerGrammar)grammar).filterMode ) {
	    if ( filterRule==null ) {
		errFinish += "\telse consume; continue := false; end; -- if";
	    }
	    else {
		errFinish += "\telse"+newline+
		    "\t\t\t\t\t\tcommit;"+newline+
		    "\t\t\t\t\t\tprotect" +newline+
		    "\t\t\t\t\t\t\tm"+filterRule+"(false);"+newline+
		    "\t\t\t\t\t\twhen $ANTLR_RECOGNITION_EXCEPTION then"+newline+
		    "\t\t\t\t\t\t\t-- catastrophic failure"+newline+
		    "\t\t\t\t\t\t\treport_error( exception );"+newline+
		    "\t\t\t\t\t\t\tconsume;"+newline+
		    "\t\t\t\t\t\tend; -- protect"+newline+
		    "\t\t\t\t\t\tcontinue := false;"+newline+
		    "\t\t\t\t\tend; -- if";
	    }
	}
	else {
	    errFinish += "\t\telse " + throwNoViable + " end; -- if";
	}
	genBlockFinish(howToFinish, errFinish);
	// at this point a valid token has been matched, undo "mark" that was done
	if ( ((LexerGrammar)grammar).filterMode && filterRule!=null ) {
	    println("if continue then");
	    tabs++;
	    println("commit;");
	    tabs--;
	    println("end; -- if");
	}
		
	// Generate literals test if desired
	// make sure _ttype is set first; note _returnToken must be
	// non-null as the rule was required to create it.
	println("if ( ~void(sa_return_token) and continue ) then;");
	tabs++;
	println("sa_ttype := sa_return_token.ttype;");
	if ( ((LexerGrammar)grammar).getTestLiterals()) {
	    genLiteralsTest();
	}

	// return token created by rule reference in switch
	println("sa_return_token.ttype := sa_ttype;");
	println("return sa_return_token;");
	tabs--;
	println("end; -- if");

	// Close try block
	tabs--;
	println("when $ANTLR_RECOGNITION_EXCEPTION then");
	tabs++;
	if ( ((LexerGrammar)grammar).filterMode ) {
	    if ( filterRule==null ) {
		println("if ( ~commit_to_path ) then");
		tabs++;
		println("consume;");	
		tabs--;
		println("end; -- if");
	    }
	    else {
		println("if ( ~commit_to_path ) then");
		tabs++;
		println("rewind( sa_m );");
		println("reset_text;");
		println("protect");
		tabs++;
		println("m" + filterRule + "(false);");
		tabs--;
		println("when $ANTLR_RECOGNITION_EXCEPTION then");
		tabs++;
		println("-- horrendous failure: error in filter rule");
		println("report_error( exception );");
		println("consume;");
		tabs--;
		println("end; -- protect");
		tabs--;
		println("end; -- if");
	    }
	}
	else {
	    if ( nextTokenBlk.getDefaultErrorHandler() ) {
		println("report_error( exception );");
		println("consume;");
	    }
	    else {
		// pass on to invoking routine
		println("raise #ANTLR_TOKEN_STREAM_RECOGNITION_EXCEPTION( exception.str );");
	    }
	}
	tabs--;
	println("end; -- protect");

	// close CharStreamException try
	tabs--;
	println("when $ANTLR_CHAR_STREAM_EXCEPTION then");
	tabs++;
	println("raise #ANTLR_TOKEN_STREAM_EXCEPTION( exception.message );");
	tabs--;
	println("end; -- protect");

	// close for-loop
	tabs--;
	println("end; -- loop");

	// close method nextToken
	tabs--;
	//		println("}");
	println("end; -- next_token");
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

	// reset the AST type to the one specified in the
	// PARSER's or TREE_WALKER's class parameter list
	labeledElementASTType = "AST";

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
		
	// Gen method name
	print( s.getId() );

	// sather requires enclosing parens only when the arglist is non-empty
	boolean hasArgs = false;

	// Additional rule parameters common to all rules for this grammar
	if (commonExtraParams.length() != 0 ) {
	    hasArgs = true;
	    _print("( " + commonExtraParams);
	    if ( rblk.argAction != null ) 
		_print(",");
	}

	if ( rblk.argAction != null ) 
	    if ( !hasArgs ) {
		hasArgs = true;
		_print("( ");
	    }

	// Gen arguments
	if (rblk.argAction != null) 
	    {
		// Has specified arguments
		print(rblk.argAction);
	    }

	if ( hasArgs )
	    _print(" )");

	// Gen method return type (note lexer return action set at rule creation)
	if (rblk.returnAction != null)
	    {
		// Has specified return value
		_print( " : " + extractSatherTypeOfAction(rblk.returnAction) );
	    } 

	_println(" is");
	tabs++;

	// Convert return action to variable declaration
	if (rblk.returnAction != null)
	    println(rblk.returnAction + ";");
		
	// print out definitions needed by rules for various grammar types
	println(commonLocalVars);
		
	if (grammar.traceRules) {
	    if ( grammar instanceof TreeWalkerGrammar ) {
		println("trace_in(\""+ s.getId() +"\",sa_t);");
	    }
	    else {
		println("trace_in(\""+ s.getId() +"\");");
	    }
	}

	if ( grammar instanceof LexerGrammar ) {
	    // lexer rule default return value is the rule's token name
	    // This is a horrible hack to support the built-in EOF lexer rule.
	    if (s.getId().equals("mEOF"))
		println("sa_ttype := ANTLR_COMMON_TOKEN::EOF_TYPE;");
	    else
		println("sa_ttype := "+ s.getId().substring(1)+";");
	    println("sa_save_index : INT;");	 // used for element! (so we can kill text matched for element)
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
		println("fire_enter_rule( " + ruleNum + ", 0 );");
	    else if (grammar instanceof LexerGrammar)
		println("fire_enter_rule( " + ruleNum + ", sa_ttype );");
   		    

	// Generate trace code if desired
	if ( grammar.debuggingOutput || grammar.traceRules) {
	    println("protect -- debugging output");
	    tabs++;
	}
		
	// Initialize AST variables
	if (grammar instanceof TreeWalkerGrammar) {
	    // "Input" value for rule
	    println( s.getId() + "_ast_in : " + labeledElementASTType + " := sa_t;");
	}
	if (grammar.buildAST) {
	    // Parser member used to pass AST returns from rule invocations
	    println("return_ast := void;");
	    // Tracks AST construction
	    println("current_ast ::= #ANTLR_AST_PAIR{AST};");
	    // User-settable return value for rule.
	    println( s.getId() + "_ast : " + labeledElementASTType + ";" );
	}

	genBlockPreamble(rblk);
	println("");

	// Search for an unlabeled exception specification attached to the rule
	ExceptionSpec unlabeledUserSpec = rblk.findExceptionSpec("");

	// Generate try block around the entire rule for  error handling
	if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
	    println("protect -- for error handling");
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
	}

	// Generate user-defined or default catch phrases
	if (unlabeledUserSpec != null) {
	    genErrorHandler(unlabeledUserSpec);
	}
	else if (rblk.getDefaultErrorHandler()) {
	    // Generate default catch phrase
	    println("when " + exceptionThrown + " then");
	    tabs++;
	    // Generate code to handle error if not guessing
	    if (grammar.hasSyntacticPredicate) {
		println("if ( input_state.guessing = 0 ) then");
		tabs++;
	    }
	    println("report_error( exception );");
	    if ( !(grammar instanceof TreeWalkerGrammar) ) {
				// Generate code to consume until token in k==1 follow set
		Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(1, rblk.endNode);
		String followSetName = "sa" + getBitsetName(markBitsetForGen(follow.fset));
		println("consume;");
		println("consume_until( " + followSetName + " );");
	    } else {
				// Just consume one token
		println("if ( ~void(sa_t) ) then");
		tabs++;
		println("sa_t := sa_t.next_sibling;");
		tabs--;
		println("end; -- if");
	    }
	    if (grammar.hasSyntacticPredicate) {
		tabs--;
				// When guessing, rethrow exception
		println("else");
		tabs++;
		println("raise exception;");
		tabs--;
		println("end; -- if");
	    }
	    // Close catch phrase
	    tabs--;
	    println("end; -- protect");
	}

	// Squirrel away the AST "return" value
	if (grammar.buildAST) {
	    println("return_ast := " + s.getId() + "_ast;");
	}

	// Set return tree value for tree walkers
	if ( grammar instanceof TreeWalkerGrammar ) {
	    println("sa_ret_tree := sa_t;");
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
	    println("if ( sa_create_token and void(sa_token) and sa_ttype /= ANTLR_COMMON_TOKEN::SKIP ) then");
	    tabs++;
	    println("sa_token := make_token( sa_ttype );");
	    println("sa_token.text := text.substring( sa_begin, text.length - sa_begin );");
	    tabs--;
	    println("end; -- if");
	    println("sa_return_token := sa_token;");
	}	

	// Gen the return statement if there is one (lexer has hard-wired return action)
	if (rblk.returnAction != null) {
	    println("return " + extractSatherIdOfAction( rblk.returnAction, rblk.getLine() ) + ";");
	}
		
	if ( grammar.debuggingOutput || grammar.traceRules) {

	    // since Sather doesn't have anything similar to
	    // Java's try..finally, we use a protect..when
	    // instead.  However, we mimic finally by printing
	    // debugging statements regardless of whether an
	    // exception is thrown

	    tabs--;
	    println("when $STR then -- assume this will catch everything");		
	    tabs++;

	    // cache debugging statements since they will have
	    // to printed twice
	    String fire = null;
	    String trace = null;

	    // If debugging, generate calls to mark exit of rule
	    if ( grammar.debuggingOutput)
		if (grammar instanceof ParserGrammar)
		    fire = "fire_exit_rule(" + ruleNum + ",0);";
		else if (grammar instanceof LexerGrammar)
		    fire = "fire_exit_rule(" + ruleNum + ", sa_ttype);";
   		    
	    if (grammar.traceRules) {
		if ( grammar instanceof TreeWalkerGrammar ) {
		    trace = "trace_out(\""+ s.getId() +"\", sa_t);";
		}
		else {
		    trace = "trace_out(\""+ s.getId() +"\");";
		}
	    }

	    // these are printed _inside_ the when class of the
	    // Sather protect statement.
	    if ( fire != null )
		println(fire);

	    if ( trace != null )
		println(trace);

	    // rethrow exception
	    println("raise exception;");
	    tabs--;
	    println("end; -- protect");

	    // these are printed _outside_ the when class of the
	    // Sather protect statement.
	    if ( fire != null )
		println(fire);

	    if ( trace != null )
		println(trace);
	}

	tabs--;
	println("end; -- rule");
	println("");
		
	// Restore the AST generation state
	genAST = savegenAST;
		
	// restore char save state
	// saveText = oldsaveTest;
    }
    private void GenRuleInvocation(RuleRefElement rr) {	
	// dump rule name
	_print( rr.targetRule );
			
	boolean hasArgs = false; // flag to let us know if we need to close the arg list
	// lexers must tell rule if it should set _returnToken
	if ( grammar instanceof LexerGrammar ) {
	    // if labeled, could access Token, so tell rule to create
	    hasArgs = true;
	    if ( rr.getLabel() != null ) {
		_print("( true");
	    }
	    else {
		_print("( false");
	    }		
	    if (commonExtraArgs.length() != 0 || rr.args!=null ) {
		_print(",");
	    }
	}	
	else if ( commonExtraArgs.length() != 0 || rr.args != null ) {
	    hasArgs = true;
	    _print( "( " );
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

	if ( hasArgs ) 
	    _print(" )");

	_println(";");
		
	// move down to the first child while parsing
	if ( grammar instanceof TreeWalkerGrammar ) {
	    println("sa_t := sa_ret_tree;");
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
	println("if (~(" + pred + ")) then");
	tabs++;
	println("raise #ANTLR_SEMANTIC_EXCEPTION(\"" + escapedPred + "\");");
	tabs--;
	println("end;");
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
	println("syn_pred_matched" + blk.ID + " : BOOL := false;");
	// Gen normal lookahead test
	println("if (" + lookaheadExpr + ") then");
	tabs++;

	// Save input state
	if ( grammar instanceof TreeWalkerGrammar ) {
	    println("sa__t" + blk.ID + " " + labeledElementASTType + " := sa_t;");
	}
	else {
	    println("sa_m" + blk.ID + " : INT := mark;");
	}

	// Once inside the try, assume synpred works unless exception caught
	println("syn_pred_matched" + blk.ID + " := true;");
	println("input_state.guessing := input_state.guessing + 1;");

	// if debugging, tell listeners that a synpred has started
	if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) ||
					(grammar instanceof LexerGrammar))) {
	    println("fireSyntacticPredicateStarted();");
	}	

	syntacticPredLevel++;
	println("protect");
	tabs++;
	gen((AlternativeBlock)blk);		// gen code to test predicate
	tabs--;
	//println("System.out.println(\"pred "+blk+" succeeded\");");
	println( "when " + exceptionThrown + " then" );
	tabs++;
	println( "syn_pred_matched" + blk.ID +" := false;");
	//println("System.out.println(\"pred "+blk+" failed\");");
	tabs--;
	println("end; -- protect");

	// Restore input state
	if ( grammar instanceof TreeWalkerGrammar ) {
	    println("sa_t := sa__t" + blk.ID + ";");
	}
	else {
	    println("rewind( sa_m" + blk.ID + " );");
	}

	println("input_state.guessing := input_state.guessing - 1;");

	// if debugging, tell listeners how the synpred turned out
	if (grammar.debuggingOutput && ((grammar instanceof ParserGrammar) ||
					(grammar instanceof LexerGrammar))) {
	    println("if ( syn_pred_matched" + blk.ID + " ) then" );
	    tabs++;
	    println("fireSyntacticPredicateSucceeded();" );
	    tabs--;
	    println("else");
	    tabs++;
	    println("  fireSyntacticPredicateFailed();");
	    tabs--;
	    println("end; -- if");
	}	

	syntacticPredLevel--;
	tabs--;
		
	// Close lookahead test
	println("end; -- if");

	// Test synred result
	println("if ( syn_pred_matched" + blk.ID + " ) then");
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
	println("const sa_token_names : ARRAY{STR} := |");
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
	println("|;");
    }
    /** Generate the token types Java file */
    protected void genTokenTypes(TokenManager tm) throws IOException {
	// Open the token output Java file and set the currentOutput stream
	// SAS: file open was moved to a method so a subclass can override
	//      This was mainly for the VAJ interface
	setupOutput(tm.getName() + "_" + TokenTypesFileSuffix.toUpperCase() );

	tabs = 0;

	// Generate the header common to all Java files
	genHeader();
	// Do not use printAction because we assume tabs==0
	println(behavior.getHeaderAction(""));

	// Encapsulate the definitions in an interface.  This can be done
	// because they are all constants.
	println("class " + tm.getName() + "_" + TokenTypesFileSuffix.toUpperCase() +" is");
	tabs++;

		
	// Generate a definition for each token type
	Vector v = tm.getVocabulary();
		
	// Do special tokens manually
	println("const EOF : INT := " + Token.EOF_TYPE + ";");
	println("const NULL_TREE_LOOKAHEAD : INT := " + Token.NULL_TREE_LOOKAHEAD + ";");
		
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
			println( "const " + sl.label + " : INT" + " := " + i + ";");
		    }
		    else {	
			String mangledName = mangleLiteral(s);
			if (mangledName != null) {
			    // We were able to create a meaningful mangled token name
			    println( "const " + mangledName + " : INT" + " := " + i + ";");
			    // if no label specified, make the label equal to the mangled name
			    sl.label = mangledName;
			}
			else {
			    println("-- " + s + " := " + i);
			}
		    }	
		}
		else if ( !s.startsWith("<") ) {
		    println( "const " + s + " : INT" + " := " + i + ";");
		}
	    }
	}

	// convert the CHAR array into CHAR_SET instance
	println("");
	println("bitset ( bool_array : ARRAY{BOOL} ) : CHAR_SET is");
	tabs++;
	println( "return #CHAR_SET( bool_array );" );
	tabs--;
	println("end;");
		

	// convert the INT array into INT_SET instance
	println("");
	println("int_set ( int_array : ARRAY{INT} ) : INT_SET is");
	tabs++;
	println( "return #INT_SET( int_array );" );
	tabs--;
	println("end;");

	// Close the interface
	tabs--;
	println("end; -- class");

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
	buf.append("ANTLR_AST_UTIL{AST}::make( ( #ANTLR_AST_ARRAY{AST}(" + v.size() + "))");
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
	if ( atom!=null && atom.getASTNodeType() != null ) {
	    return atom.getASTNodeType() + "::create("+str + ")";
	}
	else {
	    return labeledElementASTType + "::create( " + str + " )";
	}
    }

    protected String getLookaheadTestExpression(Lookahead[] look, int k) 
    {

	StringBuffer e = new StringBuffer(100);
	boolean first = true;

	e.append("(");
	for (int i = 1; i <= k; i++) {
	    BitSet p = look[i].fset;
	    if (!first) {
		e.append(") and (");
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
	  e.append(") and (");
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

	if ( degree >= bitsetTestThreshold) {
	    int bitsetIdx = markBitsetForGen(p);
	    return "sa" + getBitsetName(bitsetIdx) + ".member(" + ts + ")";
	}

	// Otherwise, generate the long-winded series of "x==X||" tests
	e = new StringBuffer();
	for (int i = 0; i < elems.length; i++) {
	    // Get the compared-to item (token or character value)
	    String cs = getValueString(elems[i]);

	    // Generate the element comparison
	    if ( i>0 ) e.append(" or ");
	    e.append(ts);
	    e.append("=");
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
	    "(" + lookaheadString(k) + " >= " + getValueString(begin) + " and " + 
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
	    return "sa_t.ttype";
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
		return in_var ? id : id + "_ast";
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
	    String r = in_var ? id + "_ast_in" : id + "_ast";
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
	    /*
	      if ( g.hasOption("ASTLabelType") ) {
	      Token tsuffix = g.getOption("ASTLabelType");
	      if ( tsuffix != null ) {
	      String suffix = Tool.stripFrontBack(tsuffix.getText(),"\"","\"");
	      if ( suffix != null ) {
	      labeledElementASTType = suffix;
	      }
	      }		
	      }
	    */
	    labeledElementType = "$ANTLR_TOKEN";
	    labeledElementInit = "void";
	    commonExtraArgs = "";
	    commonExtraParams = "";
	    commonLocalVars = "";
	    lt1Value = "LT(1)";
	    exceptionThrown = "$ANTLR_RECOGNITION_EXCEPTION";
	    throwNoViable = "raise ANTLR_NO_VIABLE_ALT_EXCEPTION{AST}::create_from_token(LT(1), file_name );";
	}
	else if (g instanceof LexerGrammar) {
	    labeledElementType = "CHAR ";
	    labeledElementInit = "'\\0'";
	    commonExtraArgs = "";
	    commonExtraParams = "sa_create_token : BOOL";
	    commonLocalVars = "sa_ttype : INT; sa_token : TOKEN; sa_begin : INT := text.length;";
	    lt1Value = "LA(1)";
	    exceptionThrown = "$ANTLR_RECOGNITION_EXCEPTION";
	    throwNoViable = "raise #ANTLR_NO_VIABLE_ALT_FOR_CHAR_EXCEPTION( LA(1), file_name, line );";
	}
	else if (g instanceof TreeWalkerGrammar) {
	    labeledElementASTType = "AST";
	    labeledElementType = labeledElementASTType;
	    /*
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
	    */
	    if ( !g.hasOption("ASTLabelType") ) {
		g.setOption("ASTLabelType", new Token(ANTLRTokenTypes.STRING_LITERAL,"AST"));
	    }	
	    labeledElementInit = "void";
	    commonExtraArgs = "sa_t";
	    commonExtraParams = "sa_t : " + labeledElementASTType;
	    commonLocalVars = "";
	    lt1Value = "sa_t";
	    exceptionThrown = "$ANTLR_RECOGNITION_EXCEPTION";
	    throwNoViable = "raise #ANTLR_NO_VIABLE_ALT_EXCEPTION{AST}(sa_t);";
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
	currentOutput = antlr.Tool.openOutputFile(className + ".sa");
    }

    private static int satherBlockId = 0;
    private static synchronized String getNextSatherPrefix() {

	// This method is required to overcome Sather's lack of
	// multiple/nested scopes within a method.  It is used as a
	// prefix to newly declared variables. The prefix disambiguates
	// varaible names that in the Java/Cpp code generators would have
	// resided in their own scope.

	String label = "sa" + satherBlockId;
	satherBlockId++;
	return label;
    }

    protected String extractSatherTypeOfAction(String s) {
	int s_length = s.length();
	for (int i = s_length - 1 ; i >=0 ; i-- )  {
	    if ( s.charAt(i) == ':' ) {
		return s.substring( i+1 , s_length );
	    }
	}
	tool.warning("Unable to determine Sather type" );
	return "";
    }

    protected String extractSatherIdOfAction(String s, int line) {
	int s_length = s.length();
	for (int i = s_length - 1 ; i >=0 ; i-- )  {
	    if ( s.charAt(i) == ':' ) {
		return s.substring( 0 , i );
	    }
	}
	tool.warning("Unable to determine Sather return identifier");
	return "";
    }

    /** Lexically process tree-specifiers in the action.
     *  This will replace @id and @(...) with the appropriate
     *  function calls and/or variables.
     * 
     *  Override the default implementation inherited from CodeGenerator
     *  in order to instantiate the Sather's ActionLexer rather than Java's
     */

    protected String processActionForTreeSpecifiers( String actionStr, 
						     int line, 
						     RuleBlock currentRule, 
						     ActionTransInfo tInfo ) 
    {
	if ( actionStr==null || actionStr.length() == 0 ) return null;
	// The action trans info tells us (at the moment) whether an
	// assignment was done to the rule's tree root.
	if (grammar==null) return actionStr;
	if ( (grammar.buildAST && actionStr.indexOf('@') != -1) ||
	     grammar instanceof TreeWalkerGrammar ||
	     (grammar instanceof LexerGrammar && actionStr.indexOf('%') != -1) ) {
	    // Create a lexer to read an action and return the translated version
	    antlr.actions.sather.ActionLexer lexer = 
		new antlr.actions.sather.ActionLexer(actionStr, currentRule, this, tInfo);
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

}

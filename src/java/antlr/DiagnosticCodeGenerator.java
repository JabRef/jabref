package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import java.util.Enumeration;
import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;
import java.io.PrintWriter; //SAS: changed for proper text file io
import java.io.IOException;
import java.io.FileWriter;

/**Generate MyParser.txt, MyLexer.txt and MyParserTokenTypes.txt */
public class DiagnosticCodeGenerator extends CodeGenerator {
	/** non-zero if inside syntactic predicate generation */
	protected int syntacticPredLevel = 0;

	/** true during lexer generation, false during parser generation */
	protected boolean doingLexRules = false;


	/** Create a Diagnostic code-generator using the given Grammar
	 * The caller must still call setTool, setBehavior, and setAnalyzer
	 * before generating code.
	 */
	public DiagnosticCodeGenerator() {
		super();
		charFormatter = new JavaCharFormatter();
	}
	/**Generate the parser, lexer, and token types documentation */
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
				g.generate();
	
				if (tool.hasError) {
					System.out.println("Exiting due to errors.");
					System.exit(1);
				}

			}

			// Loop over all token managers (some of which are lexers)
			Enumeration tmIter = behavior.tokenManagers.elements();
			while (tmIter.hasMoreElements()) {
				TokenManager tm = (TokenManager)tmIter.nextElement();
				if (!tm.isReadOnly()) {
					// Write the token manager tokens as Java
					genTokenTypes(tm);
				}
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
		if (action.isSemPred) {
			// handled elsewhere
		}
		else {
			print("ACTION: ");
			_printAction(action.actionText);
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The "x|y|z|..." block to generate
	 */
	public void gen(AlternativeBlock blk) {
		println("Start of alternative block.");
		tabs++;
		genBlockPreamble(blk);

		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);
		if (!ok) {
			println("Warning: This alternative block is non-deterministic");
		}
		genCommonBlock(blk);
		tabs--;
	}
	/** Generate code for the given grammar element.
	 * @param blk The block-end element to generate.  Block-end
	 * elements are synthesized by the grammar parser to represent
	 * the end of a block.
	 */
	public void gen(BlockEndElement end) {
		// no-op
	}
	/** Generate code for the given grammar element.
	 * @param blk The character literal reference to generate
	 */
	public void gen(CharLiteralElement atom) {
		print("Match character ");
		if (atom.not) {
			_print("NOT ");
		}
		_print(atom.atomText);
		if (atom.label != null) {
			_print(", label=" + atom.label);
		}
		_println("");
	}
	/** Generate code for the given grammar element.
	 * @param blk The character-range reference to generate
	 */
	public void gen(CharRangeElement r) {
		print("Match character range: " + r.beginText + ".." + r.endText);
		if ( r.label!=null ) {
			_print(", label = " + r.label);
		}
		_println("");
	}
	/** Generate the lexer TXT file */
	public void gen(LexerGrammar g) throws IOException {
		setGrammar(g);
		System.out.println("Generating " + grammar.getClassName() + TokenTypesFileExt);
		currentOutput = antlr.Tool.openOutputFile(grammar.getClassName() + TokenTypesFileExt);
		//SAS: changed for proper text file io
		
		tabs=0;
		doingLexRules = true;

		// Generate header common to all TXT output files
		genHeader();

		// Output the user-defined lexer premamble
		println("");
		println("*** Lexer Preamble Action.");
		println("This action will appear before the declaration of your lexer class:");
		tabs++;
		println(grammar.preambleAction.getText());
		tabs--;
		println("*** End of Lexer Preamble Action");

		// Generate lexer class definition
		println("");
		println("*** Your lexer class is called '" + grammar.getClassName() + "' and is a subclass of '" + grammar.getSuperClass() + "'.");

		// Generate user-defined parser class members
		println("");
		println("*** User-defined lexer  class members:");
		println("These are the member declarations that you defined for your class:");
		tabs++;
		printAction(grammar.classMemberAction.getText());
		tabs--;
		println("*** End of user-defined lexer class members");

		// Generate string literals
		println("");
		println("*** String literals used in the parser");
		println("The following string literals were used in the parser.");
		println("An actual code generator would arrange to place these literals");
		println("into a table in the generated lexer, so that actions in the");
		println("generated lexer could match token text against the literals.");
		println("String literals used in the lexer are not listed here, as they");
		println("are incorporated into the mainstream lexer processing.");
		tabs++;
		// Enumerate all of the symbols and look for string literal symbols
		Enumeration ids = grammar.getSymbols();
		while ( ids.hasMoreElements() ) {
			GrammarSymbol sym = (GrammarSymbol)ids.nextElement();
			// Only processing string literals -- reject other symbol entries
			if ( sym instanceof StringLiteralSymbol ) {
				StringLiteralSymbol s = (StringLiteralSymbol)sym;
				println(s.getId() + " = " + s.getTokenType());
			}
		}
		tabs--;
		println("*** End of string literals used by the parser");

		// Generate nextToken() rule.
		// nextToken() is a synthetic lexer rule that is the implicit OR of all
		// user-defined lexer rules.
		genNextToken();

		// Generate code for each rule in the lexer
		println("");
		println("*** User-defined Lexer rules:");
		tabs++;
		
		ids = grammar.rules.elements();
		while ( ids.hasMoreElements() ) {
			RuleSymbol rs = (RuleSymbol)ids.nextElement();
			if (!rs.id.equals("mnextToken")) {
				genRule(rs);
			}
		}

		tabs--;
		println("");
		println("*** End User-defined Lexer rules:");

		// Close the lexer output file
		currentOutput.close();
		currentOutput = null;
		doingLexRules = false;
	}
	/** Generate code for the given grammar element.
	 * @param blk The (...)+ block to generate
	 */
	public void gen(OneOrMoreBlock blk) {
		println("Start ONE-OR-MORE (...)+ block:");
		tabs++;
		genBlockPreamble(blk);
		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);
		if (!ok) {
			println("Warning: This one-or-more block is non-deterministic");
		}
		genCommonBlock(blk);
		tabs--;
		println("End ONE-OR-MORE block.");
	}
	/** Generate the parser TXT file */
	public void gen(ParserGrammar g) throws IOException {
		setGrammar(g);
		// Open the output stream for the parser and set the currentOutput
		System.out.println("Generating " + grammar.getClassName() + TokenTypesFileExt);
		currentOutput = antlr.Tool.openOutputFile(grammar.getClassName()+TokenTypesFileExt);
		//SAS: changed for proper text file io
		
		tabs = 0;

		// Generate the header common to all output files.
		genHeader();
		
		// Output the user-defined parser premamble
		println("");
		println("*** Parser Preamble Action.");
		println("This action will appear before the declaration of your parser class:");
		tabs++;
		println(grammar.preambleAction.getText());
		tabs--;
		println("*** End of Parser Preamble Action");

		// Generate parser class definition
		println("");
		println("*** Your parser class is called '" + grammar.getClassName() + "' and is a subclass of '" + grammar.getSuperClass() + "'.");

		// Generate user-defined parser class members
		println("");
		println("*** User-defined parser class members:");
		println("These are the member declarations that you defined for your class:");
		tabs++;
		printAction(grammar.classMemberAction.getText());
		tabs--;
		println("*** End of user-defined parser class members");

		// Generate code for each rule in the grammar
		println("");
		println("*** Parser rules:");
		tabs++;

		// Enumerate the parser rules
		Enumeration rules = grammar.rules.elements();
		while ( rules.hasMoreElements() ) {
			println("");
			// Get the rules from the list and downcast it to proper type
			GrammarSymbol sym = (GrammarSymbol) rules.nextElement();
			// Only process parser rules
			if ( sym instanceof RuleSymbol) {
				genRule((RuleSymbol)sym);
			}
		}
		tabs--;
		println("");
		println("*** End of parser rules");

		println("");
		println("*** End of parser");

		// Close the parser output stream
		currentOutput.close();
		currentOutput = null;
	}
	/** Generate code for the given grammar element.
	 * @param blk The rule-reference to generate
	 */
	public void gen(RuleRefElement rr) {
		RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);

		// Generate the actual rule description
		print("Rule Reference: " + rr.targetRule);
		if (rr.idAssign != null) {
			_print(", assigned to '" + rr.idAssign + "'");
		}
		if (rr.args != null) {
			_print(", arguments = " + rr.args);
		}
		_println("");

		// Perform diagnostics
		if (rs == null || !rs.isDefined())
		{
			println("Rule '" + rr.targetRule + "' is referenced, but that rule is not defined.");
			println("\tPerhaps the rule is misspelled, or you forgot to define it.");
			return;
		}
		if (!(rs instanceof RuleSymbol))
		{
			// Should this ever happen??
			println("Rule '" + rr.targetRule + "' is referenced, but that is not a grammar rule.");
			return;
		}
		if (rr.idAssign != null)
		{
			// Warn if the rule has no return type
			if (rs.block.returnAction == null)
			{
				println("Error: You assigned from Rule '" + rr.targetRule + "', but that rule has no return type.");
			}
		} else {
			// Warn about return value if any, but not inside syntactic predicate
			if (!(grammar instanceof LexerGrammar) && syntacticPredLevel == 0 && rs.block.returnAction != null)
			{
				println("Warning: Rule '" + rr.targetRule + "' returns a value");
			}
		}
		if (rr.args != null && rs.block.argAction == null) {
			println("Error: Rule '" + rr.targetRule + "' accepts no arguments.");
		}
	}
	/** Generate code for the given grammar element.
	 * @param blk The string-literal reference to generate
	 */
	public void gen(StringLiteralElement atom) {
		print("Match string literal ");
		_print(atom.atomText);
		if (atom.label != null) {
			_print(", label=" + atom.label);
		}
		_println("");
	}
	/** Generate code for the given grammar element.
	 * @param blk The token-range reference to generate
	 */
	public void gen(TokenRangeElement r) {
		print("Match token range: " + r.beginText + ".." + r.endText);
		if ( r.label!=null ) {
			_print(", label = " + r.label);
		}
		_println("");
	}
	/** Generate code for the given grammar element.
	 * @param blk The token-reference to generate
	 */
	public void gen(TokenRefElement atom) {
		print("Match token ");
		if (atom.not) {
			_print("NOT ");
		}
		_print(atom.atomText);
		if (atom.label != null) {
			_print(", label=" + atom.label);
		}
		_println("");
	}
	public void gen(TreeElement t) {
		print("Tree reference: "+t);
	}
	/** Generate the tree-walker TXT file */
	public  void gen(TreeWalkerGrammar g) throws IOException {
		setGrammar(g);
		// Open the output stream for the parser and set the currentOutput
		System.out.println("Generating " + grammar.getClassName() + TokenTypesFileExt);
		currentOutput = antlr.Tool.openOutputFile(grammar.getClassName()+TokenTypesFileExt);
		//SAS: changed for proper text file io
		
		tabs = 0;

		// Generate the header common to all output files.
		genHeader();
		
		// Output the user-defined parser premamble
		println("");
		println("*** Tree-walker Preamble Action.");
		println("This action will appear before the declaration of your tree-walker class:");
		tabs++;
		println(grammar.preambleAction.getText());
		tabs--;
		println("*** End of tree-walker Preamble Action");

		// Generate tree-walker class definition
		println("");
		println("*** Your tree-walker class is called '" + grammar.getClassName() + "' and is a subclass of '" + grammar.getSuperClass() + "'.");

		// Generate user-defined tree-walker class members
		println("");
		println("*** User-defined tree-walker class members:");
		println("These are the member declarations that you defined for your class:");
		tabs++;
		printAction(grammar.classMemberAction.getText());
		tabs--;
		println("*** End of user-defined tree-walker class members");

		// Generate code for each rule in the grammar
		println("");
		println("*** tree-walker rules:");
		tabs++;

		// Enumerate the tree-walker rules
		Enumeration rules = grammar.rules.elements();
		while ( rules.hasMoreElements() ) {
			println("");
			// Get the rules from the list and downcast it to proper type
			GrammarSymbol sym = (GrammarSymbol) rules.nextElement();
			// Only process tree-walker rules
			if ( sym instanceof RuleSymbol) {
				genRule((RuleSymbol)sym);
			}
		}
		tabs--;
		println("");
		println("*** End of tree-walker rules");

		println("");
		println("*** End of tree-walker");

		// Close the tree-walker output stream
		currentOutput.close();
		currentOutput = null;
	}
	/** Generate a wildcard element */
	public void gen(WildcardElement wc) {
		print("Match wildcard");
		if ( wc.getLabel()!=null ) {
			_print(", label = " + wc.getLabel());
		}
		_println("");
	}
	/** Generate code for the given grammar element.
	 * @param blk The (...)* block to generate
	 */
	public void gen(ZeroOrMoreBlock blk) {
		println("Start ZERO-OR-MORE (...)+ block:");
		tabs++;
		genBlockPreamble(blk);
		boolean ok = grammar.theLLkAnalyzer.deterministic(blk);
		if (!ok) {
			println("Warning: This zero-or-more block is non-deterministic");
		}
		genCommonBlock(blk);
		tabs--;
		println("End ZERO-OR-MORE block.");
	}
	protected void genAlt(Alternative alt) {
		for (
			AlternativeElement elem = alt.head;
			!(elem instanceof BlockEndElement);
			elem = elem.next
		)
		{
			elem.generate();
		}
		if (alt.getTreeSpecifier() != null) 
		{
			println("AST will be built as: " + alt.getTreeSpecifier().getText());
		}
	}
	/** Generate the header for a block, which may be a RuleBlock or a
	 * plain AlternativeBLock.  This generates any variable declarations,
	 * init-actions, and syntactic-predicate-testing variables.
	 * @blk The block for which the preamble is to be generated.
	 */
	protected void genBlockPreamble(AlternativeBlock blk) {
		// dump out init action
		if ( blk.initAction!=null ) {
			printAction("Init action: " + blk.initAction);
		}
	}
	/**Generate common code for a block of alternatives; return a postscript
	 * that needs to be generated at the end of the block.  Other routines
	 * may append else-clauses and such for error checking before the postfix
	 * is generated.
	 */
	public void genCommonBlock(AlternativeBlock blk) {
		boolean singleAlt = (blk.alternatives.size() == 1);

		println("Start of an alternative block.");
		tabs++;
		println("The lookahead set for this block is:");
		tabs++;
		genLookaheadSetForBlock(blk);
		tabs--;

		if (singleAlt) {
			println("This block has a single alternative");
			if (blk.getAlternativeAt(0).synPred != null)
			{
				// Generate a warning if there is one alt and it has a synPred
				println("Warning: you specified a syntactic predicate for this alternative,");
				println("and it is the only alternative of a block and will be ignored.");
			}
		}
		else {
			println("This block has multiple alternatives:");
			tabs++;
		}

		for (int i=0; i<blk.alternatives.size(); i++) {
			Alternative alt = blk.getAlternativeAt(i);
			AlternativeElement elem = alt.head;

			// Print lookahead set for alternate
			println("");
			if (i != 0) {
				print("Otherwise, ");
			} else {
				print("");
			}
			_println("Alternate(" + (i+1) + ") will be taken IF:");
			println("The lookahead set: ");
			tabs++;
			genLookaheadSetForAlt(alt);
			tabs--;
			if ( alt.semPred != null || alt.synPred != null ) {
				print("is matched, AND ");
			} else {
				println("is matched.");
			}

			// Dump semantic predicates
			if ( alt.semPred != null ) {
				_println("the semantic predicate:");
				tabs++;
				println(alt.semPred);
				if ( alt.synPred != null ) {
					print("is true, AND ");
				} else {
					println("is true.");
				}
			}

			// Dump syntactic predicate
			if ( alt.synPred != null ) {
				_println("the syntactic predicate:");
				tabs++;
				genSynPred( alt.synPred );
				tabs--;
				println("is matched.");
			}

			// Dump the alternative
			genAlt(alt);
		}
		println("");
		println("OTHERWISE, a NoViableAlt exception will be thrown");
		println("");

		if (!singleAlt) {
			tabs--;
			println("End of alternatives");
		}
		tabs--;
		println("End of alternative block.");
	}
	/** Generate a textual representation of the follow set
	 * for a block.
	 * @param blk  The rule block of interest
	 */
	public void genFollowSetForRuleBlock(RuleBlock blk)
	{
		Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(1, blk.endNode);
		printSet(grammar.maxk, 1, follow);
	}
	/** Generate a header that is common to all TXT files */
	protected void genHeader() 
	{
		println("ANTLR-generated file resulting from grammar " + tool.grammarFile);
		println("Diagnostic output");
		println("");
		println("Terence Parr, MageLang Institute");
		println("with John Lilley, Empathy Software");
		println("ANTLR Version "+Tool.version+"; 1996,1997");
		println("");
		println("*** Header Action.");
		println("This action will appear at the top of all generated files.");
		tabs++;
		printAction(behavior.getHeaderAction(""));
		tabs--;
		println("*** End of Header Action");
		println("");
	}
	/**Generate the lookahead set for an alternate. */
	protected void genLookaheadSetForAlt(Alternative alt) {
		if ( doingLexRules && alt.cache[1].containsEpsilon() ) {
			println("MATCHES ALL");
			return;
		}
		int depth = alt.lookaheadDepth;
		if ( depth == GrammarAnalyzer.NONDETERMINISTIC ) {
			// if the decision is nondeterministic, do the best we can: LL(k)
			// any predicates that are around will be generated later.
			depth = grammar.maxk;
		}
		for (int i = 1; i <= depth; i++)
		{
			Lookahead lookahead = alt.cache[i];
			printSet(depth, i, lookahead);
		}
	}
	/** Generate a textual representation of the lookahead set
	 * for a block.
	 * @param blk  The block of interest
	 */
	public void genLookaheadSetForBlock(AlternativeBlock blk)
	{
		// Find the maximal lookahead depth over all alternatives
		int depth = 0;
		for (int i=0; i<blk.alternatives.size(); i++) {
			Alternative alt = blk.getAlternativeAt(i);
			if (alt.lookaheadDepth == GrammarAnalyzer.NONDETERMINISTIC) {
				depth = grammar.maxk;
				break;
			} 
			else if (depth < alt.lookaheadDepth) {
				depth = alt.lookaheadDepth;
			}
		}

		for (int i = 1; i <= depth; i++)
		{
			Lookahead lookahead = grammar.theLLkAnalyzer.look(i, blk);
			printSet(depth, i, lookahead);
		}
	}
	/** Generate the nextToken rule.
	 * nextToken is a synthetic lexer rule that is the implicit OR of all
	 * user-defined lexer rules.
	 */
	public void genNextToken() {
		println("");
		println("*** Lexer nextToken rule:");
		println("The lexer nextToken rule is synthesized from all of the user-defined");
		println("lexer rules.  It logically consists of one big alternative block with");
		println("each user-defined rule being an alternative.");
		println("");

		// Create the synthesized rule block for nextToken consisting
		// of an alternate block containing all the user-defined lexer rules.
		RuleBlock blk = MakeGrammar.createNextTokenRule(grammar, grammar.rules, "nextToken");

		// Define the nextToken rule symbol
		RuleSymbol nextTokenRs = new RuleSymbol("mnextToken");
		nextTokenRs.setDefined();
		nextTokenRs.setBlock(blk);
		nextTokenRs.access = "private";
		grammar.define(nextTokenRs);

		// Analyze the synthesized block
		if (!grammar.theLLkAnalyzer.deterministic(blk))
		{
			println("The grammar analyzer has determined that the synthesized");
			println("nextToken rule is non-deterministic (i.e., it has ambiguities)");
			println("This means that there is some overlap of the character");
			println("lookahead for two or more of your lexer rules.");
		}

		genCommonBlock(blk);

		println("*** End of nextToken lexer rule.");
	}
	/** Generate code for a named rule block
	 * @param s The RuleSymbol describing the rule to generate
	*/
	public void genRule(RuleSymbol s) {
		println("");
		String ruleType = (doingLexRules ? "Lexer" : "Parser");
		println("*** " + ruleType + " Rule: " + s.getId());
		if (!s.isDefined() ) {
			println("This rule is undefined.");
			println("This means that the rule was referenced somewhere in the grammar,");
			println("but a definition for the rule was not encountered.");
			println("It is also possible that syntax errors during the parse of");
			println("your grammar file prevented correct processing of the rule.");
			println("*** End " + ruleType + " Rule: " + s.getId());
			return;
		}
		tabs++;

		if (s.access.length() != 0) {
			println("Access: " + s.access);
		}

		// Get rule return type and arguments
		RuleBlock rblk = s.getBlock();

		// Gen method return value(s)
		if (rblk.returnAction != null) {
			println("Return value(s): " + rblk.returnAction);
			if ( doingLexRules ) {
				println("Error: you specified return value(s) for a lexical rule.");
				println("\tLexical rules have an implicit return type of 'int'.");
			}
		} else {
			if ( doingLexRules ) {
				println("Return value: lexical rule returns an implicit token type");
			} else {
				println("Return value: none");
			}
		}

		// Gen arguments
		if (rblk.argAction != null) 
		{
			println("Arguments: " + rblk.argAction);
		}

		// Dump any init-action
		genBlockPreamble(rblk);

		// Analyze the rule
		boolean ok = grammar.theLLkAnalyzer.deterministic(rblk);
		if (!ok) {
			println("Error: This rule is non-deterministic");
		}
	
		// Dump the alternates of the rule
		genCommonBlock(rblk);

		// Search for an unlabeled exception specification attached to the rule
		ExceptionSpec unlabeledUserSpec = rblk.findExceptionSpec("");

		// Generate user-defined or default catch phrases
		if (unlabeledUserSpec != null) {
			println("You specified error-handler(s) for this rule:");
			tabs++;
			for (int i = 0; i < unlabeledUserSpec.handlers.size(); i++)
			{
				if (i != 0) {
					println("");
				}

				ExceptionHandler handler = (ExceptionHandler)unlabeledUserSpec.handlers.elementAt(i);
				println("Error-handler(" + (i+1) + ") catches [" + handler.exceptionTypeAndName.getText() + "] and executes:");
				printAction(handler.action.getText());
			}
			tabs--;
			println("End error-handlers.");
		}
		else if (!doingLexRules) {
			println("Default error-handling will be generated, which catches all");
			println("parser exceptions and consumes tokens until the follow-set is seen.");
		}


		// Dump the follow set
		// Doesn't seem to work for lexical rules...
		if (!doingLexRules) {
			println("The follow set for this rule is:");
			tabs++;
			genFollowSetForRuleBlock(rblk);
			tabs--;
		}

		tabs--;
		println("*** End " + ruleType + " Rule: " + s.getId());
	}
	/** Generate the syntactic predicate.  This basically generates
	 * the alternative block, buts tracks if we are inside a synPred
	 * @param blk  The syntactic predicate block
	 */
	protected void genSynPred(SynPredBlock blk) {
		syntacticPredLevel++;
		gen((AlternativeBlock)blk);
		syntacticPredLevel--;
	}
	/** Generate the token types TXT file */
	protected void genTokenTypes(TokenManager tm) throws IOException {
		// Open the token output TXT file and set the currentOutput stream
		System.out.println("Generating " + tm.getName() + TokenTypesFileSuffix+TokenTypesFileExt);
		currentOutput = antlr.Tool.openOutputFile(tm.getName() + TokenTypesFileSuffix+TokenTypesFileExt);
		//SAS: changed for proper text file io
		tabs = 0;
	
		// Generate the header common to all diagnostic files
		genHeader();

		// Generate a string for each token.  This creates a static
		// array of Strings indexed by token type.
		println("");
		println("*** Tokens used by the parser");
		println("This is a list of the token numeric values and the corresponding");
		println("token identifiers.  Some tokens are literals, and because of that");
		println("they have no identifiers.  Literals are double-quoted.");
		tabs++;

		// Enumerate all the valid token types
		Vector v = tm.getVocabulary();
		for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
			String s = (String)v.elementAt(i);
			if (s != null) {
				println(s + " = " + i);
			}
		}

		// Close the interface
		tabs--;
		println("*** End of tokens used by the parser");

		// Close the tokens output file
		currentOutput.close();
		currentOutput = null;
	}
	/** Get a string for an expression to generate creation of an AST subtree.
	  * @param v A Vector of String, where each element is an expression in the target language yielding an AST node.
	  */
	public String getASTCreateString(Vector v) {
		return "***Create an AST from a vector here***"+System.getProperty("line.separator");
	}
	/** Get a string for an expression to generate creating of an AST node
	  * @param str The arguments to the AST constructor
	  */
	public String getASTCreateString(GrammarAtom atom, String str) {
		return "[" + str + "]";
	}
	/** Map an identifier to it's corresponding tree-node variable.
	  * This is context-sensitive, depending on the rule and alternative
	  * being generated
	  * @param id The identifier name to map
	  * @param forInput true if the input tree node variable is to be returned, otherwise the output variable is returned.
	  */
	public String mapTreeId(String id, ActionTransInfo tInfo) {
		return id;
	}
	/** Format a lookahead or follow set.
	 * @param depth The depth of the entire lookahead/follow
	 * @param k The lookahead level to print
	 * @param lookahead  The lookahead/follow set to print
	 */
	public void printSet(int depth, int k, Lookahead lookahead) {
		int numCols = 5;

		int[] elems = lookahead.fset.toArray();

		if (depth != 1) {
			print("k==" + k + ": {");
		} else {
			print("{ ");
		}
		if (elems.length > numCols) {
			_println("");
			tabs++;
			print("");
		}

		int column = 0;
		for (int i = 0; i < elems.length; i++)
		{
			column++;
			if (column > numCols) {
				_println("");
				print("");
				column = 0;
			}
			if (doingLexRules) {
				_print(charFormatter.literalChar(elems[i]));
			} else {
				_print((String)grammar.tokenManager.getVocabulary().elementAt(elems[i]));
			}
			if (i != elems.length-1) {
				_print(", ");
			}
		}

		if (elems.length > numCols) {
			_println("");
			tabs--;
			print("");
		}
		_println(" }");
	}
}

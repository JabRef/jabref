package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;

class DefaultToolErrorHandler implements ToolErrorHandler {
    CharFormatter javaCharFormatter = new JavaCharFormatter();

    /** Dump token/character sets to System.out
     * @param lexicalAnalysis  true for lexical rule
     * @param depth  The depth of the ambiguity
     * @param sets  An array of bitsets containing the ambiguities
     */
    private void dumpSets(Grammar grammar,
			  boolean lexicalAnalysis,
			  int depth,
			  Lookahead[] sets,
			  String linePrefix)
    {
	for (int i = 1; i <= depth; i++) {
	    System.out.print(linePrefix+"\tk==" + i + ":");
	    if (lexicalAnalysis) {
		String bits = sets[i].fset.toStringWithRanges(",", javaCharFormatter);
		if ( sets[i].containsEpsilon() ) {
		    System.out.print("<end-of-token>");
		    if ( bits.length()>0 ) {
			System.out.print(",");
		    }	
		}
		System.out.println(bits);
	    }
	    else {
		System.out.println(sets[i].fset.toString(",", grammar.tokenManager.getVocabulary()));
	    }
	}
    }

    /** Issue a warning about ambiguity between a alternates
     * @param blk  The block being analyzed
     * @param lexicalAnalysis  true for lexical rule
     * @param depth  The depth of the ambiguity
     * @param sets  An array of bitsets containing the ambiguities
     * @param altIdx1  The zero-based index of the first ambiguous alternative
     * @param altIdx2  The zero-based index of the second ambiguous alternative
     */
    public void warnAltAmbiguity(Grammar grammar,
				 AlternativeBlock blk,
				 boolean lexicalAnalysis,
				 int depth,
				 Lookahead[] sets,
				 int altIdx1,
				 int altIdx2)
    {
	String fileline = FileLineFormatter.getFormatter().getFormatString(grammar.getFilename(),blk.getLine());
	if ( blk instanceof RuleBlock && ((RuleBlock)blk).isLexerAutoGenRule() ) {
	    System.out.print("warning: lexical nondeterminism between rules ");
	    Alternative ai = blk.getAlternativeAt(altIdx1);
	    Alternative aj = blk.getAlternativeAt(altIdx2);
	    RuleRefElement rri = (RuleRefElement)ai.head;
	    RuleRefElement rrj = (RuleRefElement)aj.head;
	    String ri = CodeGenerator.reverseLexerRuleName(rri.targetRule);
	    String rj = CodeGenerator.reverseLexerRuleName(rrj.targetRule);
	    System.out.println(ri+" and "+rj+" upon");
	    dumpSets(grammar, lexicalAnalysis, depth, sets, fileline);
	    return;
	}	
	System.out.println(
			   //   "warning: line " + blk.getLine() + ": " +
			   fileline+"warning: "+
			   (lexicalAnalysis ? "lexical " : "") + "nondeterminism upon"
			   );
	dumpSets(grammar, lexicalAnalysis, depth, sets, fileline);
	System.out.println(fileline+"\tbetween alts " + (altIdx1+1) + " and " + (altIdx2+1) + " of block");
    }

    /** Issue a warning about ambiguity between an alternate and exit path.
     * @param blk  The block being analyzed
     * @param lexicalAnalysis  true for lexical rule
     * @param depth  The depth of the ambiguity
     * @param sets  An array of bitsets containing the ambiguities
     * @param altIdx  The zero-based index of the ambiguous alternative
     */
    public void warnAltExitAmbiguity(Grammar grammar,
				     BlockWithImpliedExitPath blk,
				     boolean lexicalAnalysis,
				     int depth,
				     Lookahead[] sets,
				     int altIdx
				     )
    {
	String fileline = FileLineFormatter.getFormatter().getFormatString(grammar.getFilename(),blk.getLine());
	System.out.println(
			   // "warning: line " + blk.getLine() + ": " +
			   fileline+"warning: "+
			   (lexicalAnalysis ? "lexical " : "") + "nondeterminism upon"
			   );
	dumpSets(grammar, lexicalAnalysis, depth, sets, fileline);
	System.out.println(fileline+"\tbetween alt " + (altIdx+1) + " and exit branch of block");
    }
}

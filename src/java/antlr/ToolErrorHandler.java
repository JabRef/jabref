package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;

interface ToolErrorHandler {


    /** Issue a warning about ambiguity between a alternates
     * @param blk  The block being analyzed
     * @param lexicalAnalysis  true for lexical rule
     * @param depth  The depth of the ambiguity
     * @param sets  An array of bitsets containing the ambiguities
     * @param altIdx1  The zero-based index of the first ambiguous alternative
     * @param altIdx2  The zero-based index of the second ambiguous alternative
     */
    public void warnAltAmbiguity(
        Grammar grammar,
        AlternativeBlock blk,
        boolean lexicalAnalysis,
        int depth,
        Lookahead[] sets,
        int altIdx1,
        int altIdx2
        );

    /** Issue a warning about ambiguity between an alternate and exit path.
     * @param blk  The block being analyzed
     * @param lexicalAnalysis  true for lexical rule
     * @param depth  The depth of the ambiguity
     * @param sets  An array of bitsets containing the ambiguities
     * @param altIdx  The zero-based index of the ambiguous alternative
     */
    public void warnAltExitAmbiguity(
        Grammar grammar,
        BlockWithImpliedExitPath blk,
        boolean lexicalAnalysis,
        int depth,
        Lookahead[] sets,
        int altIdx
        );
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;

/**A linear-approximate LL(k) grammar analzyer.
 *
 * All lookahead elements are sets of token types.
 *
 * @author  Terence Parr, John Lilley
 * @see     antlr.Grammar
 * @see     antlr.Lookahead
 */
public class LLkAnalyzer implements LLkGrammarAnalyzer {
    // Set "analyzerDebug" to true
    public boolean DEBUG_ANALYZER = false;
    private AlternativeBlock currentBlock;
    protected Tool tool = null;
    protected Grammar grammar = null;
    // True if analyzing a lexical grammar
    protected boolean lexicalAnalysis = false;
    // Used for formatting bit sets in default (Java) format
    CharFormatter charFormatter = new JavaCharFormatter();

    /** Create an LLk analyzer */
    public LLkAnalyzer(Tool tool_) {
        tool = tool_;
    }

    /** Return true if someone used the '.' wildcard default idiom.
     *  Either #(. children) or '.' as an alt by itself.
     */
    protected boolean altUsesWildcardDefault(Alternative alt) {
        AlternativeElement head = alt.head;
        // if element is #(. blah) then check to see if el is root
        if (head instanceof TreeElement &&
            ((TreeElement)head).root instanceof WildcardElement) {
            return true;
        }
        if (head instanceof WildcardElement && head.next instanceof BlockEndElement) {
            return true;
        }
        return false;
    }

    /**Is this block of alternatives LL(k)?  Fill in alternative cache for this block.
     * @return true if the block is deterministic
     */
    public boolean deterministic(AlternativeBlock blk) {
        /** The lookahead depth for this decision */
        int k = 1;	// start at k=1
        if (DEBUG_ANALYZER) System.out.println("deterministic(" + blk + ")");
        boolean det = true;
        int nalts = blk.alternatives.size();
        AlternativeBlock saveCurrentBlock = currentBlock;
        Alternative wildcardAlt = null;
        currentBlock = blk;

        /* don't allow nongreedy (...) blocks */
        if (blk.greedy == false && !(blk instanceof OneOrMoreBlock) && !(blk instanceof ZeroOrMoreBlock)) {
            tool.warning("Being nongreedy only makes sense for (...)+ and (...)*", grammar.getFilename(), blk.getLine(), blk.getColumn());
        }

        // SPECIAL CASE: only one alternative.  We don't need to check the
        // determinism, but other code expects the lookahead cache to be
        // set for the single alt.
        if (nalts == 1) {
            AlternativeElement e = blk.getAlternativeAt(0).head;
            currentBlock.alti = 0;
            blk.getAlternativeAt(0).cache[1] = e.look(1);
            blk.getAlternativeAt(0).lookaheadDepth = 1;	// set lookahead to LL(1)
            currentBlock = saveCurrentBlock;
            return true;	// always deterministic for one alt
        }

        outer:
            for (int i = 0; i < nalts - 1; i++) {
                currentBlock.alti = i;
                currentBlock.analysisAlt = i;	// which alt are we analyzing?
                currentBlock.altj = i + 1;		// reset this alt.  Haven't computed yet,
                // but we need the alt number.
                inner:
                    // compare against other alternatives with lookahead depth k
                    for (int j = i + 1; j < nalts; j++) {
                        currentBlock.altj = j;
                        if (DEBUG_ANALYZER) System.out.println("comparing " + i + " against alt " + j);
                        currentBlock.analysisAlt = j;	// which alt are we analyzing?
                        k = 1;	// always attempt minimum lookahead possible.

                        // check to see if there is a lookahead depth that distinguishes
                        // between alternatives i and j.
                        Lookahead[] r = new Lookahead[grammar.maxk + 1];
                        boolean haveAmbiguity;
                        do {
                            haveAmbiguity = false;
                            if (DEBUG_ANALYZER) System.out.println("checking depth " + k + "<=" + grammar.maxk);
                            Lookahead p,q;
                            p = getAltLookahead(blk, i, k);
                            q = getAltLookahead(blk, j, k);

                            // compare LOOK(alt i) with LOOK(alt j).  Is there an intersection?
                            // Lookahead must be disjoint.
                            if (DEBUG_ANALYZER) System.out.println("p is " + p.toString(",", charFormatter, grammar));
                            if (DEBUG_ANALYZER) System.out.println("q is " + q.toString(",", charFormatter, grammar));
                            // r[i] = p.fset.and(q.fset);
                            r[k] = p.intersection(q);
                            if (DEBUG_ANALYZER) System.out.println("intersection at depth " + k + " is " + r[k].toString());
                            if (!r[k].nil()) {
                                haveAmbiguity = true;
                                k++;
                            }
                            // go until no more lookahead to use or no intersection
                        } while (haveAmbiguity && k <= grammar.maxk);

                        Alternative ai = blk.getAlternativeAt(i);
                        Alternative aj = blk.getAlternativeAt(j);
                        if (haveAmbiguity) {
                            det = false;
                            ai.lookaheadDepth = NONDETERMINISTIC;
                            aj.lookaheadDepth = NONDETERMINISTIC;

                            /* if ith alt starts with a syntactic predicate, computing the
                             * lookahead is still done for code generation, but messages
                             * should not be generated when comparing against alt j.
                             * Alternatives with syn preds that are unnecessary do
                             * not result in syn pred try-blocks.
                             */
                            if (ai.synPred != null) {
                                if (DEBUG_ANALYZER) {
                                    System.out.println("alt " + i + " has a syn pred");
                                }
                                // The alt with the (...)=> block is nondeterministic for sure.
                                // If the (...)=> conflicts with alt j, j is nondeterministic.
                                // This prevents alt j from being in any switch statements.
                                // move on to next alternative=>no possible ambiguity!
                                //						continue inner;
                            }

                            /* if ith alt starts with a semantic predicate, computing the
                             * lookahead is still done for code generation, but messages
                             * should not be generated when comparing against alt j.
                             */
                            else if (ai.semPred != null) {
                                if (DEBUG_ANALYZER) {
                                    System.out.println("alt " + i + " has a sem pred");
                                }
                            }

                            /* if jth alt is exactly the wildcard or wildcard root of tree,
                             * then remove elements from alt i lookahead from alt j's lookahead.
                             * Don't do an ambiguity warning.
                             */
                            else if (altUsesWildcardDefault(aj)) {
                                // System.out.println("removing pred sets");
                                // removeCompetingPredictionSetsFromWildcard(aj.cache, aj.head, grammar.maxk);
                                wildcardAlt = aj;
                            }

                            /* If the user specified warnWhenFollowAmbig=false, then we
                             * can turn off this warning IFF one of the alts is empty;
                             * that is, it points immediately at the end block.
                             */
                            else if (!blk.warnWhenFollowAmbig &&
                                (ai.head instanceof BlockEndElement ||
                                aj.head instanceof BlockEndElement)) {
                                // System.out.println("ai.head pts to "+ai.head.getClass());
                                // System.out.println("aj.head pts to "+aj.head.getClass());
                            }

                            /* If they have the generateAmbigWarnings option off for the block
                             * then don't generate a warning.
                             */
                            else if (!blk.generateAmbigWarnings) {
                            }

                            /* If greedy=true and *one* empty alt shut off warning. */
                            else if (blk.greedySet && blk.greedy &&
                                ((ai.head instanceof BlockEndElement &&
                                !(aj.head instanceof BlockEndElement)) ||
                                (aj.head instanceof BlockEndElement &&
                                !(ai.head instanceof BlockEndElement)))) {
                                // System.out.println("greedy set to true; one alt empty");
                            }


                            /* We have no choice, but to report a nondetermism */
                            else {
                                tool.errorHandler.warnAltAmbiguity(
                                    grammar,
                                    blk, // the block
                                    lexicalAnalysis, // true if lexical
                                    grammar.maxk, // depth of ambiguity
                                    r, // set of linear ambiguities
                                    i, // first ambiguous alternative
                                    j				// second ambiguous alternative
                                );
                            }
                        }
                        else {
                            // a lookahead depth, k, was found where i and j do not conflict
                            ai.lookaheadDepth = Math.max(ai.lookaheadDepth, k);
                            aj.lookaheadDepth = Math.max(aj.lookaheadDepth, k);
                        }
                    }
            }

        // finished with block.

        // If had wildcard default clause idiom, remove competing lookahead
        /*
		  if ( wildcardAlt!=null ) {
		  removeCompetingPredictionSetsFromWildcard(wildcardAlt.cache, wildcardAlt.head, grammar.maxk);
		  }
		*/

        currentBlock = saveCurrentBlock;
        return det;
    }

    /**Is (...)+ block LL(1)?  Fill in alternative cache for this block.
     * @return true if the block is deterministic
     */
    public boolean deterministic(OneOrMoreBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("deterministic(...)+(" + blk + ")");
        AlternativeBlock saveCurrentBlock = currentBlock;
        currentBlock = blk;
        boolean blkOk = deterministic((AlternativeBlock)blk);
        // block has been checked, now check that what follows does not conflict
        // with the lookahead of the (...)+ block.
        boolean det = deterministicImpliedPath(blk);
        currentBlock = saveCurrentBlock;
        return det && blkOk;
    }

    /**Is (...)* block LL(1)?  Fill in alternative cache for this block.
     * @return true if the block is deterministic
     */
    public boolean deterministic(ZeroOrMoreBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("deterministic(...)*(" + blk + ")");
        AlternativeBlock saveCurrentBlock = currentBlock;
        currentBlock = blk;
        boolean blkOk = deterministic((AlternativeBlock)blk);
        // block has been checked, now check that what follows does not conflict
        // with the lookahead of the (...)* block.
        boolean det = deterministicImpliedPath(blk);
        currentBlock = saveCurrentBlock;
        return det && blkOk;
    }

    /**Is this (...)* or (...)+ block LL(k)?
     * @return true if the block is deterministic
     */
    public boolean deterministicImpliedPath(BlockWithImpliedExitPath blk) {
        /** The lookahead depth for this decision considering implied exit path */
        int k;
        boolean det = true;
        Vector alts = blk.getAlternatives();
        int nalts = alts.size();
        currentBlock.altj = -1;	// comparing against implicit optional/exit alt

        if (DEBUG_ANALYZER) System.out.println("deterministicImpliedPath");
        for (int i = 0; i < nalts; i++) {		// check follow against all alts
            Alternative alt = blk.getAlternativeAt(i);

            if (alt.head instanceof BlockEndElement) {
                tool.warning("empty alternative makes no sense in (...)* or (...)+", grammar.getFilename(), blk.getLine(), blk.getColumn());
            }

            k = 1;							// assume eac alt is LL(1) with exit branch
            // check to see if there is a lookahead depth that distinguishes
            // between alternative i and the exit branch.
            Lookahead[] r = new Lookahead[grammar.maxk + 1];
            boolean haveAmbiguity;
            do {
                haveAmbiguity = false;
                if (DEBUG_ANALYZER) System.out.println("checking depth " + k + "<=" + grammar.maxk);
                Lookahead p;
                Lookahead follow = blk.next.look(k);
                blk.exitCache[k] = follow;
                currentBlock.alti = i;
                p = getAltLookahead(blk, i, k);

                if (DEBUG_ANALYZER) System.out.println("follow is " + follow.toString(",", charFormatter, grammar));
                if (DEBUG_ANALYZER) System.out.println("p is " + p.toString(",", charFormatter, grammar));
                //r[k] = follow.fset.and(p.fset);
                r[k] = follow.intersection(p);
                if (DEBUG_ANALYZER) System.out.println("intersection at depth " + k + " is " + r[k]);
                if (!r[k].nil()) {
                    haveAmbiguity = true;
                    k++;
                }
                // go until no more lookahead to use or no intersection
            } while (haveAmbiguity && k <= grammar.maxk);

            if (haveAmbiguity) {
                det = false;
                alt.lookaheadDepth = NONDETERMINISTIC;
                blk.exitLookaheadDepth = NONDETERMINISTIC;
                Alternative ambigAlt = blk.getAlternativeAt(currentBlock.alti);

                /* If the user specified warnWhenFollowAmbig=false, then we
                 * can turn off this warning.
                 */
                if (!blk.warnWhenFollowAmbig) {
                }

                /* If they have the generateAmbigWarnings option off for the block
                 * then don't generate a warning.
                 */
                else if (!blk.generateAmbigWarnings) {
                }

                /* If greedy=true and alt not empty, shut off warning */
                else if (blk.greedy == true && blk.greedySet &&
                    !(ambigAlt.head instanceof BlockEndElement)) {
                    if (DEBUG_ANALYZER) System.out.println("greedy loop");
                }

                /* If greedy=false then shut off warning...will have
                 * to add "if FOLLOW break"
                 * block during code gen to compensate for removal of warning.
                 */
                else if (blk.greedy == false &&
                    !(ambigAlt.head instanceof BlockEndElement)) {
                    if (DEBUG_ANALYZER) System.out.println("nongreedy loop");
                    // if FOLLOW not single k-string (|set[k]| can
                    // be > 1 actually) then must warn them that
                    // loop may terminate incorrectly.
                    // For example, ('a'..'d')+ ("ad"|"cb")
                    if (!lookaheadEquivForApproxAndFullAnalysis(blk.exitCache, grammar.maxk)) {
                        tool.warning(new String[]{
                            "nongreedy block may exit incorrectly due",
                            "\tto limitations of linear approximate lookahead (first k-1 sets",
                            "\tin lookahead not singleton)."},
                                     grammar.getFilename(), blk.getLine(), blk.getColumn());
                    }
                }

                // no choice but to generate a warning
                else {
                    tool.errorHandler.warnAltExitAmbiguity(
                        grammar,
                        blk, // the block
                        lexicalAnalysis, // true if lexical
                        grammar.maxk, // depth of ambiguity
                        r, // set of linear ambiguities
                        i		// ambiguous alternative
                    );
                }
            }
            else {
                alt.lookaheadDepth = Math.max(alt.lookaheadDepth, k);
                blk.exitLookaheadDepth = Math.max(blk.exitLookaheadDepth, k);
            }
        }
        return det;
    }

    /**Compute the lookahead set of whatever follows references to
     * the rule associated witht the FOLLOW block.
     */
    public Lookahead FOLLOW(int k, RuleEndElement end) {
        // what rule are we trying to compute FOLLOW of?
        RuleBlock rb = (RuleBlock)end.block;
        // rule name is different in lexer
        String rule;
        if (lexicalAnalysis) {
            rule = CodeGenerator.encodeLexerRuleName(rb.getRuleName());
        }
        else {
            rule = rb.getRuleName();
        }

        if (DEBUG_ANALYZER) System.out.println("FOLLOW(" + k + "," + rule + ")");

        // are we in the midst of computing this FOLLOW already?
        if (end.lock[k]) {
            if (DEBUG_ANALYZER) System.out.println("FOLLOW cycle to " + rule);
            return new Lookahead(rule);
        }

        // Check to see if there is cached value
        if (end.cache[k] != null) {
            if (DEBUG_ANALYZER) {
                System.out.println("cache entry FOLLOW(" + k + ") for " + rule + ": " + end.cache[k].toString(",", charFormatter, grammar));
            }
            // if the cache is a complete computation then simply return entry
            if (end.cache[k].cycle == null) {
                return (Lookahead)end.cache[k].clone();
            }
            // A cache entry exists, but it is a reference to a cyclic computation.
            RuleSymbol rs = (RuleSymbol)grammar.getSymbol(end.cache[k].cycle);
            RuleEndElement re = rs.getBlock().endNode;
            // The other entry may not exist because it is still being
            // computed when this cycle cache entry was found here.
            if (re.cache[k] == null) {
                // return the cycle...that's all we can do at the moment.
                return (Lookahead)end.cache[k].clone();
            }
            else {
                if (DEBUG_ANALYZER) {
                    System.out.println("combining FOLLOW(" + k + ") for " + rule + ": from "+end.cache[k].toString(",", charFormatter, grammar) + " with FOLLOW for "+((RuleBlock)re.block).getRuleName()+": "+re.cache[k].toString(",", charFormatter, grammar));
                }
                // combine results from other rule's FOLLOW
                if ( re.cache[k].cycle==null ) {
                    // current rule depends on another rule's FOLLOW and
                    // it is complete with no cycle; just kill our cycle and
                    // combine full result from other rule's FOLLOW
                    end.cache[k].combineWith(re.cache[k]);
                    end.cache[k].cycle = null; // kill cycle as we're complete
                }
                else {
                    // the FOLLOW cache for other rule has a cycle also.
                    // Here is where we bubble up a cycle.  We better recursively
                    // wipe out cycles (partial computations).  I'm a little nervous
                    // that we might leave a cycle here, however.
                    Lookahead refFOLLOW = FOLLOW(k, re);
                    end.cache[k].combineWith( refFOLLOW );
                    // all cycles should be gone, but if not, record ref to cycle
                    end.cache[k].cycle = refFOLLOW.cycle;
                }
                if (DEBUG_ANALYZER) {
                    System.out.println("saving FOLLOW(" + k + ") for " + rule + ": from "+end.cache[k].toString(",", charFormatter, grammar));
                }
                // Return the updated cache entry associated
                // with the cycle reference.
                return (Lookahead)end.cache[k].clone();
            }
        }

        end.lock[k] = true;	// prevent FOLLOW computation cycles

        Lookahead p = new Lookahead();

        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rule);

        // Walk list of references to this rule to compute FOLLOW
        for (int i = 0; i < rs.numReferences(); i++) {
            RuleRefElement rr = rs.getReference(i);
            if (DEBUG_ANALYZER) System.out.println("next[" + rule + "] is " + rr.next.toString());
            Lookahead q = rr.next.look(k);
            if (DEBUG_ANALYZER) System.out.println("FIRST of next[" + rule + "] ptr is " + q.toString());
            /* If there is a cycle then if the cycle is to the rule for
			 * this end block, you have a cycle to yourself.  Remove the
			 * cycle indication--the lookahead is complete.
			 */
            if (q.cycle != null && q.cycle.equals(rule)) {
                q.cycle = null;	// don't want cycle to yourself!
            }
            // add the lookahead into the current FOLLOW computation set
            p.combineWith(q);
            if (DEBUG_ANALYZER) System.out.println("combined FOLLOW[" + rule + "] is " + p.toString());
        }

        end.lock[k] = false; // we're not doing FOLLOW anymore

        // if no rules follow this, it can be a start symbol or called by a start sym.
        // set the follow to be end of file.
        if (p.fset.nil() && p.cycle == null) {
            if (grammar instanceof TreeWalkerGrammar) {
                // Tree grammars don't see EOF, they see end of sibling list or
                // "NULL TREE LOOKAHEAD".
                p.fset.add(Token.NULL_TREE_LOOKAHEAD);
            }
            else if (grammar instanceof LexerGrammar) {
                // Lexical grammars use Epsilon to indicate that the end of rule has been hit
                // EOF would be misleading; any character can follow a token rule not just EOF
                // as in a grammar (where a start symbol is followed by EOF).  There is no
                // sequence info in a lexer between tokens to indicate what is the last token
                // to be seen.
                // p.fset.add(EPSILON_TYPE);
                p.setEpsilon();
            }
            else {
                p.fset.add(Token.EOF_TYPE);
            }
        }

        // Cache the result of the FOLLOW computation
        if (DEBUG_ANALYZER) {
            System.out.println("saving FOLLOW(" + k + ") for " + rule + ": " + p.toString(",", charFormatter, grammar));
        }
        end.cache[k] = (Lookahead)p.clone();

        return p;
    }

    private Lookahead getAltLookahead(AlternativeBlock blk, int alt, int k) {
        Lookahead p;
        Alternative a = blk.getAlternativeAt(alt);
        AlternativeElement e = a.head;
        //System.out.println("getAltLookahead("+k+","+e+"), cache size is "+a.cache.length);
        if (a.cache[k] == null) {
            p = e.look(k);
            a.cache[k] = p;
        }
        else {
            p = a.cache[k];
        }
        return p;
    }

    /**Actions are ignored */
    public Lookahead look(int k, ActionElement action) {
        if (DEBUG_ANALYZER) System.out.println("lookAction(" + k + "," + action + ")");
        return action.next.look(k);
    }

    /**Combine the lookahead computed for each alternative */
    public Lookahead look(int k, AlternativeBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("lookAltBlk(" + k + "," + blk + ")");
        AlternativeBlock saveCurrentBlock = currentBlock;
        currentBlock = blk;
        Lookahead p = new Lookahead();
        for (int i = 0; i < blk.alternatives.size(); i++) {
            if (DEBUG_ANALYZER) System.out.println("alt " + i + " of " + blk);
            // must set analysis alt
            currentBlock.analysisAlt = i;
            Alternative alt = blk.getAlternativeAt(i);
            AlternativeElement elem = alt.head;
            if (DEBUG_ANALYZER) {
                if (alt.head == alt.tail) {
                    System.out.println("alt " + i + " is empty");
                }
            }
            Lookahead q = elem.look(k);
            p.combineWith(q);
        }
        if (k == 1 && blk.not && subruleCanBeInverted(blk, lexicalAnalysis)) {
            // Invert the lookahead set
            if (lexicalAnalysis) {
                BitSet b = (BitSet)((LexerGrammar)grammar).charVocabulary.clone();
                int[] elems = p.fset.toArray();
                for (int j = 0; j < elems.length; j++) {
                    b.remove(elems[j]);
                }
                p.fset = b;
            }
            else {
                p.fset.notInPlace(Token.MIN_USER_TYPE, grammar.tokenManager.maxTokenType());
            }
        }
        currentBlock = saveCurrentBlock;
        return p;
    }

    /**Compute what follows this place-holder node and possibly
     * what begins the associated loop unless the
     * node is locked.
     * <p>
     * if we hit the end of a loop, we have to include
     * what tokens can begin the loop as well.  If the start
     * node is locked, then we simply found an empty path
     * through this subrule while analyzing it.  If the
     * start node is not locked, then this node was hit
     * during a FOLLOW operation and the FIRST of this
     * block must be included in that lookahead computation.
     */
    public Lookahead look(int k, BlockEndElement end) {
        if (DEBUG_ANALYZER) System.out.println("lookBlockEnd(" + k + ", " + end.block + "); lock is " + end.lock[k]);
        if (end.lock[k]) {
            // computation in progress => the tokens we would have
            // computed (had we not been locked) will be included
            // in the set by that computation with the lock on this
            // node.
            return new Lookahead();
        }

        Lookahead p;

        /* Hitting the end of a loop means you can see what begins the loop */
        if (end.block instanceof ZeroOrMoreBlock ||
            end.block instanceof OneOrMoreBlock) {
            // compute what can start the block,
            // but lock end node so we don't do it twice in same
            // computation.
            end.lock[k] = true;
            p = look(k, end.block);
            end.lock[k] = false;
        }
        else {
            p = new Lookahead();
        }

        /* Tree blocks do not have any follow because they are children
		 * of what surrounds them.  For example, A #(B C) D results in
		 * a look() for the TreeElement end of NULL_TREE_LOOKAHEAD, which
		 * indicates that nothing can follow the last node of tree #(B C)
		 */
        if (end.block instanceof TreeElement) {
            p.combineWith(Lookahead.of(Token.NULL_TREE_LOOKAHEAD));
        }

        /* Syntactic predicates such as ( (A)? )=> have no follow per se.
		 * We cannot accurately say what would be matched following a
		 * syntactic predicate (you MIGHT be ok if you said it was whatever
		 * followed the alternative predicted by the predicate).  Hence,
		 * (like end-of-token) we return Epsilon to indicate "unknown
		 * lookahead."
		 */
        else if (end.block instanceof SynPredBlock) {
            p.setEpsilon();
        }

        // compute what can follow the block
        else {
            Lookahead q = end.block.next.look(k);
            p.combineWith(q);
        }

        return p;
    }

    /**Return this char as the lookahead if k=1.
     * <p>### Doesn't work for ( 'a' 'b' | 'a' ~'b' ) yet!!!
     * <p>
     * If the atom has the <tt>not</tt> flag on, then
     * create the set complement of the tokenType
     * which is the set of all characters referenced
     * in the grammar with this char turned off.
     * Also remove characters from the set that
     * are currently allocated for predicting
     * previous alternatives.  This avoids ambiguity
     * messages and is more properly what is meant.
     * ( 'a' | ~'a' ) implies that the ~'a' is the
     * "else" clause.
     * <p>
     * NOTE: we do <b>NOT</b> include exit path in
     * the exclusion set. E.g.,
     * ( 'a' | ~'a' )* 'b'
     * should exit upon seeing a 'b' during the loop.
     */
    public Lookahead look(int k, CharLiteralElement atom) {
        if (DEBUG_ANALYZER) System.out.println("lookCharLiteral(" + k + "," + atom + ")");
        // Skip until analysis hits k==1
        if (k > 1) {
            return atom.next.look(k - 1);
        }
        if (lexicalAnalysis) {
            if (atom.not) {
                BitSet b = (BitSet)((LexerGrammar)grammar).charVocabulary.clone();
                if (DEBUG_ANALYZER) System.out.println("charVocab is " + b.toString());
                // remove stuff predicted by preceding alts and follow of block
                removeCompetingPredictionSets(b, atom);
                if (DEBUG_ANALYZER) System.out.println("charVocab after removal of prior alt lookahead " + b.toString());
                // now remove element that is stated not to be in the set
                b.clear(atom.getType());
                return new Lookahead(b);
            }
            else {
                return Lookahead.of(atom.getType());
            }
        }
        else {
            // Should have been avoided by MakeGrammar
            tool.panic("Character literal reference found in parser");
            // ... so we make the compiler happy
            return Lookahead.of(atom.getType());
        }
    }

    public Lookahead look(int k, CharRangeElement r) {
        if (DEBUG_ANALYZER) System.out.println("lookCharRange(" + k + "," + r + ")");
        // Skip until analysis hits k==1
        if (k > 1) {
            return r.next.look(k - 1);
        }
        BitSet p = BitSet.of(r.begin);
        for (int i = r.begin + 1; i <= r.end; i++) {
            p.add(i);
        }
        return new Lookahead(p);
    }

    public Lookahead look(int k, GrammarAtom atom) {
        if (DEBUG_ANALYZER) System.out.println("look(" + k + "," + atom + "[" + atom.getType() + "])");

        if (lexicalAnalysis) {
            // MakeGrammar should have created a rule reference instead
            tool.panic("token reference found in lexer");
        }
        // Skip until analysis hits k==1
        if (k > 1) {
            return atom.next.look(k - 1);
        }
        Lookahead l = Lookahead.of(atom.getType());
        if (atom.not) {
            // Invert the lookahead set against the token vocabulary
            int maxToken = grammar.tokenManager.maxTokenType();
            l.fset.notInPlace(Token.MIN_USER_TYPE, maxToken);
            // remove stuff predicted by preceding alts and follow of block
            removeCompetingPredictionSets(l.fset, atom);
        }
        return l;
    }

    /**The lookahead of a (...)+ block is the combined lookahead of
     * all alternatives and, if an empty path is found, the lookahead
     * of what follows the block.
     */
    public Lookahead look(int k, OneOrMoreBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("look+" + k + "," + blk + ")");
        Lookahead p = look(k, (AlternativeBlock)blk);
        return p;
    }

    /**Combine the lookahead computed for each alternative.
     * Lock the node so that no other computation may come back
     * on itself--infinite loop.  This also implies infinite left-recursion
     * in the grammar (or an error in this algorithm ;)).
     */
    public Lookahead look(int k, RuleBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("lookRuleBlk(" + k + "," + blk + ")");
        Lookahead p = look(k, (AlternativeBlock)blk);
        return p;
    }

    /**If not locked or noFOLLOW set, compute FOLLOW of a rule.
     * <p>
     * TJP says 8/12/99: not true anymore:
     * Lexical rules never compute follow.  They set epsilon and
     * the code generator gens code to check for any character.
     * The code generator must remove the tokens used to predict
     * any previous alts in the same block.
     * <p>
     * When the last node of a rule is reached and noFOLLOW,
     * it implies that a "local" FOLLOW will be computed
     * after this call.  I.e.,
     * <pre>
     *		a : b A;
     *		b : B | ;
     *		c : b C;
     * </pre>
     * Here, when computing the look of rule b from rule a,
     * we want only {B,EPSILON_TYPE} so that look(b A) will
     * be {B,A} not {B,A,C}.
     * <p>
     * if the end block is not locked and the FOLLOW is
     * wanted, the algorithm must compute the lookahead
     * of what follows references to this rule.  If
     * end block is locked, FOLLOW will return an empty set
     * with a cycle to the rule associated with this end block.
     */
    public Lookahead look(int k, RuleEndElement end) {
        if (DEBUG_ANALYZER)
            System.out.println("lookRuleBlockEnd(" + k + "); noFOLLOW=" +
                               end.noFOLLOW + "; lock is " + end.lock[k]);
        if (/*lexicalAnalysis ||*/ end.noFOLLOW) {
            Lookahead p = new Lookahead();
            p.setEpsilon();
            p.epsilonDepth = BitSet.of(k);
            return p;
        }
        Lookahead p = FOLLOW(k, end);
        return p;
    }

    /**Compute the lookahead contributed by a rule reference.
     *
     * <p>
     * When computing ruleref lookahead, we don't want the FOLLOW
     * computation done if an empty path exists for the rule.
     * The FOLLOW is too loose of a set...we want only to
     * include the "local" FOLLOW or what can follow this
     * particular ref to the node.  In other words, we use
     * context information to reduce the complexity of the
     * analysis and strengthen the parser.
     *
     * The noFOLLOW flag is used as a means of restricting
     * the FOLLOW to a "local" FOLLOW.  This variable is
     * orthogonal to the <tt>lock</tt> variable that prevents
     * infinite recursion.  noFOLLOW does not care about what k is.
     */
    public Lookahead look(int k, RuleRefElement rr) {
        if (DEBUG_ANALYZER) System.out.println("lookRuleRef(" + k + "," + rr + ")");
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
        if (rs == null || !rs.defined) {
            tool.error("no definition of rule " + rr.targetRule, grammar.getFilename(), rr.getLine(), rr.getColumn());
            return new Lookahead();
        }
        RuleBlock rb = rs.getBlock();
        RuleEndElement end = rb.endNode;
        boolean saveEnd = end.noFOLLOW;
        end.noFOLLOW = true;
        // go off to the rule and get the lookahead (w/o FOLLOW)
        Lookahead p = look(k, rr.targetRule);
        if (DEBUG_ANALYZER) System.out.println("back from rule ref to " + rr.targetRule);
        // restore state of end block
        end.noFOLLOW = saveEnd;

        // check for infinite recursion.  If a cycle is returned: trouble!
        if (p.cycle != null) {
            tool.error("infinite recursion to rule " + p.cycle + " from rule " +
                       rr.enclosingRuleName, grammar.getFilename(), rr.getLine(), rr.getColumn());
        }

        // is the local FOLLOW required?
        if (p.containsEpsilon()) {
            if (DEBUG_ANALYZER)
                System.out.println("rule ref to " +
                                   rr.targetRule + " has eps, depth: " + p.epsilonDepth);

            // remove epsilon
            p.resetEpsilon();
            // fset.clear(EPSILON_TYPE);

            // for each lookahead depth that saw epsilon
            int[] depths = p.epsilonDepth.toArray();
            p.epsilonDepth = null;		// clear all epsilon stuff
            for (int i = 0; i < depths.length; i++) {
                int rk = k - (k - depths[i]);
                Lookahead q = rr.next.look(rk);	// see comments in Lookahead
                p.combineWith(q);
            }
            // note: any of these look() computations for local follow can
            // set EPSILON in the set again if the end of this rule is found.
        }

        return p;
    }

    public Lookahead look(int k, StringLiteralElement atom) {
        if (DEBUG_ANALYZER) System.out.println("lookStringLiteral(" + k + "," + atom + ")");
        if (lexicalAnalysis) {
            // need more lookahead than string can provide?
            if (k > atom.processedAtomText.length()) {
                return atom.next.look(k - atom.processedAtomText.length());
            }
            else {
                // get char at lookahead depth k, from the processed literal text
                return Lookahead.of(atom.processedAtomText.charAt(k - 1));
            }
        }
        else {
            // Skip until analysis hits k==1
            if (k > 1) {
                return atom.next.look(k - 1);
            }
            Lookahead l = Lookahead.of(atom.getType());
            if (atom.not) {
                // Invert the lookahead set against the token vocabulary
                int maxToken = grammar.tokenManager.maxTokenType();
                l.fset.notInPlace(Token.MIN_USER_TYPE, maxToken);
            }
            return l;
        }
    }

    /**The lookahead of a (...)=> block is the lookahead of
     * what follows the block.  By definition, the syntactic
     * predicate block defies static analysis (you want to try it
     * out at run-time).  The LOOK of (a)=>A B is A for LL(1)
     * ### is this even called?
     */
    public Lookahead look(int k, SynPredBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("look=>(" + k + "," + blk + ")");
        return blk.next.look(k);
    }

    public Lookahead look(int k, TokenRangeElement r) {
        if (DEBUG_ANALYZER) System.out.println("lookTokenRange(" + k + "," + r + ")");
        // Skip until analysis hits k==1
        if (k > 1) {
            return r.next.look(k - 1);
        }
        BitSet p = BitSet.of(r.begin);
        for (int i = r.begin + 1; i <= r.end; i++) {
            p.add(i);
        }
        return new Lookahead(p);
    }

    public Lookahead look(int k, TreeElement t) {
        if (DEBUG_ANALYZER)
            System.out.println("look(" + k + "," + t.root + "[" + t.root.getType() + "])");
        if (k > 1) {
            return t.next.look(k - 1);
        }
        Lookahead l = null;
        if (t.root instanceof WildcardElement) {
            l = t.root.look(1); // compute FIRST set minus previous rows
        }
        else {
            l = Lookahead.of(t.root.getType());
            if (t.root.not) {
                // Invert the lookahead set against the token vocabulary
                int maxToken = grammar.tokenManager.maxTokenType();
                l.fset.notInPlace(Token.MIN_USER_TYPE, maxToken);
            }
        }
        return l;
    }

    public Lookahead look(int k, WildcardElement wc) {
        if (DEBUG_ANALYZER) System.out.println("look(" + k + "," + wc + ")");

        // Skip until analysis hits k==1
        if (k > 1) {
            return wc.next.look(k - 1);
        }

        BitSet b;
        if (lexicalAnalysis) {
            // Copy the character vocabulary
            b = (BitSet)((LexerGrammar)grammar).charVocabulary.clone();
        }
        else {
            b = new BitSet(1);
            // Invert the lookahead set against the token vocabulary
            int maxToken = grammar.tokenManager.maxTokenType();
            b.notInPlace(Token.MIN_USER_TYPE, maxToken);
            if (DEBUG_ANALYZER) System.out.println("look(" + k + "," + wc + ") after not: " + b);
        }

        // Remove prediction sets from competing alternatives
        // removeCompetingPredictionSets(b, wc);

        return new Lookahead(b);
    }

    /** The (...)* element is the combined lookahead of the alternatives and what can
     *  follow the loop.
     */
    public Lookahead look(int k, ZeroOrMoreBlock blk) {
        if (DEBUG_ANALYZER) System.out.println("look*(" + k + "," + blk + ")");
        Lookahead p = look(k, (AlternativeBlock)blk);
        Lookahead q = blk.next.look(k);
        p.combineWith(q);
        return p;
    }

    /**Compute the combined lookahead for all productions of a rule.
     * If the lookahead returns with epsilon, at least one epsilon
     * path exists (one that consumes no tokens).  The noFOLLOW
     * flag being set for this endruleblk, indicates that the
     * a rule ref invoked this rule.
     *
     * Currently only look(RuleRef) calls this.  There is no need
     * for the code generator to call this.
     */
    public Lookahead look(int k, String rule) {
        if (DEBUG_ANALYZER) System.out.println("lookRuleName(" + k + "," + rule + ")");
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rule);
        RuleBlock rb = rs.getBlock();

        if (rb.lock[k]) {
            if (DEBUG_ANALYZER)
                System.out.println("infinite recursion to rule " + rb.getRuleName());
            return new Lookahead(rule);
        }

        // have we computed it before?
        if (rb.cache[k] != null) {
            if (DEBUG_ANALYZER) {
                System.out.println("found depth " + k + " result in FIRST " + rule + " cache: " +
                                   rb.cache[k].toString(",", charFormatter, grammar));
            }
            return (Lookahead)rb.cache[k].clone();
        }

        rb.lock[k] = true;
        Lookahead p = look(k, (RuleBlock)rb);
        rb.lock[k] = false;

        // cache results
        rb.cache[k] = (Lookahead)p.clone();
        if (DEBUG_ANALYZER) {
            System.out.println("saving depth " + k + " result in FIRST " + rule + " cache: " +
                               rb.cache[k].toString(",", charFormatter, grammar));
        }
        return p;
    }

    /** If the first k-1 sets are singleton sets, the appoximate
     *  lookahead analysis is equivalent to full lookahead analysis.
     */
    public static boolean lookaheadEquivForApproxAndFullAnalysis(Lookahead[] bset, int k) {
        // first k-1 sets degree 1?
        for (int i = 1; i <= k - 1; i++) {
            BitSet look = bset[i].fset;
            if (look.degree() > 1) {
                return false;
            }
        }
        return true;
    }

    /** Remove the prediction sets from preceding alternatives
     * and follow set, but *only* if this element is the first element
     * of the alternative.  The class members currenBlock and
     * currentBlock.analysisAlt must be set correctly.
     * @param b The prediction bitset to be modified
     * @el The element of interest
     */
    private void removeCompetingPredictionSets(BitSet b, AlternativeElement el) {
        // Only do this if the element is the first element of the alt,
        // because we are making an implicit assumption that k==1.
        GrammarElement head = currentBlock.getAlternativeAt(currentBlock.analysisAlt).head;
        // if element is #(. blah) then check to see if el is root
        if (head instanceof TreeElement) {
            if (((TreeElement)head).root != el) {
                return;
            }
        }
        else if (el != head) {
            return;
        }
        for (int i = 0; i < currentBlock.analysisAlt; i++) {
            AlternativeElement e = currentBlock.getAlternativeAt(i).head;
            b.subtractInPlace(e.look(1).fset);
        }
    }

    /** Remove the prediction sets from preceding alternatives
     * The class members currenBlock must be set correctly.
     * Remove prediction sets from 1..k.
     * @param look The prediction lookahead to be modified
     * @el The element of interest
     * @k  How deep into lookahead to modify
     */
    private void removeCompetingPredictionSetsFromWildcard(Lookahead[] look, AlternativeElement el, int k) {
        for (int d = 1; d <= k; d++) {
            for (int i = 0; i < currentBlock.analysisAlt; i++) {
                AlternativeElement e = currentBlock.getAlternativeAt(i).head;
                look[d].fset.subtractInPlace(e.look(d).fset);
            }
        }
    }

    /** reset the analyzer so it looks like a new one */
    private void reset() {
        grammar = null;
        DEBUG_ANALYZER = false;
        currentBlock = null;
        lexicalAnalysis = false;
    }

    /** Set the grammar for the analyzer */
    public void setGrammar(Grammar g) {
        if (grammar != null) {
            reset();
        }
        grammar = g;

        // Is this lexical?
        lexicalAnalysis = (grammar instanceof LexerGrammar);
        DEBUG_ANALYZER = grammar.analyzerDebug;
    }

    public boolean subruleCanBeInverted(AlternativeBlock blk, boolean forLexer) {
        if (
            blk instanceof ZeroOrMoreBlock ||
            blk instanceof OneOrMoreBlock ||
            blk instanceof SynPredBlock
        ) {
            return false;
        }
        // Cannot invert an empty subrule
        if (blk.alternatives.size() == 0) {
            return false;
        }
        // The block must only contain alternatives with a single element,
        // where each element is a char, token, char range, or token range.
        for (int i = 0; i < blk.alternatives.size(); i++) {
            Alternative alt = blk.getAlternativeAt(i);
            // Cannot have anything interesting in the alternative ...
            if (alt.synPred != null || alt.semPred != null || alt.exceptionSpec != null) {
                return false;
            }
            // ... and there must be one simple element
            AlternativeElement elt = alt.head;
            if (
                !(
                elt instanceof CharLiteralElement ||
                elt instanceof TokenRefElement ||
                elt instanceof CharRangeElement ||
                elt instanceof TokenRangeElement ||
                (elt instanceof StringLiteralElement && !forLexer)
                ) ||
                !(elt.next instanceof BlockEndElement) ||
                elt.getAutoGenType() != GrammarElement.AUTO_GEN_NONE
            ) {
                return false;
            }
        }
        return true;
    }
}

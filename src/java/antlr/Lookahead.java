package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;
import antlr.collections.impl.Vector;

/**This object holds all information needed to represent
 * the lookahead for any particular lookahead computation
 * for a <b>single</b> lookahead depth.  Final lookahead
 * information is a simple bit set, but intermediate
 * stages need computation cycle and FOLLOW information.
 *
 * <p>
 * Concerning the <tt>cycle</tt> variable.
 * If lookahead is computed for a RuleEnd node, then
 * computation is part of a FOLLOW cycle for this rule.
 * If lookahead is computed for a RuleBlock node, the
 * computation is part of a FIRST cycle to this rule.
 *
 * <p>
 * Concerning the <tt>epsilonDepth</tt> variable.
 * This is not the depth relative to the rule reference
 * that epsilon was encountered.  That value is
 * <pre>
 * 		initial_k - epsilonDepth + 1
 * </pre>
 * Also, lookahead depths past rule ref for local follow are:
 * <pre>
 * 		initial_k - (initial_k - epsilonDepth)
 * </pre>
 * Used for rule references.  If we try
 * to compute look(k, ruleref) and there are fewer
 * than k lookahead terminals before the end of the
 * the rule, epsilon will be returned (don't want to
 * pass the end of the rule).  We must track when the
 * the lookahead got stuck.  For example,
 * <pre>
 * 		a : b A B E F G;
 * 		b : C ;
 * </pre>
 * LOOK(5, ref-to(b)) is {<EPSILON>} with depth = 4, which
 * indicates that at 2 (5-4+1) tokens ahead, end of rule was reached.
 * Therefore, the token at 4=5-(5-4) past rule ref b must be
 * included in the set == F.
 * The situation is complicated by the fact that a computation
 * may hit the end of a rule at many different depths.  For example,
 * <pre>
 * 		a : b A B C ;
 * 		b : E F		// epsilon depth of 1 relative to initial k=3
 * 		  | G		// epsilon depth of 2
 * 		  ;
 * </pre>
 * Here, LOOK(3,ref-to(b)) returns epsilon, but the depths are
 * {1, 2}; i.e., 3-(3-1) and 3-(3-2).  Those are the lookahead depths
 * past the rule ref needed for the local follow.
 *
 * <p>
 * This is null unless an epsilon is created.
 *
 * @see antlr.Lookahead#combineWith(Lookahead)
 */
public class Lookahead implements Cloneable {
    /** actual bitset of the lookahead */
    BitSet fset;
    /** is this computation part of a computation cycle? */
    String cycle;
    /** What k values were being computed when end of rule hit? */
    BitSet epsilonDepth;
    /** Does this lookahead depth include Epsilon token type? This
     *  is used to avoid having a bit in the set for Epsilon as it
     *  conflicts with parsing binary files.
     */
    boolean hasEpsilon = false;

    public Lookahead() {
        fset = new BitSet();
    }

    /** create a new lookahead set with the LL(1) set to the parameter */
    public Lookahead(BitSet p) {
        fset = p;
    }

    /** create an empty lookahead set, but with cycle */
    public Lookahead(String c) {
        this();
        cycle = c;
    }

    /** Make a deep copy of everything in this object */
    public Object clone() {
        Lookahead p = null;
        try {
            p = (Lookahead)super.clone();
            p.fset = (BitSet)fset.clone();
            p.cycle = cycle; // strings are immutable
            if (epsilonDepth != null) {
                p.epsilonDepth = (BitSet)epsilonDepth.clone();
            }
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return p;
    }

    public void combineWith(Lookahead q) {
        if (cycle == null) {	// track at least one cycle
            cycle = q.cycle;
        }

        if (q.containsEpsilon()) {
            hasEpsilon = true;
        }

        // combine epsilon depths
        if (epsilonDepth != null) {
            if (q.epsilonDepth != null) {
                epsilonDepth.orInPlace(q.epsilonDepth);
            }
        }
        else if (q.epsilonDepth != null) {
            epsilonDepth = (BitSet)q.epsilonDepth.clone();
        }
        fset.orInPlace(q.fset);
    }

    public boolean containsEpsilon() {
        return hasEpsilon;
    }

    /** What is the intersection of two lookahead depths?
     *  Only the Epsilon "bit" and bitset are considered.
     */
    public Lookahead intersection(Lookahead q) {
        Lookahead p = new Lookahead(fset.and(q.fset));
        if (this.hasEpsilon && q.hasEpsilon) {
            p.setEpsilon();
        }
        return p;
    }

    public boolean nil() {
        return fset.nil() && !hasEpsilon;
    }

    public static Lookahead of(int el) {
        Lookahead look = new Lookahead();
        look.fset.add(el);
        return look;
    }

    public void resetEpsilon() {
        hasEpsilon = false;
    }

    public void setEpsilon() {
        hasEpsilon = true;
    }

    public String toString() {
        String e = "",b,f = "",d = "";
        b = fset.toString(",");
        if (containsEpsilon()) {
            e = "+<epsilon>";
        }
        if (cycle != null) {
            f = "; FOLLOW(" + cycle + ")";
        }
        if (epsilonDepth != null) {
            d = "; depths=" + epsilonDepth.toString(",");
        }
        return b + e + f + d;

    }

    public String toString(String separator, CharFormatter formatter) {
        String e = "",b,f = "",d = "";
        b = fset.toString(separator, formatter);
        if (containsEpsilon()) {
            e = "+<epsilon>";
        }
        if (cycle != null) {
            f = "; FOLLOW(" + cycle + ")";
        }
        if (epsilonDepth != null) {
            d = "; depths=" + epsilonDepth.toString(",");
        }
        return b + e + f + d;
    }

    public String toString(String separator, CharFormatter formatter, Grammar g) {
        if (g instanceof LexerGrammar) {
            return toString(separator, formatter);
        }
        else {
            return toString(separator, g.tokenManager.getVocabulary());
        }
    }

    public String toString(String separator, Vector vocab) {
        String b,f = "",d = "";
        b = fset.toString(separator, vocab);
        if (cycle != null) {
            f = "; FOLLOW(" + cycle + ")";
        }
        if (epsilonDepth != null) {
            d = "; depths=" + epsilonDepth.toString(",");
        }
        return b + f + d;
    }
}

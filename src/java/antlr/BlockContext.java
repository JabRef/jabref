package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**BlockContext stores the information needed when creating an
 * alternative (list of elements).  Entering a subrule requires
 * that we save this state as each block of alternatives
 * requires state such as "tail of current alternative."
 */
class BlockContext {
    AlternativeBlock block; // current block of alternatives
    int altNum;				// which alt are we accepting 0..n-1
    BlockEndElement blockEnd; // used if nested


    public void addAlternativeElement(AlternativeElement e) {
        currentAlt().addElement(e);
    }

    public Alternative currentAlt() {
        return (Alternative)block.alternatives.elementAt(altNum);
    }

    public AlternativeElement currentElement() {
        return currentAlt().tail;
    }
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**The context needed to add root,child elements to a Tree.  There
 * is only one alternative (i.e., a list of children).  We subclass to
 * specialize. MakeGrammar.addElementToCurrentAlt will work correctly
 * now for either a block of alts or a Tree child list.
 *
 * The first time addAlternativeElement is called, it sets the root element
 * rather than adding it to one of the alternative lists.  Rather than have
 * the grammar duplicate the rules for grammar atoms etc... we use the same
 * grammar and same refToken behavior etc...  We have to special case somewhere
 * and here is where we do it.
 */
class TreeBlockContext extends BlockContext {
    protected boolean nextElementIsRoot = true;


    public void addAlternativeElement(AlternativeElement e) {
        TreeElement tree = (TreeElement)block;
        if (nextElementIsRoot) {
            tree.root = (GrammarAtom)e;
            nextElementIsRoot = false;
        }
        else {
            super.addAlternativeElement(e);
        }
    }
}

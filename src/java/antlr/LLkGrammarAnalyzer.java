package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

public interface LLkGrammarAnalyzer extends GrammarAnalyzer {


    public boolean deterministic(AlternativeBlock blk);

    public boolean deterministic(OneOrMoreBlock blk);

    public boolean deterministic(ZeroOrMoreBlock blk);

    public Lookahead FOLLOW(int k, RuleEndElement end);

    public Lookahead look(int k, ActionElement action);

    public Lookahead look(int k, AlternativeBlock blk);

    public Lookahead look(int k, BlockEndElement end);

    public Lookahead look(int k, CharLiteralElement atom);

    public Lookahead look(int k, CharRangeElement end);

    public Lookahead look(int k, GrammarAtom atom);

    public Lookahead look(int k, OneOrMoreBlock blk);

    public Lookahead look(int k, RuleBlock blk);

    public Lookahead look(int k, RuleEndElement end);

    public Lookahead look(int k, RuleRefElement rr);

    public Lookahead look(int k, StringLiteralElement atom);

    public Lookahead look(int k, SynPredBlock blk);

    public Lookahead look(int k, TokenRangeElement end);

    public Lookahead look(int k, TreeElement end);

    public Lookahead look(int k, WildcardElement wc);

    public Lookahead look(int k, ZeroOrMoreBlock blk);

    public Lookahead look(int k, String rule);

    public void setGrammar(Grammar g);

    public boolean subruleCanBeInverted(AlternativeBlock blk, boolean forLexer);
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class RuleRefElement extends AlternativeElement {
    protected String targetRule; // which rule is being called?
    protected String args = null;		 // were any args passed to rule?
    protected String idAssign = null;	 // is the return type assigned to a variable?
    protected String label;


    public RuleRefElement(Grammar g, Token t, int autoGenType_) {
        super(g, t, autoGenType_);
        targetRule = t.getText();
        //		if ( Character.isUpperCase(targetRule.charAt(0)) ) { // lexer rule?
        if (t.type == ANTLRTokenTypes.TOKEN_REF) { // lexer rule?
            targetRule = CodeGenerator.encodeLexerRuleName(targetRule);
        }
    }

//	public RuleRefElement(Grammar g, String t, int line, int autoGenType_) {
//		super(g, autoGenType_);
//		targetRule = t;
//		if ( Character.isUpperCase(targetRule.charAt(0)) ) { // lexer rule?
//			targetRule = CodeGenerator.lexerRuleName(targetRule);
//		}
//		this.line = line;
//	}

    public void generate() {
        grammar.generator.gen(this);
    }

    public String getArgs() {
        return args;
    }

    public String getIdAssign() {
        return idAssign;
    }

    public String getLabel() {
        return label;
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public void setArgs(String a) {
        args = a;
    }

    public void setIdAssign(String id) {
        idAssign = id;
    }

    public void setLabel(String label_) {
        label = label_;
    }

    public String toString() {
        if (args != null)
            return " " + targetRule + args;
        else
            return " " + targetRule;
    }
}

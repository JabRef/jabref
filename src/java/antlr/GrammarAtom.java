package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

/**A GrammarAtom is either a token ref, a character ref, or string.
 * The analysis doesn't care.
 */
abstract class GrammarAtom extends AlternativeElement {
	protected String label;
	protected String atomText;
	protected int tokenType = Token.INVALID_TYPE;
	protected boolean not = false;	// ~T or ~'c' or ~"foo"
	/** Set to type of AST node to create during parse.  Defaults to what is
	 *  set in the TokenSymbol.
	 */
    protected String ASTNodeType = null;

	public GrammarAtom(Grammar g, Token t, int autoGenType) {
		super(g, autoGenType);
		atomText = t.getText();
	}

	public String getLabel() {
		return label;
	}

	public String getText() {
		return atomText;
	}

	public int getType() {
		return tokenType;
	}

	public void setLabel(String label_) { 
		label = label_; 
	}

	public String getASTNodeType() {
		return ASTNodeType;
	}

	public void setASTNodeType(String type) {
		ASTNodeType = type;
	}

	public void setOption(Token option, Token value) {
		if ( option.getText().equals("AST") ) {
			setASTNodeType(value.getText());
		}
		else {
			grammar.tool.error("Invalid element option:"+option.getText(),
							   grammar.getFilename(), option.getLine());
		}
	}

	public String toString() {
		String s = " ";
		if ( label!=null ) s += label+":";
		if ( not ) s += "~";
		return s+atomText;
	}
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.Vector;

import java.util.Hashtable;

/**A list of alternatives and info contained in
 * the rule definition.
 */
public class RuleBlock extends AlternativeBlock {
    protected String ruleName;
    protected String argAction = null;	// string for rule arguments [...]
    protected String throwsSpec = null;
    protected String returnAction = null;// string for rule return type(s) <...>
    protected RuleEndElement endNode;	// which node ends this rule?

    // Generate literal-testing code for lexer rule?
    protected boolean testLiterals = false;

    Vector labeledElements;	// List of labeled elements found in this rule
    // This is a list of AlternativeElement (or subclass)

    protected boolean[] lock;	// for analysis; used to avoid infinite loops
    // 1..k
    protected Lookahead cache[];// Each rule can cache it's lookahead computation.

    // This cache contains an epsilon
    // imaginary token if the FOLLOW is required.  No
    // FOLLOW information is cached here.
    // The FIRST(rule) is stored in this cache; 1..k
    // This set includes FIRST of all alts.

    Hashtable exceptionSpecs;		// table of String-to-ExceptionSpec.

    // grammar-settable options
    protected boolean defaultErrorHandler = true;
    protected String ignoreRule = null;

    /** Construct a named rule. */
    public RuleBlock(Grammar g, String r) {
        super(g);
        ruleName = r;
        labeledElements = new Vector();
        cache = new Lookahead[g.maxk + 1];
        exceptionSpecs = new Hashtable();
        setAutoGen(g instanceof ParserGrammar);
    }

    /** Construct a named rule with line number information */
    public RuleBlock(Grammar g, String r, int line, boolean doAutoGen_) {
        this(g, r);
        this.line = line;
        setAutoGen(doAutoGen_);
    }

    public void addExceptionSpec(ExceptionSpec ex) {
        if (findExceptionSpec(ex.label) != null) {
            if (ex.label != null) {
                grammar.antlrTool.error("Rule '" + ruleName + "' already has an exception handler for label: " + ex.label);
            }
            else {
                grammar.antlrTool.error("Rule '" + ruleName + "' already has an exception handler");
            }
        }
        else {
            exceptionSpecs.put((ex.label == null ? "" : ex.label.getText()), ex);
        }
    }

    public ExceptionSpec findExceptionSpec(Token label) {
        return (ExceptionSpec)exceptionSpecs.get(label == null ? "" : label.getText());
    }

    public ExceptionSpec findExceptionSpec(String label) {
        return (ExceptionSpec)exceptionSpecs.get(label == null ? "" : label);
    }

    public void generate() {
        grammar.generator.gen(this);
    }

    public boolean getDefaultErrorHandler() {
        return defaultErrorHandler;
    }

    public RuleEndElement getEndElement() {
        return endNode;
    }

    public String getIgnoreRule() {
        return ignoreRule;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean getTestLiterals() {
        return testLiterals;
    }

    public boolean isLexerAutoGenRule() {
        return ruleName.equals("nextToken");
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public void prepareForAnalysis() {
        super.prepareForAnalysis();
        lock = new boolean[grammar.maxk + 1];
    }

    // rule option values
    public void setDefaultErrorHandler(boolean value) {
        defaultErrorHandler = value;
    }

    public void setEndElement(RuleEndElement re) {
        endNode = re;
    }

    public void setOption(Token key, Token value) {
        if (key.getText().equals("defaultErrorHandler")) {
            if (value.getText().equals("true")) {
                defaultErrorHandler = true;
            }
            else if (value.getText().equals("false")) {
                defaultErrorHandler = false;
            }
            else {
                grammar.antlrTool.error("Value for defaultErrorHandler must be true or false", grammar.getFilename(), key.getLine(), key.getColumn());
            }
        }
        else if (key.getText().equals("testLiterals")) {
            if (!(grammar instanceof LexerGrammar)) {
                grammar.antlrTool.error("testLiterals option only valid for lexer rules", grammar.getFilename(), key.getLine(), key.getColumn());
            }
            else {
                if (value.getText().equals("true")) {
                    testLiterals = true;
                }
                else if (value.getText().equals("false")) {
                    testLiterals = false;
                }
                else {
                    grammar.antlrTool.error("Value for testLiterals must be true or false", grammar.getFilename(), key.getLine(), key.getColumn());
                }
            }
        }
        else if (key.getText().equals("ignore")) {
            if (!(grammar instanceof LexerGrammar)) {
                grammar.antlrTool.error("ignore option only valid for lexer rules", grammar.getFilename(), key.getLine(), key.getColumn());
            }
            else {
                ignoreRule = value.getText();
            }
        }
        else if (key.getText().equals("paraphrase")) {
            if (!(grammar instanceof LexerGrammar)) {
                grammar.antlrTool.error("paraphrase option only valid for lexer rules", grammar.getFilename(), key.getLine(), key.getColumn());
            }
            else {
                // find token def associated with this rule
                TokenSymbol ts = grammar.tokenManager.getTokenSymbol(ruleName);
                if (ts == null) {
                    grammar.antlrTool.panic("cannot find token associated with rule " + ruleName);
                }
                ts.setParaphrase(value.getText());
            }
        }
        else if (key.getText().equals("generateAmbigWarnings")) {
            if (value.getText().equals("true")) {
                generateAmbigWarnings = true;
            }
            else if (value.getText().equals("false")) {
                generateAmbigWarnings = false;
            }
            else {
                grammar.antlrTool.error("Value for generateAmbigWarnings must be true or false", grammar.getFilename(), key.getLine(), key.getColumn());
            }
        }
        else {
            grammar.antlrTool.error("Invalid rule option: " + key.getText(), grammar.getFilename(), key.getLine(), key.getColumn());
        }
    }

    public String toString() {
        String s = " FOLLOW={";
        Lookahead cache[] = endNode.cache;
        int k = grammar.maxk;
        boolean allNull = true;
        for (int j = 1; j <= k; j++) {
            if (cache[j] == null) continue;
            s += cache[j].toString(",", grammar.tokenManager.getVocabulary());
            allNull = false;
            if (j < k && cache[j + 1] != null) s += ";";
        }
        s += "}";
        if (allNull) s = "";
        return ruleName + ": " + super.toString() + " ;" + s;
    }
}

// $ANTLR : "preproc.g" -> "Preprocessor.java"$

package antlr.preprocessor;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

import antlr.collections.impl.IndexedVector;
import java.util.Hashtable;
import antlr.preprocessor.Grammar;

public class Preprocessor extends antlr.LLkParser       implements PreprocessorTokenTypes
 {

	// This chunk of error reporting code provided by Brian Smith

    private antlr.Tool antlrTool;

    /** In order to make it so existing subclasses don't break, we won't require
     * that the antlr.Tool instance be passed as a constructor element. Instead,
     * the antlr.Tool instance should register itself via {@link #initTool(antlr.Tool)}
     * @throws IllegalStateException if a tool has already been registered
     * @since 2.7.2
     */
    public void setTool(antlr.Tool tool) {
        if (antlrTool == null) {
            antlrTool = tool;
		}
        else {
            throw new IllegalStateException("antlr.Tool already registered");
		}
    }

    /** @since 2.7.2 */
    protected antlr.Tool getTool() {
        return antlrTool;
    }

    /** Delegates the error message to the tool if any was registered via
     *  {@link #initTool(antlr.Tool)}
     *  @since 2.7.2
     */
    public void reportError(String s) {
        if (getTool() != null) {
            getTool().error(s, getFilename(), -1, -1);
		}
        else {
            super.reportError(s);
		}
    }

    /** Delegates the error message to the tool if any was registered via
     *  {@link #initTool(antlr.Tool)}
     *  @since 2.7.2
     */
    public void reportError(RecognitionException e) {
        if (getTool() != null) {
            getTool().error(e.getErrorMessage(), e.getFilename(), e.getLine(), e.getColumn());
		}
        else {
            super.reportError(e);
		}
    }

    /** Delegates the warning message to the tool if any was registered via
     *  {@link #initTool(antlr.Tool)}
     *  @since 2.7.2
     */
    public void reportWarning(String s) {
        if (getTool() != null) {
            getTool().warning(s, getFilename(), -1, -1);
		}
        else {
            super.reportWarning(s);
		}
    }

protected Preprocessor(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public Preprocessor(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected Preprocessor(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public Preprocessor(TokenStream lexer) {
  this(lexer,1);
}

public Preprocessor(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void grammarFile(
		Hierarchy hier, String file
	) throws RecognitionException, TokenStreamException {
		
		Token  hdr = null;
		
			Grammar gr;
			IndexedVector opt=null;
		
		
		try {      // for error handling
			{
			_loop265:
			do {
				if ((LA(1)==HEADER_ACTION)) {
					hdr = LT(1);
					match(HEADER_ACTION);
					hier.getFile(file).addHeaderAction(hdr.getText());
				}
				else {
					break _loop265;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				opt=optionSpec(null);
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			_loop268:
			do {
				if ((LA(1)==ACTION||LA(1)==LITERAL_class)) {
					gr=class_def(file, hier);
					
								if ( gr!=null && opt!=null ) {
									hier.getFile(file).setOptions(opt);
								}
								if ( gr!=null ) {
									gr.setFileName(file);
									hier.addGrammar(gr);
								}
								
				}
				else {
					break _loop268;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
	}
	
	public final IndexedVector  optionSpec(
		Grammar gr
	) throws RecognitionException, TokenStreamException {
		IndexedVector options;
		
		Token  op = null;
		Token  rhs = null;
		
			options = new IndexedVector();
		
		
		try {      // for error handling
			match(OPTIONS_START);
			{
			_loop280:
			do {
				if ((LA(1)==ID)) {
					op = LT(1);
					match(ID);
					rhs = LT(1);
					match(ASSIGN_RHS);
					
									Option newOp = new Option(op.getText(),rhs.getText(),gr);
									options.appendElement(newOp.getName(),newOp);
									if ( gr!=null && op.getText().equals("importVocab") ) {
										gr.specifiedVocabulary = true;
										gr.importVocab = rhs.getText();
									}
									else if ( gr!=null && op.getText().equals("exportVocab") ) {
										// don't want ';' included in outputVocab.
										// This is heinously inconsistent!  Ugh.
										gr.exportVocab = rhs.getText().substring(0,rhs.getText().length()-1);
										gr.exportVocab = gr.exportVocab.trim();
									}
									
				}
				else {
					break _loop280;
				}
				
			} while (true);
			}
			match(RCURLY);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return options;
	}
	
	public final Grammar  class_def(
		String file, Hierarchy hier
	) throws RecognitionException, TokenStreamException {
		Grammar gr;
		
		Token  preamble = null;
		Token  sub = null;
		Token  sup = null;
		Token  tk = null;
		Token  memberA = null;
		
			gr=null;
			IndexedVector rules = new IndexedVector(100);
			IndexedVector classOptions = null;
			String sc = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ACTION:
			{
				preamble = LT(1);
				match(ACTION);
				break;
			}
			case LITERAL_class:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LITERAL_class);
			sub = LT(1);
			match(ID);
			match(LITERAL_extends);
			sup = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case SUBRULE_BLOCK:
			{
				sc=superClass();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			
						gr = (Grammar)hier.getGrammar(sub.getText());
						if ( gr!=null ) {
			//				antlr.Tool.toolError("redefinition of grammar "+gr.getName()+" ignored");
							gr=null;
							throw new SemanticException("redefinition of grammar "+sub.getText(), file, sub.getLine(), sub.getColumn());
						}
						else {
							gr = new Grammar(hier.getTool(), sub.getText(), sup.getText(), rules);
							gr.superClass=sc;
							if ( preamble!=null ) {
								gr.setPreambleAction(preamble.getText());
							}
						}
					
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				classOptions=optionSpec(gr);
				break;
			}
			case ACTION:
			case ID:
			case TOKENS_SPEC:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
					if ( gr!=null ) {
						gr.setOptions(classOptions);
					}
					
			{
			switch ( LA(1)) {
			case TOKENS_SPEC:
			{
				tk = LT(1);
				match(TOKENS_SPEC);
				gr.setTokenSection(tk.getText());
				break;
			}
			case ACTION:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case ACTION:
			{
				memberA = LT(1);
				match(ACTION);
				gr.setMemberAction(memberA.getText());
				break;
			}
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			int _cnt277=0;
			_loop277:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					rule(gr);
				}
				else {
					if ( _cnt277>=1 ) { break _loop277; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt277++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return gr;
	}
	
	public final String  superClass() throws RecognitionException, TokenStreamException {
		String sup;
		
		sup=LT(1).getText();
		
		try {      // for error handling
			match(SUBRULE_BLOCK);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
		return sup;
	}
	
	public final void rule(
		Grammar gr
	) throws RecognitionException, TokenStreamException {
		
		Token  r = null;
		Token  arg = null;
		Token  ret = null;
		Token  init = null;
		Token  blk = null;
		
			IndexedVector o = null;	// options for rule
			String vis = null;
			boolean bang=false;
			String eg=null, thr="";
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_protected:
			{
				match(LITERAL_protected);
				vis="protected";
				break;
			}
			case LITERAL_private:
			{
				match(LITERAL_private);
				vis="private";
				break;
			}
			case LITERAL_public:
			{
				match(LITERAL_public);
				vis="public";
				break;
			}
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			r = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case BANG:
			{
				match(BANG);
				bang=true;
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case ARG_ACTION:
			case LITERAL_returns:
			case RULE_BLOCK:
			case LITERAL_throws:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				arg = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case LITERAL_returns:
			case RULE_BLOCK:
			case LITERAL_throws:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_returns:
			{
				match(LITERAL_returns);
				ret = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case RULE_BLOCK:
			case LITERAL_throws:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_throws:
			{
				thr=throwsSpec();
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				o=optionSpec(null);
				break;
			}
			case ACTION:
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case ACTION:
			{
				init = LT(1);
				match(ACTION);
				break;
			}
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			blk = LT(1);
			match(RULE_BLOCK);
			eg=exceptionGroup();
			
					String rtext = blk.getText()+eg;
					Rule ppr = new Rule(r.getText(),rtext,o,gr);
					ppr.setThrowsSpec(thr);
					if ( arg!=null ) {
						ppr.setArgs(arg.getText());
					}
					if ( ret!=null ) {
						ppr.setReturnValue(ret.getText());
					}
					if ( init!=null ) {
						ppr.setInitAction(init.getText());
					}
					if ( bang ) {
						ppr.setBang();
					}
					ppr.setVisibility(vis);
					if ( gr!=null ) {
						gr.addRule(ppr);
					}
					
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
	}
	
	public final String  throwsSpec() throws RecognitionException, TokenStreamException {
		String t;
		
		Token  a = null;
		Token  b = null;
		t="throws ";
		
		try {      // for error handling
			match(LITERAL_throws);
			a = LT(1);
			match(ID);
			t+=a.getText();
			{
			_loop291:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					b = LT(1);
					match(ID);
					t+=","+b.getText();
				}
				else {
					break _loop291;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
		return t;
	}
	
	public final String  exceptionGroup() throws RecognitionException, TokenStreamException {
		String g;
		
		String e=null; g="";
		
		try {      // for error handling
			{
			_loop294:
			do {
				if ((LA(1)==LITERAL_exception)) {
					e=exceptionSpec();
					g += e;
				}
				else {
					break _loop294;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
		return g;
	}
	
	public final String  exceptionSpec() throws RecognitionException, TokenStreamException {
		String es;
		
		Token  aa = null;
		String h=null;
		es = System.getProperty("line.separator")+"exception ";
		
		
		try {      // for error handling
			match(LITERAL_exception);
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				aa = LT(1);
				match(ARG_ACTION);
				es += aa.getText();
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_exception:
			case LITERAL_catch:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			_loop298:
			do {
				if ((LA(1)==LITERAL_catch)) {
					h=exceptionHandler();
					es += h;
				}
				else {
					break _loop298;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return es;
	}
	
	public final String  exceptionHandler() throws RecognitionException, TokenStreamException {
		String h;
		
		Token  a1 = null;
		Token  a2 = null;
		h=null;
		
		try {      // for error handling
			match(LITERAL_catch);
			a1 = LT(1);
			match(ARG_ACTION);
			a2 = LT(1);
			match(ACTION);
			h = System.getProperty("line.separator")+
						 "catch "+a1.getText()+" "+a2.getText();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_8);
		}
		return h;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"tokens\"",
		"HEADER_ACTION",
		"SUBRULE_BLOCK",
		"ACTION",
		"\"class\"",
		"ID",
		"\"extends\"",
		"SEMI",
		"TOKENS_SPEC",
		"OPTIONS_START",
		"ASSIGN_RHS",
		"RCURLY",
		"\"protected\"",
		"\"private\"",
		"\"public\"",
		"BANG",
		"ARG_ACTION",
		"\"returns\"",
		"RULE_BLOCK",
		"\"throws\"",
		"COMMA",
		"\"exception\"",
		"\"catch\"",
		"ALT",
		"ELEMENT",
		"LPAREN",
		"RPAREN",
		"ID_OR_KEYWORD",
		"CURLY_BLOCK_SCARF",
		"WS",
		"NEWLINE",
		"COMMENT",
		"SL_COMMENT",
		"ML_COMMENT",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"ESC",
		"DIGIT",
		"XDIGIT"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 4658050L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 459264L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 386L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 2048L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 459650L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 4202624L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 34014082L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 101122946L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	
	}

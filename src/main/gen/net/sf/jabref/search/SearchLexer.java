// Generated from /home/fronchetti/workspace/jabref/src/main/antlr4/net/sf/jabref/search/Search.g4 by ANTLR 4.5.3
package net.sf.jabref.search;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SearchLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, LPAREN=2, RPAREN=3, EQUAL=4, EEQUAL=5, NEQUAL=6, AND=7, OR=8, CONTAINS=9, 
		MATCHES=10, NOT=11, STRING=12, QUOTE=13, FIELDTYPE=14;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"WS", "LPAREN", "RPAREN", "EQUAL", "EEQUAL", "NEQUAL", "AND", "OR", "CONTAINS", 
		"MATCHES", "NOT", "STRING", "QUOTE", "FIELDTYPE", "LETTER"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'('", "')'", "'='", "'=='", "'!='", null, null, null, null, 
		null, null, "'\"'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "WS", "LPAREN", "RPAREN", "EQUAL", "EEQUAL", "NEQUAL", "AND", "OR", 
		"CONTAINS", "MATCHES", "NOT", "STRING", "QUOTE", "FIELDTYPE"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public SearchLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Search.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\20_\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\2\3\2\3\3"+
		"\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\t\3\t\3"+
		"\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\f\3\f\3\f\3\f\3\r\3\r\7\rP\n\r\f\r\16\rS\13\r\3\r\3\r\3\16"+
		"\3\16\3\17\6\17Z\n\17\r\17\16\17[\3\20\3\20\2\2\21\3\3\5\4\7\5\t\6\13"+
		"\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\2\3\2\21\4\2\13"+
		"\13\"\"\4\2CCcc\4\2PPpp\4\2FFff\4\2QQqq\4\2TTtt\4\2EEee\4\2VVvv\4\2KK"+
		"kk\4\2UUuu\4\2OOoo\4\2JJjj\4\2GGgg\3\2$$\6\2\13\13\"$*+??_\2\3\3\2\2\2"+
		"\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2"+
		"\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2"+
		"\2\33\3\2\2\2\2\35\3\2\2\2\3!\3\2\2\2\5%\3\2\2\2\7\'\3\2\2\2\t)\3\2\2"+
		"\2\13+\3\2\2\2\r.\3\2\2\2\17\61\3\2\2\2\21\65\3\2\2\2\238\3\2\2\2\25A"+
		"\3\2\2\2\27I\3\2\2\2\31M\3\2\2\2\33V\3\2\2\2\35Y\3\2\2\2\37]\3\2\2\2!"+
		"\"\t\2\2\2\"#\3\2\2\2#$\b\2\2\2$\4\3\2\2\2%&\7*\2\2&\6\3\2\2\2\'(\7+\2"+
		"\2(\b\3\2\2\2)*\7?\2\2*\n\3\2\2\2+,\7?\2\2,-\7?\2\2-\f\3\2\2\2./\7#\2"+
		"\2/\60\7?\2\2\60\16\3\2\2\2\61\62\t\3\2\2\62\63\t\4\2\2\63\64\t\5\2\2"+
		"\64\20\3\2\2\2\65\66\t\6\2\2\66\67\t\7\2\2\67\22\3\2\2\289\t\b\2\29:\t"+
		"\6\2\2:;\t\4\2\2;<\t\t\2\2<=\t\3\2\2=>\t\n\2\2>?\t\4\2\2?@\t\13\2\2@\24"+
		"\3\2\2\2AB\t\f\2\2BC\t\3\2\2CD\t\t\2\2DE\t\b\2\2EF\t\r\2\2FG\t\16\2\2"+
		"GH\t\13\2\2H\26\3\2\2\2IJ\t\4\2\2JK\t\6\2\2KL\t\t\2\2L\30\3\2\2\2MQ\5"+
		"\33\16\2NP\n\17\2\2ON\3\2\2\2PS\3\2\2\2QO\3\2\2\2QR\3\2\2\2RT\3\2\2\2"+
		"SQ\3\2\2\2TU\5\33\16\2U\32\3\2\2\2VW\7$\2\2W\34\3\2\2\2XZ\5\37\20\2YX"+
		"\3\2\2\2Z[\3\2\2\2[Y\3\2\2\2[\\\3\2\2\2\\\36\3\2\2\2]^\n\20\2\2^ \3\2"+
		"\2\2\5\2Q[\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
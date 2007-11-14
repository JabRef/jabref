package net.sf.jabref.bst;

// $ANTLR 3.0b5 Bst.g 2006-11-23 23:20:24

import org.antlr.runtime.CharStream;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

public class BstLexer extends Lexer {
    public static final int LETTER=21;
    public static final int T29=29;
    public static final int T33=33;
    public static final int INTEGERS=9;
    public static final int ENTRY=6;
    public static final int WS=23;
    public static final int COMMANDS=7;
    public static final int STRING=12;
    public static final int T28=28;
    public static final int EXECUTE=14;
    public static final int LINE_COMMENT=24;
    public static final int SORT=17;
    public static final int STACK=5;
    public static final int REVERSE=16;
    public static final int QUOTED=20;
    public static final int T25=25;
    public static final int INTEGER=19;
    public static final int ITERATE=15;
    public static final int FUNCTION=10;
    public static final int T26=26;
    public static final int EOF=-1;
    public static final int T32=32;
    public static final int Tokens=34;
    public static final int STRINGS=8;
    public static final int T31=31;
    public static final int T27=27;
    public static final int IDENTIFIER=18;
    public static final int MACRO=11;
    public static final int T30=30;
    public static final int IDLIST=4;
    public static final int NUMERAL=22;
    public static final int READ=13;
    public BstLexer() {
        
    } 
    public BstLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "Bst.g"; }

    // $ANTLR start T25
    public void mT25() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T25;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:3:7: ( '{' )
            // Bst.g:3:7: '{'
            {
            match('{'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T25

    // $ANTLR start T26
    public void mT26() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T26;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:4:7: ( '}' )
            // Bst.g:4:7: '}'
            {
            match('}'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T26

    // $ANTLR start T27
    public void mT27() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T27;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:5:7: ( '<' )
            // Bst.g:5:7: '<'
            {
            match('<'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T27

    // $ANTLR start T28
    public void mT28() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T28;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:6:7: ( '>' )
            // Bst.g:6:7: '>'
            {
            match('>'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T28

    // $ANTLR start T29
    public void mT29() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T29;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:7:7: ( '=' )
            // Bst.g:7:7: '='
            {
            match('='); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T29

    // $ANTLR start T30
    public void mT30() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T30;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:8:7: ( '+' )
            // Bst.g:8:7: '+'
            {
            match('+'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T30

    // $ANTLR start T31
    public void mT31() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T31;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:9:7: ( '-' )
            // Bst.g:9:7: '-'
            {
            match('-'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T31

    // $ANTLR start T32
    public void mT32() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T32;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:10:7: ( ':=' )
            // Bst.g:10:7: ':='
            {
            match(":="); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T32

    // $ANTLR start T33
    public void mT33() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = T33;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:11:7: ( '*' )
            // Bst.g:11:7: '*'
            {
            match('*'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T33

    // $ANTLR start STRINGS
    public void mSTRINGS() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = STRINGS;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:53:11: ( 'STRINGS' )
            // Bst.g:53:11: 'STRINGS'
            {
            match("STRINGS"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end STRINGS

    // $ANTLR start INTEGERS
    public void mINTEGERS() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = INTEGERS;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:54:12: ( 'INTEGERS' )
            // Bst.g:54:12: 'INTEGERS'
            {
            match("INTEGERS"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end INTEGERS

    // $ANTLR start FUNCTION
    public void mFUNCTION() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = FUNCTION;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:55:12: ( 'FUNCTION' )
            // Bst.g:55:12: 'FUNCTION'
            {
            match("FUNCTION"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end FUNCTION

    // $ANTLR start EXECUTE
    public void mEXECUTE() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = EXECUTE;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:56:11: ( 'EXECUTE' )
            // Bst.g:56:11: 'EXECUTE'
            {
            match("EXECUTE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end EXECUTE

    // $ANTLR start SORT
    public void mSORT() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = SORT;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:57:8: ( 'SORT' )
            // Bst.g:57:8: 'SORT'
            {
            match("SORT"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end SORT

    // $ANTLR start ITERATE
    public void mITERATE() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = ITERATE;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:58:11: ( 'ITERATE' )
            // Bst.g:58:11: 'ITERATE'
            {
            match("ITERATE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end ITERATE

    // $ANTLR start REVERSE
    public void mREVERSE() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = REVERSE;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:59:11: ( 'REVERSE' )
            // Bst.g:59:11: 'REVERSE'
            {
            match("REVERSE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end REVERSE

    // $ANTLR start ENTRY
    public void mENTRY() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = ENTRY;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:60:9: ( 'ENTRY' )
            // Bst.g:60:9: 'ENTRY'
            {
            match("ENTRY"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end ENTRY

    // $ANTLR start READ
    public void mREAD() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = READ;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:61:8: ( 'READ' )
            // Bst.g:61:8: 'READ'
            {
            match("READ"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end READ

    // $ANTLR start MACRO
    public void mMACRO() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = MACRO;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:62:9: ( 'MACRO' )
            // Bst.g:62:9: 'MACRO'
            {
            match("MACRO"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end MACRO

    // $ANTLR start QUOTED
    public void mQUOTED() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = QUOTED;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:65:4: ( '\\'' IDENTIFIER )
            // Bst.g:65:4: '\\'' IDENTIFIER
            {
            match('\''); 
            mIDENTIFIER(); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end QUOTED

    // $ANTLR start IDENTIFIER
    public void mIDENTIFIER() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = IDENTIFIER;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:68:4: ( LETTER ( LETTER | NUMERAL )* )
            // Bst.g:68:4: LETTER ( LETTER | NUMERAL )*
            {
            mLETTER(); 
            // Bst.g:68:11: ( LETTER | NUMERAL )*
            loop1:
            do {
                int alt1=3;
                int LA1_0 = input.LA(1);
                if ( (LA1_0=='$'||LA1_0=='.'||(LA1_0>='A' && LA1_0<='Z')||(LA1_0>='a' && LA1_0<='z')) ) {
                    alt1=1;
                }
                else if ( ((LA1_0>='0' && LA1_0<='9')) ) {
                    alt1=2;
                }


                switch (alt1) {
            	case 1 :
            	    // Bst.g:68:12: LETTER
            	    {
            	    mLETTER(); 

            	    }
            	    break;
            	case 2 :
            	    // Bst.g:68:19: NUMERAL
            	    {
            	    mNUMERAL(); 

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end IDENTIFIER

    // $ANTLR start LETTER
    public void mLETTER() throws RecognitionException {
        try {
            ruleNestingLevel++;
            // Bst.g:71:4: ( ('a'..'z'|'A'..'Z'|'.'|'$'))
            // Bst.g:71:4: ('a'..'z'|'A'..'Z'|'.'|'$')
            {
            if ( input.LA(1)=='$'||input.LA(1)=='.'||(input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end LETTER

    // $ANTLR start STRING
    public void mSTRING() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = STRING;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:74:4: ( '\"' (~ '\"' )* '\"' )
            // Bst.g:74:4: '\"' (~ '\"' )* '\"'
            {
            match('\"'); 
            // Bst.g:74:8: (~ '\"' )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);
                if ( ((LA2_0>='\u0000' && LA2_0<='!')||(LA2_0>='#' && LA2_0<='\uFFFE')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // Bst.g:74:9: ~ '\"'
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);

            match('\"'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end STRING

    // $ANTLR start INTEGER
    public void mINTEGER() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = INTEGER;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:77:4: ( '#' ( ('+'|'-'))? ( NUMERAL )+ )
            // Bst.g:77:4: '#' ( ('+'|'-'))? ( NUMERAL )+
            {
            match('#'); 
            // Bst.g:77:8: ( ('+'|'-'))?
            int alt3=2;
            int LA3_0 = input.LA(1);
            if ( (LA3_0=='+'||LA3_0=='-') ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // Bst.g:77:9: ('+'|'-')
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }


                    }
                    break;

            }

            // Bst.g:77:19: ( NUMERAL )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);
                if ( ((LA4_0>='0' && LA4_0<='9')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // Bst.g:77:19: NUMERAL
            	    {
            	    mNUMERAL(); 

            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end INTEGER

    // $ANTLR start NUMERAL
    public void mNUMERAL() throws RecognitionException {
        try {
            ruleNestingLevel++;
            // Bst.g:80:4: ( ( '0' .. '9' ) )
            // Bst.g:80:4: ( '0' .. '9' )
            {
            // Bst.g:80:4: ( '0' .. '9' )
            // Bst.g:80:5: '0' .. '9'
            {
            matchRange('0','9'); 

            }


            }

        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end NUMERAL

    // $ANTLR start WS
    public void mWS() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = WS;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:82:9: ( ( (' '|'\\t'|'\\r'|'\\n'))+ )
            // Bst.g:82:9: ( (' '|'\\t'|'\\r'|'\\n'))+
            {
            // Bst.g:82:9: ( (' '|'\\t'|'\\r'|'\\n'))+
            int cnt5=0;
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);
                if ( ((LA5_0>='\t' && LA5_0<='\n')||LA5_0=='\r'||LA5_0==' ') ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // Bst.g:82:13: (' '|'\\t'|'\\r'|'\\n')
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt5 >= 1 ) break loop5;
                        EarlyExitException eee =
                            new EarlyExitException(5, input);
                        throw eee;
                }
                cnt5++;
            } while (true);

             _channel=HIDDEN; 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end WS

    // $ANTLR start LINE_COMMENT
    public void mLINE_COMMENT() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int _type = LINE_COMMENT;
            int _start = getCharIndex();
            int _line = getLine();
            int _charPosition = getCharPositionInLine();
            int _channel = Token.DEFAULT_CHANNEL;
            // Bst.g:90:7: ( '%' (~ ('\\n'|'\\r'))* ( '\\r' )? '\\n' )
            // Bst.g:90:7: '%' (~ ('\\n'|'\\r'))* ( '\\r' )? '\\n'
            {
            match('%'); 
            // Bst.g:90:11: (~ ('\\n'|'\\r'))*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);
                if ( ((LA6_0>='\u0000' && LA6_0<='\t')||(LA6_0>='\u000B' && LA6_0<='\f')||(LA6_0>='\u000E' && LA6_0<='\uFFFE')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // Bst.g:90:11: ~ ('\\n'|'\\r')
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\f')||(input.LA(1)>='\u000E' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            // Bst.g:90:25: ( '\\r' )?
            int alt7=2;
            int LA7_0 = input.LA(1);
            if ( (LA7_0=='\r') ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // Bst.g:90:25: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 
             _channel=HIDDEN; 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(_type,_line,_charPosition,_channel,_start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end LINE_COMMENT

    public void mTokens() throws RecognitionException {
        // Bst.g:1:10: ( T25 | T26 | T27 | T28 | T29 | T30 | T31 | T32 | T33 | STRINGS | INTEGERS | FUNCTION | EXECUTE | SORT | ITERATE | REVERSE | ENTRY | READ | MACRO | QUOTED | IDENTIFIER | STRING | INTEGER | WS | LINE_COMMENT )
        int alt8=25;
        switch ( input.LA(1) ) {
        case '{':
            alt8=1;
            break;
        case '}':
            alt8=2;
            break;
        case '<':
            alt8=3;
            break;
        case '>':
            alt8=4;
            break;
        case '=':
            alt8=5;
            break;
        case '+':
            alt8=6;
            break;
        case '-':
            alt8=7;
            break;
        case ':':
            alt8=8;
            break;
        case '*':
            alt8=9;
            break;
        case 'S':
            switch ( input.LA(2) ) {
            case 'T':
                int LA8_22 = input.LA(3);
                if ( (LA8_22=='R') ) {
                    int LA8_31 = input.LA(4);
                    if ( (LA8_31=='I') ) {
                        int LA8_41 = input.LA(5);
                        if ( (LA8_41=='N') ) {
                            int LA8_51 = input.LA(6);
                            if ( (LA8_51=='G') ) {
                                int LA8_61 = input.LA(7);
                                if ( (LA8_61=='S') ) {
                                    int LA8_69 = input.LA(8);
                                    if ( (LA8_69=='$'||LA8_69=='.'||(LA8_69>='0' && LA8_69<='9')||(LA8_69>='A' && LA8_69<='Z')||(LA8_69>='a' && LA8_69<='z')) ) {
                                        alt8=21;
                                    }
                                    else {
                                        alt8=10;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            case 'O':
                int LA8_23 = input.LA(3);
                if ( (LA8_23=='R') ) {
                    int LA8_32 = input.LA(4);
                    if ( (LA8_32=='T') ) {
                        int LA8_42 = input.LA(5);
                        if ( (LA8_42=='$'||LA8_42=='.'||(LA8_42>='0' && LA8_42<='9')||(LA8_42>='A' && LA8_42<='Z')||(LA8_42>='a' && LA8_42<='z')) ) {
                            alt8=21;
                        }
                        else {
                            alt8=14;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            default:
                alt8=21;}

            break;
        case 'I':
            switch ( input.LA(2) ) {
            case 'T':
                int LA8_24 = input.LA(3);
                if ( (LA8_24=='E') ) {
                    int LA8_33 = input.LA(4);
                    if ( (LA8_33=='R') ) {
                        int LA8_43 = input.LA(5);
                        if ( (LA8_43=='A') ) {
                            int LA8_53 = input.LA(6);
                            if ( (LA8_53=='T') ) {
                                int LA8_62 = input.LA(7);
                                if ( (LA8_62=='E') ) {
                                    int LA8_70 = input.LA(8);
                                    if ( (LA8_70=='$'||LA8_70=='.'||(LA8_70>='0' && LA8_70<='9')||(LA8_70>='A' && LA8_70<='Z')||(LA8_70>='a' && LA8_70<='z')) ) {
                                        alt8=21;
                                    }
                                    else {
                                        alt8=15;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            case 'N':
                int LA8_25 = input.LA(3);
                if ( (LA8_25=='T') ) {
                    int LA8_34 = input.LA(4);
                    if ( (LA8_34=='E') ) {
                        int LA8_44 = input.LA(5);
                        if ( (LA8_44=='G') ) {
                            int LA8_54 = input.LA(6);
                            if ( (LA8_54=='E') ) {
                                int LA8_63 = input.LA(7);
                                if ( (LA8_63=='R') ) {
                                    int LA8_71 = input.LA(8);
                                    if ( (LA8_71=='S') ) {
                                        int LA8_77 = input.LA(9);
                                        if ( (LA8_77=='$'||LA8_77=='.'||(LA8_77>='0' && LA8_77<='9')||(LA8_77>='A' && LA8_77<='Z')||(LA8_77>='a' && LA8_77<='z')) ) {
                                            alt8=21;
                                        }
                                        else {
                                            alt8=11;}
                                    }
                                    else {
                                        alt8=21;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            default:
                alt8=21;}

            break;
        case 'F':
            int LA8_12 = input.LA(2);
            if ( (LA8_12=='U') ) {
                int LA8_26 = input.LA(3);
                if ( (LA8_26=='N') ) {
                    int LA8_35 = input.LA(4);
                    if ( (LA8_35=='C') ) {
                        int LA8_45 = input.LA(5);
                        if ( (LA8_45=='T') ) {
                            int LA8_55 = input.LA(6);
                            if ( (LA8_55=='I') ) {
                                int LA8_64 = input.LA(7);
                                if ( (LA8_64=='O') ) {
                                    int LA8_72 = input.LA(8);
                                    if ( (LA8_72=='N') ) {
                                        int LA8_78 = input.LA(9);
                                        if ( (LA8_78=='$'||LA8_78=='.'||(LA8_78>='0' && LA8_78<='9')||(LA8_78>='A' && LA8_78<='Z')||(LA8_78>='a' && LA8_78<='z')) ) {
                                            alt8=21;
                                        }
                                        else {
                                            alt8=12;}
                                    }
                                    else {
                                        alt8=21;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
            }
            else {
                alt8=21;}
            break;
        case 'E':
            switch ( input.LA(2) ) {
            case 'N':
                int LA8_27 = input.LA(3);
                if ( (LA8_27=='T') ) {
                    int LA8_36 = input.LA(4);
                    if ( (LA8_36=='R') ) {
                        int LA8_46 = input.LA(5);
                        if ( (LA8_46=='Y') ) {
                            int LA8_56 = input.LA(6);
                            if ( (LA8_56=='$'||LA8_56=='.'||(LA8_56>='0' && LA8_56<='9')||(LA8_56>='A' && LA8_56<='Z')||(LA8_56>='a' && LA8_56<='z')) ) {
                                alt8=21;
                            }
                            else {
                                alt8=17;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            case 'X':
                int LA8_28 = input.LA(3);
                if ( (LA8_28=='E') ) {
                    int LA8_37 = input.LA(4);
                    if ( (LA8_37=='C') ) {
                        int LA8_47 = input.LA(5);
                        if ( (LA8_47=='U') ) {
                            int LA8_57 = input.LA(6);
                            if ( (LA8_57=='T') ) {
                                int LA8_66 = input.LA(7);
                                if ( (LA8_66=='E') ) {
                                    int LA8_73 = input.LA(8);
                                    if ( (LA8_73=='$'||LA8_73=='.'||(LA8_73>='0' && LA8_73<='9')||(LA8_73>='A' && LA8_73<='Z')||(LA8_73>='a' && LA8_73<='z')) ) {
                                        alt8=21;
                                    }
                                    else {
                                        alt8=13;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
                break;
            default:
                alt8=21;}

            break;
        case 'R':
            int LA8_14 = input.LA(2);
            if ( (LA8_14=='E') ) {
                switch ( input.LA(3) ) {
                case 'A':
                    int LA8_38 = input.LA(4);
                    if ( (LA8_38=='D') ) {
                        int LA8_48 = input.LA(5);
                        if ( (LA8_48=='$'||LA8_48=='.'||(LA8_48>='0' && LA8_48<='9')||(LA8_48>='A' && LA8_48<='Z')||(LA8_48>='a' && LA8_48<='z')) ) {
                            alt8=21;
                        }
                        else {
                            alt8=18;}
                    }
                    else {
                        alt8=21;}
                    break;
                case 'V':
                    int LA8_39 = input.LA(4);
                    if ( (LA8_39=='E') ) {
                        int LA8_49 = input.LA(5);
                        if ( (LA8_49=='R') ) {
                            int LA8_59 = input.LA(6);
                            if ( (LA8_59=='S') ) {
                                int LA8_67 = input.LA(7);
                                if ( (LA8_67=='E') ) {
                                    int LA8_74 = input.LA(8);
                                    if ( (LA8_74=='$'||LA8_74=='.'||(LA8_74>='0' && LA8_74<='9')||(LA8_74>='A' && LA8_74<='Z')||(LA8_74>='a' && LA8_74<='z')) ) {
                                        alt8=21;
                                    }
                                    else {
                                        alt8=16;}
                                }
                                else {
                                    alt8=21;}
                            }
                            else {
                                alt8=21;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                    break;
                default:
                    alt8=21;}

            }
            else {
                alt8=21;}
            break;
        case 'M':
            int LA8_15 = input.LA(2);
            if ( (LA8_15=='A') ) {
                int LA8_30 = input.LA(3);
                if ( (LA8_30=='C') ) {
                    int LA8_40 = input.LA(4);
                    if ( (LA8_40=='R') ) {
                        int LA8_50 = input.LA(5);
                        if ( (LA8_50=='O') ) {
                            int LA8_60 = input.LA(6);
                            if ( (LA8_60=='$'||LA8_60=='.'||(LA8_60>='0' && LA8_60<='9')||(LA8_60>='A' && LA8_60<='Z')||(LA8_60>='a' && LA8_60<='z')) ) {
                                alt8=21;
                            }
                            else {
                                alt8=19;}
                        }
                        else {
                            alt8=21;}
                    }
                    else {
                        alt8=21;}
                }
                else {
                    alt8=21;}
            }
            else {
                alt8=21;}
            break;
        case '\'':
            alt8=20;
            break;
        case '$':
        case '.':
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'G':
        case 'H':
        case 'J':
        case 'K':
        case 'L':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'g':
        case 'h':
        case 'i':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
        case 'n':
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
        case 't':
        case 'u':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
            alt8=21;
            break;
        case '\"':
            alt8=22;
            break;
        case '#':
            alt8=23;
            break;
        case '\t':
        case '\n':
        case '\r':
        case ' ':
            alt8=24;
            break;
        case '%':
            alt8=25;
            break;
        default:
            NoViableAltException nvae =
                new NoViableAltException("1:1: Tokens : ( T25 | T26 | T27 | T28 | T29 | T30 | T31 | T32 | T33 | STRINGS | INTEGERS | FUNCTION | EXECUTE | SORT | ITERATE | REVERSE | ENTRY | READ | MACRO | QUOTED | IDENTIFIER | STRING | INTEGER | WS | LINE_COMMENT );", 8, 0, input);

            throw nvae;
        }

        switch (alt8) {
            case 1 :
                // Bst.g:1:10: T25
                {
                mT25(); 

                }
                break;
            case 2 :
                // Bst.g:1:14: T26
                {
                mT26(); 

                }
                break;
            case 3 :
                // Bst.g:1:18: T27
                {
                mT27(); 

                }
                break;
            case 4 :
                // Bst.g:1:22: T28
                {
                mT28(); 

                }
                break;
            case 5 :
                // Bst.g:1:26: T29
                {
                mT29(); 

                }
                break;
            case 6 :
                // Bst.g:1:30: T30
                {
                mT30(); 

                }
                break;
            case 7 :
                // Bst.g:1:34: T31
                {
                mT31(); 

                }
                break;
            case 8 :
                // Bst.g:1:38: T32
                {
                mT32(); 

                }
                break;
            case 9 :
                // Bst.g:1:42: T33
                {
                mT33(); 

                }
                break;
            case 10 :
                // Bst.g:1:46: STRINGS
                {
                mSTRINGS(); 

                }
                break;
            case 11 :
                // Bst.g:1:54: INTEGERS
                {
                mINTEGERS(); 

                }
                break;
            case 12 :
                // Bst.g:1:63: FUNCTION
                {
                mFUNCTION(); 

                }
                break;
            case 13 :
                // Bst.g:1:72: EXECUTE
                {
                mEXECUTE(); 

                }
                break;
            case 14 :
                // Bst.g:1:80: SORT
                {
                mSORT(); 

                }
                break;
            case 15 :
                // Bst.g:1:85: ITERATE
                {
                mITERATE(); 

                }
                break;
            case 16 :
                // Bst.g:1:93: REVERSE
                {
                mREVERSE(); 

                }
                break;
            case 17 :
                // Bst.g:1:101: ENTRY
                {
                mENTRY(); 

                }
                break;
            case 18 :
                // Bst.g:1:107: READ
                {
                mREAD(); 

                }
                break;
            case 19 :
                // Bst.g:1:112: MACRO
                {
                mMACRO(); 

                }
                break;
            case 20 :
                // Bst.g:1:118: QUOTED
                {
                mQUOTED(); 

                }
                break;
            case 21 :
                // Bst.g:1:125: IDENTIFIER
                {
                mIDENTIFIER(); 

                }
                break;
            case 22 :
                // Bst.g:1:136: STRING
                {
                mSTRING(); 

                }
                break;
            case 23 :
                // Bst.g:1:143: INTEGER
                {
                mINTEGER(); 

                }
                break;
            case 24 :
                // Bst.g:1:151: WS
                {
                mWS(); 

                }
                break;
            case 25 :
                // Bst.g:1:154: LINE_COMMENT
                {
                mLINE_COMMENT(); 

                }
                break;

        }

    }


 

}
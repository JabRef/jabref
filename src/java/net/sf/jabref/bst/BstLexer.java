package net.sf.jabref.bst;
// $ANTLR 3.0b4 bst.g 2006-09-17 01:45:41

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class BstLexer extends Lexer {
    public static final int T29=29;
    public static final int ENTRY=6;
    public static final int INTEGERS=9;
    public static final int T36=36;
    public static final int T58=58;
    public static final int COMMANDS=7;
    public static final int T35=35;
    public static final int EXECUTE=14;
    public static final int T61=61;
    public static final int T45=45;
    public static final int T34=34;
    public static final int INTEGER=19;
    public static final int T25=25;
    public static final int T37=37;
    public static final int FUNCTION=10;
    public static final int T26=26;
    public static final int T32=32;
    public static final int T51=51;
    public static final int STRINGS=8;
    public static final int T46=46;
    public static final int T38=38;
    public static final int MACRO=11;
    public static final int T41=41;
    public static final int IDLIST=4;
    public static final int NUMERAL=22;
    public static final int T39=39;
    public static final int T62=62;
    public static final int T44=44;
    public static final int T55=55;
    public static final int LETTER=21;
    public static final int T33=33;
    public static final int T50=50;
    public static final int WS=23;
    public static final int STRING=12;
    public static final int T43=43;
    public static final int T28=28;
    public static final int T42=42;
    public static final int T40=40;
    public static final int T63=63;
    public static final int T57=57;
    public static final int LINE_COMMENT=24;
    public static final int SORT=17;
    public static final int T56=56;
    public static final int STACK=5;
    public static final int REVERSE=16;
    public static final int QUOTED=20;
    public static final int T59=59;
    public static final int ITERATE=15;
    public static final int T48=48;
    public static final int T54=54;
    public static final int EOF=-1;
    public static final int T47=47;
    public static final int Tokens=64;
    public static final int T53=53;
    public static final int T60=60;
    public static final int T31=31;
    public static final int T49=49;
    public static final int IDENTIFIER=18;
    public static final int T27=27;
    public static final int T52=52;
    public static final int T30=30;
    public static final int READ=13;
    public BstLexer() {;} 
    public BstLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "bst.g"; }

    // $ANTLR start T25
    public void mT25() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T25;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:3:7: ( '{' )
            // bst.g:3:7: '{'
            {
            match('{'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T26;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:4:7: ( '}' )
            // bst.g:4:7: '}'
            {
            match('}'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T27;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:5:7: ( '<' )
            // bst.g:5:7: '<'
            {
            match('<'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T28;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:6:7: ( '>' )
            // bst.g:6:7: '>'
            {
            match('>'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T29;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:7:7: ( '=' )
            // bst.g:7:7: '='
            {
            match('='); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T30;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:8:7: ( '+' )
            // bst.g:8:7: '+'
            {
            match('+'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T31;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:9:7: ( '-' )
            // bst.g:9:7: '-'
            {
            match('-'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T32;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:10:7: ( ':=' )
            // bst.g:10:7: ':='
            {
            match(":="); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = T33;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:11:7: ( '*' )
            // bst.g:11:7: '*'
            {
            match('*'); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T33

    // $ANTLR start T34
    public void mT34() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T34;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:12:7: ( 'add.period$' )
            // bst.g:12:7: 'add.period$'
            {
            match("add.period$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T34

    // $ANTLR start T35
    public void mT35() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T35;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:13:7: ( 'call.type$' )
            // bst.g:13:7: 'call.type$'
            {
            match("call.type$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T35

    // $ANTLR start T36
    public void mT36() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T36;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:14:7: ( 'change.case$' )
            // bst.g:14:7: 'change.case$'
            {
            match("change.case$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T36

    // $ANTLR start T37
    public void mT37() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T37;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:15:7: ( 'chr.to.int$' )
            // bst.g:15:7: 'chr.to.int$'
            {
            match("chr.to.int$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T37

    // $ANTLR start T38
    public void mT38() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T38;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:16:7: ( 'cite$' )
            // bst.g:16:7: 'cite$'
            {
            match("cite$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T38

    // $ANTLR start T39
    public void mT39() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T39;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:17:7: ( 'duplicat$' )
            // bst.g:17:7: 'duplicat$'
            {
            match("duplicat$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T39

    // $ANTLR start T40
    public void mT40() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T40;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:18:7: ( 'empty$' )
            // bst.g:18:7: 'empty$'
            {
            match("empty$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T40

    // $ANTLR start T41
    public void mT41() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T41;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:19:7: ( 'format.name$' )
            // bst.g:19:7: 'format.name$'
            {
            match("format.name$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T41

    // $ANTLR start T42
    public void mT42() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T42;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:20:7: ( 'if$' )
            // bst.g:20:7: 'if$'
            {
            match("if$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T42

    // $ANTLR start T43
    public void mT43() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T43;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:21:7: ( 'int.to.chr$' )
            // bst.g:21:7: 'int.to.chr$'
            {
            match("int.to.chr$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T43

    // $ANTLR start T44
    public void mT44() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T44;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:22:7: ( 'int.to.str$' )
            // bst.g:22:7: 'int.to.str$'
            {
            match("int.to.str$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T44

    // $ANTLR start T45
    public void mT45() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T45;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:23:7: ( 'missing$' )
            // bst.g:23:7: 'missing$'
            {
            match("missing$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T45

    // $ANTLR start T46
    public void mT46() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T46;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:24:7: ( 'newline$' )
            // bst.g:24:7: 'newline$'
            {
            match("newline$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T46

    // $ANTLR start T47
    public void mT47() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T47;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:25:7: ( 'num.names$' )
            // bst.g:25:7: 'num.names$'
            {
            match("num.names$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T47

    // $ANTLR start T48
    public void mT48() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T48;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:26:7: ( 'pop$' )
            // bst.g:26:7: 'pop$'
            {
            match("pop$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T48

    // $ANTLR start T49
    public void mT49() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T49;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:27:7: ( 'preamble$' )
            // bst.g:27:7: 'preamble$'
            {
            match("preamble$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T49

    // $ANTLR start T50
    public void mT50() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T50;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:28:7: ( 'purify$' )
            // bst.g:28:7: 'purify$'
            {
            match("purify$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T50

    // $ANTLR start T51
    public void mT51() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T51;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:29:7: ( 'quote$' )
            // bst.g:29:7: 'quote$'
            {
            match("quote$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T51

    // $ANTLR start T52
    public void mT52() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T52;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:30:7: ( 'skip$' )
            // bst.g:30:7: 'skip$'
            {
            match("skip$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T52

    // $ANTLR start T53
    public void mT53() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T53;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:31:7: ( 'stack$' )
            // bst.g:31:7: 'stack$'
            {
            match("stack$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T53

    // $ANTLR start T54
    public void mT54() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T54;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:32:7: ( 'substring$' )
            // bst.g:32:7: 'substring$'
            {
            match("substring$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T54

    // $ANTLR start T55
    public void mT55() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T55;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:33:7: ( 'swap$' )
            // bst.g:33:7: 'swap$'
            {
            match("swap$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T55

    // $ANTLR start T56
    public void mT56() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T56;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:34:7: ( 'text.length$' )
            // bst.g:34:7: 'text.length$'
            {
            match("text.length$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T56

    // $ANTLR start T57
    public void mT57() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T57;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:35:7: ( 'text.prefix$' )
            // bst.g:35:7: 'text.prefix$'
            {
            match("text.prefix$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T57

    // $ANTLR start T58
    public void mT58() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T58;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:36:7: ( 'top$' )
            // bst.g:36:7: 'top$'
            {
            match("top$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T58

    // $ANTLR start T59
    public void mT59() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T59;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:37:7: ( 'type$' )
            // bst.g:37:7: 'type$'
            {
            match("type$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T59

    // $ANTLR start T60
    public void mT60() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T60;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:38:7: ( 'warning$' )
            // bst.g:38:7: 'warning$'
            {
            match("warning$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T60

    // $ANTLR start T61
    public void mT61() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T61;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:39:7: ( 'while$' )
            // bst.g:39:7: 'while$'
            {
            match("while$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T61

    // $ANTLR start T62
    public void mT62() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T62;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:40:7: ( 'width$' )
            // bst.g:40:7: 'width$'
            {
            match("width$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T62

    // $ANTLR start T63
    public void mT63() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = T63;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:41:7: ( 'write$' )
            // bst.g:41:7: 'write$'
            {
            match("write$"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end T63

    // $ANTLR start STRINGS
    public void mSTRINGS() throws RecognitionException {
        try {
            ruleNestingLevel++;
            int type = STRINGS;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:58:11: ( 'STRINGS' )
            // bst.g:58:11: 'STRINGS'
            {
            match("STRINGS"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = INTEGERS;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:59:12: ( 'INTEGERS' )
            // bst.g:59:12: 'INTEGERS'
            {
            match("INTEGERS"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = FUNCTION;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:60:12: ( 'FUNCTION' )
            // bst.g:60:12: 'FUNCTION'
            {
            match("FUNCTION"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = EXECUTE;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:61:11: ( 'EXECUTE' )
            // bst.g:61:11: 'EXECUTE'
            {
            match("EXECUTE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = SORT;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:62:8: ( 'SORT' )
            // bst.g:62:8: 'SORT'
            {
            match("SORT"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = ITERATE;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:63:11: ( 'ITERATE' )
            // bst.g:63:11: 'ITERATE'
            {
            match("ITERATE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = REVERSE;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:64:11: ( 'REVERSE' )
            // bst.g:64:11: 'REVERSE'
            {
            match("REVERSE"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = ENTRY;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:65:9: ( 'ENTRY' )
            // bst.g:65:9: 'ENTRY'
            {
            match("ENTRY"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = READ;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:66:8: ( 'READ' )
            // bst.g:66:8: 'READ'
            {
            match("READ"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = MACRO;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:67:9: ( 'MACRO' )
            // bst.g:67:9: 'MACRO'
            {
            match("MACRO"); 


            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = QUOTED;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:70:4: ( '\\'' IDENTIFIER )
            // bst.g:70:4: '\\'' IDENTIFIER
            {
            match('\''); 
            mIDENTIFIER(); 

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = IDENTIFIER;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:73:4: ( LETTER ( LETTER | NUMERAL )* )
            // bst.g:73:4: LETTER ( LETTER | NUMERAL )*
            {
            mLETTER(); 
            // bst.g:73:11: ( LETTER | NUMERAL )*
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
            	    // bst.g:73:12: LETTER
            	    {
            	    mLETTER(); 

            	    }
            	    break;
            	case 2 :
            	    // bst.g:73:19: NUMERAL
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
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            // bst.g:76:4: ( ('a'..'z'|'A'..'Z'|'.'|'$'))
            // bst.g:76:4: ('a'..'z'|'A'..'Z'|'.'|'$')
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
            int type = STRING;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:79:4: ( '\"' (~ '\"' )* '\"' )
            // bst.g:79:4: '\"' (~ '\"' )* '\"'
            {
            match('\"'); 
            // bst.g:79:8: (~ '\"' )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);
                if ( ((LA2_0>='\u0000' && LA2_0<='!')||(LA2_0>='#' && LA2_0<='\uFFFE')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // bst.g:79:9: ~ '\"'
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
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = INTEGER;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:82:4: ( '#' ( ('+'|'-'))? ( NUMERAL )+ )
            // bst.g:82:4: '#' ( ('+'|'-'))? ( NUMERAL )+
            {
            match('#'); 
            // bst.g:82:8: ( ('+'|'-'))?
            int alt3=2;
            int LA3_0 = input.LA(1);
            if ( (LA3_0=='+'||LA3_0=='-') ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // bst.g:82:9: ('+'|'-')
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

            // bst.g:82:19: ( NUMERAL )+
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
            	    // bst.g:82:19: NUMERAL
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
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            // bst.g:85:4: ( ( '0' .. '9' ) )
            // bst.g:85:4: ( '0' .. '9' )
            {
            // bst.g:85:4: ( '0' .. '9' )
            // bst.g:85:5: '0' .. '9'
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
            int type = WS;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:88:4: ( ( (' '|'\\t'|'\\n'))+ )
            // bst.g:88:4: ( (' '|'\\t'|'\\n'))+
            {
            // bst.g:88:4: ( (' '|'\\t'|'\\n'))+
            int cnt5=0;
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);
                if ( ((LA5_0>='\t' && LA5_0<='\n')||LA5_0==' ') ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // bst.g:88:5: (' '|'\\t'|'\\n')
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)==' ' ) {
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

            channel=99;

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
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
            int type = LINE_COMMENT;
            int start = getCharIndex();
            int line = getLine();
            int charPosition = getCharPositionInLine();
            int channel = Token.DEFAULT_CHANNEL;
            // bst.g:91:7: ( '%' (~ ('\\n'|'\\r'))* ( '\\r' )? '\\n' )
            // bst.g:91:7: '%' (~ ('\\n'|'\\r'))* ( '\\r' )? '\\n'
            {
            match('%'); 
            // bst.g:91:11: (~ ('\\n'|'\\r'))*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);
                if ( ((LA6_0>='\u0000' && LA6_0<='\t')||(LA6_0>='\u000B' && LA6_0<='\f')||(LA6_0>='\u000E' && LA6_0<='\uFFFE')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // bst.g:91:11: ~ ('\\n'|'\\r')
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

            // bst.g:91:25: ( '\\r' )?
            int alt7=2;
            int LA7_0 = input.LA(1);
            if ( (LA7_0=='\r') ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // bst.g:91:25: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 
            channel=99;

            }



                    if ( token==null && ruleNestingLevel==1 ) {
                        emit(type,line,charPosition,channel,start,getCharIndex()-1);
                    }

                        }
        finally {
            ruleNestingLevel--;
        }
    }
    // $ANTLR end LINE_COMMENT

    public void mTokens() throws RecognitionException {
        // bst.g:1:10: ( T25 | T26 | T27 | T28 | T29 | T30 | T31 | T32 | T33 | T34 | T35 | T36 | T37 | T38 | T39 | T40 | T41 | T42 | T43 | T44 | T45 | T46 | T47 | T48 | T49 | T50 | T51 | T52 | T53 | T54 | T55 | T56 | T57 | T58 | T59 | T60 | T61 | T62 | T63 | STRINGS | INTEGERS | FUNCTION | EXECUTE | SORT | ITERATE | REVERSE | ENTRY | READ | MACRO | QUOTED | IDENTIFIER | STRING | INTEGER | WS | LINE_COMMENT )
        int alt8=55;
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
        case 'a':
            int LA8_10 = input.LA(2);
            if ( (LA8_10=='d') ) {
                int LA8_35 = input.LA(3);
                if ( (LA8_35=='d') ) {
                    int LA8_71 = input.LA(4);
                    if ( (LA8_71=='.') ) {
                        int LA8_109 = input.LA(5);
                        if ( (LA8_109=='p') ) {
                            int LA8_147 = input.LA(6);
                            if ( (LA8_147=='e') ) {
                                int LA8_184 = input.LA(7);
                                if ( (LA8_184=='r') ) {
                                    int LA8_218 = input.LA(8);
                                    if ( (LA8_218=='i') ) {
                                        int LA8_246 = input.LA(9);
                                        if ( (LA8_246=='o') ) {
                                            int LA8_269 = input.LA(10);
                                            if ( (LA8_269=='d') ) {
                                                int LA8_287 = input.LA(11);
                                                if ( (LA8_287=='$') ) {
                                                    int LA8_300 = input.LA(12);
                                                    if ( (LA8_300=='$'||LA8_300=='.'||(LA8_300>='0' && LA8_300<='9')||(LA8_300>='A' && LA8_300<='Z')||(LA8_300>='a' && LA8_300<='z')) ) {
                                                        alt8=51;
                                                    }
                                                    else {
                                                        alt8=10;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'c':
            switch ( input.LA(2) ) {
            case 'h':
                switch ( input.LA(3) ) {
                case 'a':
                    int LA8_72 = input.LA(4);
                    if ( (LA8_72=='n') ) {
                        int LA8_110 = input.LA(5);
                        if ( (LA8_110=='g') ) {
                            int LA8_148 = input.LA(6);
                            if ( (LA8_148=='e') ) {
                                int LA8_185 = input.LA(7);
                                if ( (LA8_185=='.') ) {
                                    int LA8_219 = input.LA(8);
                                    if ( (LA8_219=='c') ) {
                                        int LA8_247 = input.LA(9);
                                        if ( (LA8_247=='a') ) {
                                            int LA8_270 = input.LA(10);
                                            if ( (LA8_270=='s') ) {
                                                int LA8_288 = input.LA(11);
                                                if ( (LA8_288=='e') ) {
                                                    int LA8_301 = input.LA(12);
                                                    if ( (LA8_301=='$') ) {
                                                        int LA8_312 = input.LA(13);
                                                        if ( (LA8_312=='$'||LA8_312=='.'||(LA8_312>='0' && LA8_312<='9')||(LA8_312>='A' && LA8_312<='Z')||(LA8_312>='a' && LA8_312<='z')) ) {
                                                            alt8=51;
                                                        }
                                                        else {
                                                            alt8=12;}
                                                    }
                                                    else {
                                                        alt8=51;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                    break;
                case 'r':
                    int LA8_73 = input.LA(4);
                    if ( (LA8_73=='.') ) {
                        int LA8_111 = input.LA(5);
                        if ( (LA8_111=='t') ) {
                            int LA8_149 = input.LA(6);
                            if ( (LA8_149=='o') ) {
                                int LA8_186 = input.LA(7);
                                if ( (LA8_186=='.') ) {
                                    int LA8_220 = input.LA(8);
                                    if ( (LA8_220=='i') ) {
                                        int LA8_248 = input.LA(9);
                                        if ( (LA8_248=='n') ) {
                                            int LA8_271 = input.LA(10);
                                            if ( (LA8_271=='t') ) {
                                                int LA8_289 = input.LA(11);
                                                if ( (LA8_289=='$') ) {
                                                    int LA8_302 = input.LA(12);
                                                    if ( (LA8_302=='$'||LA8_302=='.'||(LA8_302>='0' && LA8_302<='9')||(LA8_302>='A' && LA8_302<='Z')||(LA8_302>='a' && LA8_302<='z')) ) {
                                                        alt8=51;
                                                    }
                                                    else {
                                                        alt8=13;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                    break;
                default:
                    alt8=51;}

                break;
            case 'i':
                int LA8_37 = input.LA(3);
                if ( (LA8_37=='t') ) {
                    int LA8_74 = input.LA(4);
                    if ( (LA8_74=='e') ) {
                        int LA8_112 = input.LA(5);
                        if ( (LA8_112=='$') ) {
                            int LA8_150 = input.LA(6);
                            if ( (LA8_150=='$'||LA8_150=='.'||(LA8_150>='0' && LA8_150<='9')||(LA8_150>='A' && LA8_150<='Z')||(LA8_150>='a' && LA8_150<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=14;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'a':
                int LA8_38 = input.LA(3);
                if ( (LA8_38=='l') ) {
                    int LA8_75 = input.LA(4);
                    if ( (LA8_75=='l') ) {
                        int LA8_113 = input.LA(5);
                        if ( (LA8_113=='.') ) {
                            int LA8_151 = input.LA(6);
                            if ( (LA8_151=='t') ) {
                                int LA8_188 = input.LA(7);
                                if ( (LA8_188=='y') ) {
                                    int LA8_221 = input.LA(8);
                                    if ( (LA8_221=='p') ) {
                                        int LA8_249 = input.LA(9);
                                        if ( (LA8_249=='e') ) {
                                            int LA8_272 = input.LA(10);
                                            if ( (LA8_272=='$') ) {
                                                int LA8_290 = input.LA(11);
                                                if ( (LA8_290=='$'||LA8_290=='.'||(LA8_290>='0' && LA8_290<='9')||(LA8_290>='A' && LA8_290<='Z')||(LA8_290>='a' && LA8_290<='z')) ) {
                                                    alt8=51;
                                                }
                                                else {
                                                    alt8=11;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'd':
            int LA8_12 = input.LA(2);
            if ( (LA8_12=='u') ) {
                int LA8_39 = input.LA(3);
                if ( (LA8_39=='p') ) {
                    int LA8_76 = input.LA(4);
                    if ( (LA8_76=='l') ) {
                        int LA8_114 = input.LA(5);
                        if ( (LA8_114=='i') ) {
                            int LA8_152 = input.LA(6);
                            if ( (LA8_152=='c') ) {
                                int LA8_189 = input.LA(7);
                                if ( (LA8_189=='a') ) {
                                    int LA8_222 = input.LA(8);
                                    if ( (LA8_222=='t') ) {
                                        int LA8_250 = input.LA(9);
                                        if ( (LA8_250=='$') ) {
                                            int LA8_273 = input.LA(10);
                                            if ( (LA8_273=='$'||LA8_273=='.'||(LA8_273>='0' && LA8_273<='9')||(LA8_273>='A' && LA8_273<='Z')||(LA8_273>='a' && LA8_273<='z')) ) {
                                                alt8=51;
                                            }
                                            else {
                                                alt8=15;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'e':
            int LA8_13 = input.LA(2);
            if ( (LA8_13=='m') ) {
                int LA8_40 = input.LA(3);
                if ( (LA8_40=='p') ) {
                    int LA8_77 = input.LA(4);
                    if ( (LA8_77=='t') ) {
                        int LA8_115 = input.LA(5);
                        if ( (LA8_115=='y') ) {
                            int LA8_153 = input.LA(6);
                            if ( (LA8_153=='$') ) {
                                int LA8_190 = input.LA(7);
                                if ( (LA8_190=='$'||LA8_190=='.'||(LA8_190>='0' && LA8_190<='9')||(LA8_190>='A' && LA8_190<='Z')||(LA8_190>='a' && LA8_190<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=16;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'f':
            int LA8_14 = input.LA(2);
            if ( (LA8_14=='o') ) {
                int LA8_41 = input.LA(3);
                if ( (LA8_41=='r') ) {
                    int LA8_78 = input.LA(4);
                    if ( (LA8_78=='m') ) {
                        int LA8_116 = input.LA(5);
                        if ( (LA8_116=='a') ) {
                            int LA8_154 = input.LA(6);
                            if ( (LA8_154=='t') ) {
                                int LA8_191 = input.LA(7);
                                if ( (LA8_191=='.') ) {
                                    int LA8_224 = input.LA(8);
                                    if ( (LA8_224=='n') ) {
                                        int LA8_251 = input.LA(9);
                                        if ( (LA8_251=='a') ) {
                                            int LA8_274 = input.LA(10);
                                            if ( (LA8_274=='m') ) {
                                                int LA8_292 = input.LA(11);
                                                if ( (LA8_292=='e') ) {
                                                    int LA8_304 = input.LA(12);
                                                    if ( (LA8_304=='$') ) {
                                                        int LA8_314 = input.LA(13);
                                                        if ( (LA8_314=='$'||LA8_314=='.'||(LA8_314>='0' && LA8_314<='9')||(LA8_314>='A' && LA8_314<='Z')||(LA8_314>='a' && LA8_314<='z')) ) {
                                                            alt8=51;
                                                        }
                                                        else {
                                                            alt8=17;}
                                                    }
                                                    else {
                                                        alt8=51;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'i':
            switch ( input.LA(2) ) {
            case 'f':
                int LA8_42 = input.LA(3);
                if ( (LA8_42=='$') ) {
                    int LA8_79 = input.LA(4);
                    if ( (LA8_79=='$'||LA8_79=='.'||(LA8_79>='0' && LA8_79<='9')||(LA8_79>='A' && LA8_79<='Z')||(LA8_79>='a' && LA8_79<='z')) ) {
                        alt8=51;
                    }
                    else {
                        alt8=18;}
                }
                else {
                    alt8=51;}
                break;
            case 'n':
                int LA8_43 = input.LA(3);
                if ( (LA8_43=='t') ) {
                    int LA8_80 = input.LA(4);
                    if ( (LA8_80=='.') ) {
                        int LA8_118 = input.LA(5);
                        if ( (LA8_118=='t') ) {
                            int LA8_155 = input.LA(6);
                            if ( (LA8_155=='o') ) {
                                int LA8_192 = input.LA(7);
                                if ( (LA8_192=='.') ) {
                                    switch ( input.LA(8) ) {
                                    case 'c':
                                        int LA8_252 = input.LA(9);
                                        if ( (LA8_252=='h') ) {
                                            int LA8_275 = input.LA(10);
                                            if ( (LA8_275=='r') ) {
                                                int LA8_293 = input.LA(11);
                                                if ( (LA8_293=='$') ) {
                                                    int LA8_305 = input.LA(12);
                                                    if ( (LA8_305=='$'||LA8_305=='.'||(LA8_305>='0' && LA8_305<='9')||(LA8_305>='A' && LA8_305<='Z')||(LA8_305>='a' && LA8_305<='z')) ) {
                                                        alt8=51;
                                                    }
                                                    else {
                                                        alt8=19;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                        break;
                                    case 's':
                                        int LA8_253 = input.LA(9);
                                        if ( (LA8_253=='t') ) {
                                            int LA8_276 = input.LA(10);
                                            if ( (LA8_276=='r') ) {
                                                int LA8_294 = input.LA(11);
                                                if ( (LA8_294=='$') ) {
                                                    int LA8_306 = input.LA(12);
                                                    if ( (LA8_306=='$'||LA8_306=='.'||(LA8_306>='0' && LA8_306<='9')||(LA8_306>='A' && LA8_306<='Z')||(LA8_306>='a' && LA8_306<='z')) ) {
                                                        alt8=51;
                                                    }
                                                    else {
                                                        alt8=20;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                        break;
                                    default:
                                        alt8=51;}

                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'm':
            int LA8_16 = input.LA(2);
            if ( (LA8_16=='i') ) {
                int LA8_44 = input.LA(3);
                if ( (LA8_44=='s') ) {
                    int LA8_81 = input.LA(4);
                    if ( (LA8_81=='s') ) {
                        int LA8_119 = input.LA(5);
                        if ( (LA8_119=='i') ) {
                            int LA8_156 = input.LA(6);
                            if ( (LA8_156=='n') ) {
                                int LA8_193 = input.LA(7);
                                if ( (LA8_193=='g') ) {
                                    int LA8_226 = input.LA(8);
                                    if ( (LA8_226=='$') ) {
                                        int LA8_254 = input.LA(9);
                                        if ( (LA8_254=='$'||LA8_254=='.'||(LA8_254>='0' && LA8_254<='9')||(LA8_254>='A' && LA8_254<='Z')||(LA8_254>='a' && LA8_254<='z')) ) {
                                            alt8=51;
                                        }
                                        else {
                                            alt8=21;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'n':
            switch ( input.LA(2) ) {
            case 'u':
                int LA8_45 = input.LA(3);
                if ( (LA8_45=='m') ) {
                    int LA8_82 = input.LA(4);
                    if ( (LA8_82=='.') ) {
                        int LA8_120 = input.LA(5);
                        if ( (LA8_120=='n') ) {
                            int LA8_157 = input.LA(6);
                            if ( (LA8_157=='a') ) {
                                int LA8_194 = input.LA(7);
                                if ( (LA8_194=='m') ) {
                                    int LA8_227 = input.LA(8);
                                    if ( (LA8_227=='e') ) {
                                        int LA8_255 = input.LA(9);
                                        if ( (LA8_255=='s') ) {
                                            int LA8_278 = input.LA(10);
                                            if ( (LA8_278=='$') ) {
                                                int LA8_295 = input.LA(11);
                                                if ( (LA8_295=='$'||LA8_295=='.'||(LA8_295>='0' && LA8_295<='9')||(LA8_295>='A' && LA8_295<='Z')||(LA8_295>='a' && LA8_295<='z')) ) {
                                                    alt8=51;
                                                }
                                                else {
                                                    alt8=23;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'e':
                int LA8_46 = input.LA(3);
                if ( (LA8_46=='w') ) {
                    int LA8_83 = input.LA(4);
                    if ( (LA8_83=='l') ) {
                        int LA8_121 = input.LA(5);
                        if ( (LA8_121=='i') ) {
                            int LA8_158 = input.LA(6);
                            if ( (LA8_158=='n') ) {
                                int LA8_195 = input.LA(7);
                                if ( (LA8_195=='e') ) {
                                    int LA8_228 = input.LA(8);
                                    if ( (LA8_228=='$') ) {
                                        int LA8_256 = input.LA(9);
                                        if ( (LA8_256=='$'||LA8_256=='.'||(LA8_256>='0' && LA8_256<='9')||(LA8_256>='A' && LA8_256<='Z')||(LA8_256>='a' && LA8_256<='z')) ) {
                                            alt8=51;
                                        }
                                        else {
                                            alt8=22;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'p':
            switch ( input.LA(2) ) {
            case 'u':
                int LA8_47 = input.LA(3);
                if ( (LA8_47=='r') ) {
                    int LA8_84 = input.LA(4);
                    if ( (LA8_84=='i') ) {
                        int LA8_122 = input.LA(5);
                        if ( (LA8_122=='f') ) {
                            int LA8_159 = input.LA(6);
                            if ( (LA8_159=='y') ) {
                                int LA8_196 = input.LA(7);
                                if ( (LA8_196=='$') ) {
                                    int LA8_229 = input.LA(8);
                                    if ( (LA8_229=='$'||LA8_229=='.'||(LA8_229>='0' && LA8_229<='9')||(LA8_229>='A' && LA8_229<='Z')||(LA8_229>='a' && LA8_229<='z')) ) {
                                        alt8=51;
                                    }
                                    else {
                                        alt8=26;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'o':
                int LA8_48 = input.LA(3);
                if ( (LA8_48=='p') ) {
                    int LA8_85 = input.LA(4);
                    if ( (LA8_85=='$') ) {
                        int LA8_123 = input.LA(5);
                        if ( (LA8_123=='$'||LA8_123=='.'||(LA8_123>='0' && LA8_123<='9')||(LA8_123>='A' && LA8_123<='Z')||(LA8_123>='a' && LA8_123<='z')) ) {
                            alt8=51;
                        }
                        else {
                            alt8=24;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'r':
                int LA8_49 = input.LA(3);
                if ( (LA8_49=='e') ) {
                    int LA8_86 = input.LA(4);
                    if ( (LA8_86=='a') ) {
                        int LA8_124 = input.LA(5);
                        if ( (LA8_124=='m') ) {
                            int LA8_161 = input.LA(6);
                            if ( (LA8_161=='b') ) {
                                int LA8_197 = input.LA(7);
                                if ( (LA8_197=='l') ) {
                                    int LA8_230 = input.LA(8);
                                    if ( (LA8_230=='e') ) {
                                        int LA8_258 = input.LA(9);
                                        if ( (LA8_258=='$') ) {
                                            int LA8_280 = input.LA(10);
                                            if ( (LA8_280=='$'||LA8_280=='.'||(LA8_280>='0' && LA8_280<='9')||(LA8_280>='A' && LA8_280<='Z')||(LA8_280>='a' && LA8_280<='z')) ) {
                                                alt8=51;
                                            }
                                            else {
                                                alt8=25;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'q':
            int LA8_19 = input.LA(2);
            if ( (LA8_19=='u') ) {
                int LA8_50 = input.LA(3);
                if ( (LA8_50=='o') ) {
                    int LA8_87 = input.LA(4);
                    if ( (LA8_87=='t') ) {
                        int LA8_125 = input.LA(5);
                        if ( (LA8_125=='e') ) {
                            int LA8_162 = input.LA(6);
                            if ( (LA8_162=='$') ) {
                                int LA8_198 = input.LA(7);
                                if ( (LA8_198=='$'||LA8_198=='.'||(LA8_198>='0' && LA8_198<='9')||(LA8_198>='A' && LA8_198<='Z')||(LA8_198>='a' && LA8_198<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=27;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 's':
            switch ( input.LA(2) ) {
            case 'w':
                int LA8_51 = input.LA(3);
                if ( (LA8_51=='a') ) {
                    int LA8_88 = input.LA(4);
                    if ( (LA8_88=='p') ) {
                        int LA8_126 = input.LA(5);
                        if ( (LA8_126=='$') ) {
                            int LA8_163 = input.LA(6);
                            if ( (LA8_163=='$'||LA8_163=='.'||(LA8_163>='0' && LA8_163<='9')||(LA8_163>='A' && LA8_163<='Z')||(LA8_163>='a' && LA8_163<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=31;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'k':
                int LA8_52 = input.LA(3);
                if ( (LA8_52=='i') ) {
                    int LA8_89 = input.LA(4);
                    if ( (LA8_89=='p') ) {
                        int LA8_127 = input.LA(5);
                        if ( (LA8_127=='$') ) {
                            int LA8_164 = input.LA(6);
                            if ( (LA8_164=='$'||LA8_164=='.'||(LA8_164>='0' && LA8_164<='9')||(LA8_164>='A' && LA8_164<='Z')||(LA8_164>='a' && LA8_164<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=28;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 't':
                int LA8_53 = input.LA(3);
                if ( (LA8_53=='a') ) {
                    int LA8_90 = input.LA(4);
                    if ( (LA8_90=='c') ) {
                        int LA8_128 = input.LA(5);
                        if ( (LA8_128=='k') ) {
                            int LA8_165 = input.LA(6);
                            if ( (LA8_165=='$') ) {
                                int LA8_201 = input.LA(7);
                                if ( (LA8_201=='$'||LA8_201=='.'||(LA8_201>='0' && LA8_201<='9')||(LA8_201>='A' && LA8_201<='Z')||(LA8_201>='a' && LA8_201<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=29;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'u':
                int LA8_54 = input.LA(3);
                if ( (LA8_54=='b') ) {
                    int LA8_91 = input.LA(4);
                    if ( (LA8_91=='s') ) {
                        int LA8_129 = input.LA(5);
                        if ( (LA8_129=='t') ) {
                            int LA8_166 = input.LA(6);
                            if ( (LA8_166=='r') ) {
                                int LA8_202 = input.LA(7);
                                if ( (LA8_202=='i') ) {
                                    int LA8_233 = input.LA(8);
                                    if ( (LA8_233=='n') ) {
                                        int LA8_259 = input.LA(9);
                                        if ( (LA8_259=='g') ) {
                                            int LA8_281 = input.LA(10);
                                            if ( (LA8_281=='$') ) {
                                                int LA8_297 = input.LA(11);
                                                if ( (LA8_297=='$'||LA8_297=='.'||(LA8_297>='0' && LA8_297<='9')||(LA8_297>='A' && LA8_297<='Z')||(LA8_297>='a' && LA8_297<='z')) ) {
                                                    alt8=51;
                                                }
                                                else {
                                                    alt8=30;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 't':
            switch ( input.LA(2) ) {
            case 'y':
                int LA8_55 = input.LA(3);
                if ( (LA8_55=='p') ) {
                    int LA8_92 = input.LA(4);
                    if ( (LA8_92=='e') ) {
                        int LA8_130 = input.LA(5);
                        if ( (LA8_130=='$') ) {
                            int LA8_167 = input.LA(6);
                            if ( (LA8_167=='$'||LA8_167=='.'||(LA8_167>='0' && LA8_167<='9')||(LA8_167>='A' && LA8_167<='Z')||(LA8_167>='a' && LA8_167<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=35;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'e':
                int LA8_56 = input.LA(3);
                if ( (LA8_56=='x') ) {
                    int LA8_93 = input.LA(4);
                    if ( (LA8_93=='t') ) {
                        int LA8_131 = input.LA(5);
                        if ( (LA8_131=='.') ) {
                            switch ( input.LA(6) ) {
                            case 'l':
                                int LA8_204 = input.LA(7);
                                if ( (LA8_204=='e') ) {
                                    int LA8_234 = input.LA(8);
                                    if ( (LA8_234=='n') ) {
                                        int LA8_260 = input.LA(9);
                                        if ( (LA8_260=='g') ) {
                                            int LA8_282 = input.LA(10);
                                            if ( (LA8_282=='t') ) {
                                                int LA8_298 = input.LA(11);
                                                if ( (LA8_298=='h') ) {
                                                    int LA8_309 = input.LA(12);
                                                    if ( (LA8_309=='$') ) {
                                                        int LA8_317 = input.LA(13);
                                                        if ( (LA8_317=='$'||LA8_317=='.'||(LA8_317>='0' && LA8_317<='9')||(LA8_317>='A' && LA8_317<='Z')||(LA8_317>='a' && LA8_317<='z')) ) {
                                                            alt8=51;
                                                        }
                                                        else {
                                                            alt8=32;}
                                                    }
                                                    else {
                                                        alt8=51;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                                break;
                            case 'p':
                                int LA8_205 = input.LA(7);
                                if ( (LA8_205=='r') ) {
                                    int LA8_235 = input.LA(8);
                                    if ( (LA8_235=='e') ) {
                                        int LA8_261 = input.LA(9);
                                        if ( (LA8_261=='f') ) {
                                            int LA8_283 = input.LA(10);
                                            if ( (LA8_283=='i') ) {
                                                int LA8_299 = input.LA(11);
                                                if ( (LA8_299=='x') ) {
                                                    int LA8_310 = input.LA(12);
                                                    if ( (LA8_310=='$') ) {
                                                        int LA8_318 = input.LA(13);
                                                        if ( (LA8_318=='$'||LA8_318=='.'||(LA8_318>='0' && LA8_318<='9')||(LA8_318>='A' && LA8_318<='Z')||(LA8_318>='a' && LA8_318<='z')) ) {
                                                            alt8=51;
                                                        }
                                                        else {
                                                            alt8=33;}
                                                    }
                                                    else {
                                                        alt8=51;}
                                                }
                                                else {
                                                    alt8=51;}
                                            }
                                            else {
                                                alt8=51;}
                                        }
                                        else {
                                            alt8=51;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                                break;
                            default:
                                alt8=51;}

                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'o':
                int LA8_57 = input.LA(3);
                if ( (LA8_57=='p') ) {
                    int LA8_94 = input.LA(4);
                    if ( (LA8_94=='$') ) {
                        int LA8_132 = input.LA(5);
                        if ( (LA8_132=='$'||LA8_132=='.'||(LA8_132>='0' && LA8_132<='9')||(LA8_132>='A' && LA8_132<='Z')||(LA8_132>='a' && LA8_132<='z')) ) {
                            alt8=51;
                        }
                        else {
                            alt8=34;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'w':
            switch ( input.LA(2) ) {
            case 'a':
                int LA8_58 = input.LA(3);
                if ( (LA8_58=='r') ) {
                    int LA8_95 = input.LA(4);
                    if ( (LA8_95=='n') ) {
                        int LA8_133 = input.LA(5);
                        if ( (LA8_133=='i') ) {
                            int LA8_170 = input.LA(6);
                            if ( (LA8_170=='n') ) {
                                int LA8_206 = input.LA(7);
                                if ( (LA8_206=='g') ) {
                                    int LA8_236 = input.LA(8);
                                    if ( (LA8_236=='$') ) {
                                        int LA8_262 = input.LA(9);
                                        if ( (LA8_262=='$'||LA8_262=='.'||(LA8_262>='0' && LA8_262<='9')||(LA8_262>='A' && LA8_262<='Z')||(LA8_262>='a' && LA8_262<='z')) ) {
                                            alt8=51;
                                        }
                                        else {
                                            alt8=36;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'h':
                int LA8_59 = input.LA(3);
                if ( (LA8_59=='i') ) {
                    int LA8_96 = input.LA(4);
                    if ( (LA8_96=='l') ) {
                        int LA8_134 = input.LA(5);
                        if ( (LA8_134=='e') ) {
                            int LA8_171 = input.LA(6);
                            if ( (LA8_171=='$') ) {
                                int LA8_207 = input.LA(7);
                                if ( (LA8_207=='$'||LA8_207=='.'||(LA8_207>='0' && LA8_207<='9')||(LA8_207>='A' && LA8_207<='Z')||(LA8_207>='a' && LA8_207<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=37;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'r':
                int LA8_60 = input.LA(3);
                if ( (LA8_60=='i') ) {
                    int LA8_97 = input.LA(4);
                    if ( (LA8_97=='t') ) {
                        int LA8_135 = input.LA(5);
                        if ( (LA8_135=='e') ) {
                            int LA8_172 = input.LA(6);
                            if ( (LA8_172=='$') ) {
                                int LA8_208 = input.LA(7);
                                if ( (LA8_208=='$'||LA8_208=='.'||(LA8_208>='0' && LA8_208<='9')||(LA8_208>='A' && LA8_208<='Z')||(LA8_208>='a' && LA8_208<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=39;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'i':
                int LA8_61 = input.LA(3);
                if ( (LA8_61=='d') ) {
                    int LA8_98 = input.LA(4);
                    if ( (LA8_98=='t') ) {
                        int LA8_136 = input.LA(5);
                        if ( (LA8_136=='h') ) {
                            int LA8_173 = input.LA(6);
                            if ( (LA8_173=='$') ) {
                                int LA8_209 = input.LA(7);
                                if ( (LA8_209=='$'||LA8_209=='.'||(LA8_209>='0' && LA8_209<='9')||(LA8_209>='A' && LA8_209<='Z')||(LA8_209>='a' && LA8_209<='z')) ) {
                                    alt8=51;
                                }
                                else {
                                    alt8=38;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'S':
            switch ( input.LA(2) ) {
            case 'O':
                int LA8_62 = input.LA(3);
                if ( (LA8_62=='R') ) {
                    int LA8_99 = input.LA(4);
                    if ( (LA8_99=='T') ) {
                        int LA8_137 = input.LA(5);
                        if ( (LA8_137=='$'||LA8_137=='.'||(LA8_137>='0' && LA8_137<='9')||(LA8_137>='A' && LA8_137<='Z')||(LA8_137>='a' && LA8_137<='z')) ) {
                            alt8=51;
                        }
                        else {
                            alt8=44;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'T':
                int LA8_63 = input.LA(3);
                if ( (LA8_63=='R') ) {
                    int LA8_100 = input.LA(4);
                    if ( (LA8_100=='I') ) {
                        int LA8_138 = input.LA(5);
                        if ( (LA8_138=='N') ) {
                            int LA8_175 = input.LA(6);
                            if ( (LA8_175=='G') ) {
                                int LA8_210 = input.LA(7);
                                if ( (LA8_210=='S') ) {
                                    int LA8_240 = input.LA(8);
                                    if ( (LA8_240=='$'||LA8_240=='.'||(LA8_240>='0' && LA8_240<='9')||(LA8_240>='A' && LA8_240<='Z')||(LA8_240>='a' && LA8_240<='z')) ) {
                                        alt8=51;
                                    }
                                    else {
                                        alt8=40;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'I':
            switch ( input.LA(2) ) {
            case 'T':
                int LA8_64 = input.LA(3);
                if ( (LA8_64=='E') ) {
                    int LA8_101 = input.LA(4);
                    if ( (LA8_101=='R') ) {
                        int LA8_139 = input.LA(5);
                        if ( (LA8_139=='A') ) {
                            int LA8_176 = input.LA(6);
                            if ( (LA8_176=='T') ) {
                                int LA8_211 = input.LA(7);
                                if ( (LA8_211=='E') ) {
                                    int LA8_241 = input.LA(8);
                                    if ( (LA8_241=='$'||LA8_241=='.'||(LA8_241>='0' && LA8_241<='9')||(LA8_241>='A' && LA8_241<='Z')||(LA8_241>='a' && LA8_241<='z')) ) {
                                        alt8=51;
                                    }
                                    else {
                                        alt8=45;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'N':
                int LA8_65 = input.LA(3);
                if ( (LA8_65=='T') ) {
                    int LA8_102 = input.LA(4);
                    if ( (LA8_102=='E') ) {
                        int LA8_140 = input.LA(5);
                        if ( (LA8_140=='G') ) {
                            int LA8_177 = input.LA(6);
                            if ( (LA8_177=='E') ) {
                                int LA8_212 = input.LA(7);
                                if ( (LA8_212=='R') ) {
                                    int LA8_242 = input.LA(8);
                                    if ( (LA8_242=='S') ) {
                                        int LA8_265 = input.LA(9);
                                        if ( (LA8_265=='$'||LA8_265=='.'||(LA8_265>='0' && LA8_265<='9')||(LA8_265>='A' && LA8_265<='Z')||(LA8_265>='a' && LA8_265<='z')) ) {
                                            alt8=51;
                                        }
                                        else {
                                            alt8=41;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'F':
            int LA8_25 = input.LA(2);
            if ( (LA8_25=='U') ) {
                int LA8_66 = input.LA(3);
                if ( (LA8_66=='N') ) {
                    int LA8_103 = input.LA(4);
                    if ( (LA8_103=='C') ) {
                        int LA8_141 = input.LA(5);
                        if ( (LA8_141=='T') ) {
                            int LA8_178 = input.LA(6);
                            if ( (LA8_178=='I') ) {
                                int LA8_213 = input.LA(7);
                                if ( (LA8_213=='O') ) {
                                    int LA8_243 = input.LA(8);
                                    if ( (LA8_243=='N') ) {
                                        int LA8_266 = input.LA(9);
                                        if ( (LA8_266=='$'||LA8_266=='.'||(LA8_266>='0' && LA8_266<='9')||(LA8_266>='A' && LA8_266<='Z')||(LA8_266>='a' && LA8_266<='z')) ) {
                                            alt8=51;
                                        }
                                        else {
                                            alt8=42;}
                                    }
                                    else {
                                        alt8=51;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case 'E':
            switch ( input.LA(2) ) {
            case 'N':
                int LA8_67 = input.LA(3);
                if ( (LA8_67=='T') ) {
                    int LA8_104 = input.LA(4);
                    if ( (LA8_104=='R') ) {
                        int LA8_142 = input.LA(5);
                        if ( (LA8_142=='Y') ) {
                            int LA8_179 = input.LA(6);
                            if ( (LA8_179=='$'||LA8_179=='.'||(LA8_179>='0' && LA8_179<='9')||(LA8_179>='A' && LA8_179<='Z')||(LA8_179>='a' && LA8_179<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=47;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            case 'X':
                int LA8_68 = input.LA(3);
                if ( (LA8_68=='E') ) {
                    int LA8_105 = input.LA(4);
                    if ( (LA8_105=='C') ) {
                        int LA8_143 = input.LA(5);
                        if ( (LA8_143=='U') ) {
                            int LA8_180 = input.LA(6);
                            if ( (LA8_180=='T') ) {
                                int LA8_215 = input.LA(7);
                                if ( (LA8_215=='E') ) {
                                    int LA8_244 = input.LA(8);
                                    if ( (LA8_244=='$'||LA8_244=='.'||(LA8_244>='0' && LA8_244<='9')||(LA8_244>='A' && LA8_244<='Z')||(LA8_244>='a' && LA8_244<='z')) ) {
                                        alt8=51;
                                    }
                                    else {
                                        alt8=43;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
                break;
            default:
                alt8=51;}

            break;
        case 'R':
            int LA8_27 = input.LA(2);
            if ( (LA8_27=='E') ) {
                switch ( input.LA(3) ) {
                case 'V':
                    int LA8_106 = input.LA(4);
                    if ( (LA8_106=='E') ) {
                        int LA8_144 = input.LA(5);
                        if ( (LA8_144=='R') ) {
                            int LA8_181 = input.LA(6);
                            if ( (LA8_181=='S') ) {
                                int LA8_216 = input.LA(7);
                                if ( (LA8_216=='E') ) {
                                    int LA8_245 = input.LA(8);
                                    if ( (LA8_245=='$'||LA8_245=='.'||(LA8_245>='0' && LA8_245<='9')||(LA8_245>='A' && LA8_245<='Z')||(LA8_245>='a' && LA8_245<='z')) ) {
                                        alt8=51;
                                    }
                                    else {
                                        alt8=46;}
                                }
                                else {
                                    alt8=51;}
                            }
                            else {
                                alt8=51;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                    break;
                case 'A':
                    int LA8_107 = input.LA(4);
                    if ( (LA8_107=='D') ) {
                        int LA8_145 = input.LA(5);
                        if ( (LA8_145=='$'||LA8_145=='.'||(LA8_145>='0' && LA8_145<='9')||(LA8_145>='A' && LA8_145<='Z')||(LA8_145>='a' && LA8_145<='z')) ) {
                            alt8=51;
                        }
                        else {
                            alt8=48;}
                    }
                    else {
                        alt8=51;}
                    break;
                default:
                    alt8=51;}

            }
            else {
                alt8=51;}
            break;
        case 'M':
            int LA8_28 = input.LA(2);
            if ( (LA8_28=='A') ) {
                int LA8_70 = input.LA(3);
                if ( (LA8_70=='C') ) {
                    int LA8_108 = input.LA(4);
                    if ( (LA8_108=='R') ) {
                        int LA8_146 = input.LA(5);
                        if ( (LA8_146=='O') ) {
                            int LA8_183 = input.LA(6);
                            if ( (LA8_183=='$'||LA8_183=='.'||(LA8_183>='0' && LA8_183<='9')||(LA8_183>='A' && LA8_183<='Z')||(LA8_183>='a' && LA8_183<='z')) ) {
                                alt8=51;
                            }
                            else {
                                alt8=49;}
                        }
                        else {
                            alt8=51;}
                    }
                    else {
                        alt8=51;}
                }
                else {
                    alt8=51;}
            }
            else {
                alt8=51;}
            break;
        case '\'':
            alt8=50;
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
        case 'b':
        case 'g':
        case 'h':
        case 'j':
        case 'k':
        case 'l':
        case 'o':
        case 'r':
        case 'u':
        case 'v':
        case 'x':
        case 'y':
        case 'z':
            alt8=51;
            break;
        case '\"':
            alt8=52;
            break;
        case '#':
            alt8=53;
            break;
        case '\t':
        case '\n':
        case ' ':
            alt8=54;
            break;
        case '%':
            alt8=55;
            break;
        default:
            NoViableAltException nvae =
                new NoViableAltException("1:1: Tokens : ( T25 | T26 | T27 | T28 | T29 | T30 | T31 | T32 | T33 | T34 | T35 | T36 | T37 | T38 | T39 | T40 | T41 | T42 | T43 | T44 | T45 | T46 | T47 | T48 | T49 | T50 | T51 | T52 | T53 | T54 | T55 | T56 | T57 | T58 | T59 | T60 | T61 | T62 | T63 | STRINGS | INTEGERS | FUNCTION | EXECUTE | SORT | ITERATE | REVERSE | ENTRY | READ | MACRO | QUOTED | IDENTIFIER | STRING | INTEGER | WS | LINE_COMMENT );", 8, 0, input);

            throw nvae;
        }

        switch (alt8) {
            case 1 :
                // bst.g:1:10: T25
                {
                mT25(); 

                }
                break;
            case 2 :
                // bst.g:1:14: T26
                {
                mT26(); 

                }
                break;
            case 3 :
                // bst.g:1:18: T27
                {
                mT27(); 

                }
                break;
            case 4 :
                // bst.g:1:22: T28
                {
                mT28(); 

                }
                break;
            case 5 :
                // bst.g:1:26: T29
                {
                mT29(); 

                }
                break;
            case 6 :
                // bst.g:1:30: T30
                {
                mT30(); 

                }
                break;
            case 7 :
                // bst.g:1:34: T31
                {
                mT31(); 

                }
                break;
            case 8 :
                // bst.g:1:38: T32
                {
                mT32(); 

                }
                break;
            case 9 :
                // bst.g:1:42: T33
                {
                mT33(); 

                }
                break;
            case 10 :
                // bst.g:1:46: T34
                {
                mT34(); 

                }
                break;
            case 11 :
                // bst.g:1:50: T35
                {
                mT35(); 

                }
                break;
            case 12 :
                // bst.g:1:54: T36
                {
                mT36(); 

                }
                break;
            case 13 :
                // bst.g:1:58: T37
                {
                mT37(); 

                }
                break;
            case 14 :
                // bst.g:1:62: T38
                {
                mT38(); 

                }
                break;
            case 15 :
                // bst.g:1:66: T39
                {
                mT39(); 

                }
                break;
            case 16 :
                // bst.g:1:70: T40
                {
                mT40(); 

                }
                break;
            case 17 :
                // bst.g:1:74: T41
                {
                mT41(); 

                }
                break;
            case 18 :
                // bst.g:1:78: T42
                {
                mT42(); 

                }
                break;
            case 19 :
                // bst.g:1:82: T43
                {
                mT43(); 

                }
                break;
            case 20 :
                // bst.g:1:86: T44
                {
                mT44(); 

                }
                break;
            case 21 :
                // bst.g:1:90: T45
                {
                mT45(); 

                }
                break;
            case 22 :
                // bst.g:1:94: T46
                {
                mT46(); 

                }
                break;
            case 23 :
                // bst.g:1:98: T47
                {
                mT47(); 

                }
                break;
            case 24 :
                // bst.g:1:102: T48
                {
                mT48(); 

                }
                break;
            case 25 :
                // bst.g:1:106: T49
                {
                mT49(); 

                }
                break;
            case 26 :
                // bst.g:1:110: T50
                {
                mT50(); 

                }
                break;
            case 27 :
                // bst.g:1:114: T51
                {
                mT51(); 

                }
                break;
            case 28 :
                // bst.g:1:118: T52
                {
                mT52(); 

                }
                break;
            case 29 :
                // bst.g:1:122: T53
                {
                mT53(); 

                }
                break;
            case 30 :
                // bst.g:1:126: T54
                {
                mT54(); 

                }
                break;
            case 31 :
                // bst.g:1:130: T55
                {
                mT55(); 

                }
                break;
            case 32 :
                // bst.g:1:134: T56
                {
                mT56(); 

                }
                break;
            case 33 :
                // bst.g:1:138: T57
                {
                mT57(); 

                }
                break;
            case 34 :
                // bst.g:1:142: T58
                {
                mT58(); 

                }
                break;
            case 35 :
                // bst.g:1:146: T59
                {
                mT59(); 

                }
                break;
            case 36 :
                // bst.g:1:150: T60
                {
                mT60(); 

                }
                break;
            case 37 :
                // bst.g:1:154: T61
                {
                mT61(); 

                }
                break;
            case 38 :
                // bst.g:1:158: T62
                {
                mT62(); 

                }
                break;
            case 39 :
                // bst.g:1:162: T63
                {
                mT63(); 

                }
                break;
            case 40 :
                // bst.g:1:166: STRINGS
                {
                mSTRINGS(); 

                }
                break;
            case 41 :
                // bst.g:1:174: INTEGERS
                {
                mINTEGERS(); 

                }
                break;
            case 42 :
                // bst.g:1:183: FUNCTION
                {
                mFUNCTION(); 

                }
                break;
            case 43 :
                // bst.g:1:192: EXECUTE
                {
                mEXECUTE(); 

                }
                break;
            case 44 :
                // bst.g:1:200: SORT
                {
                mSORT(); 

                }
                break;
            case 45 :
                // bst.g:1:205: ITERATE
                {
                mITERATE(); 

                }
                break;
            case 46 :
                // bst.g:1:213: REVERSE
                {
                mREVERSE(); 

                }
                break;
            case 47 :
                // bst.g:1:221: ENTRY
                {
                mENTRY(); 

                }
                break;
            case 48 :
                // bst.g:1:227: READ
                {
                mREAD(); 

                }
                break;
            case 49 :
                // bst.g:1:232: MACRO
                {
                mMACRO(); 

                }
                break;
            case 50 :
                // bst.g:1:238: QUOTED
                {
                mQUOTED(); 

                }
                break;
            case 51 :
                // bst.g:1:245: IDENTIFIER
                {
                mIDENTIFIER(); 

                }
                break;
            case 52 :
                // bst.g:1:256: STRING
                {
                mSTRING(); 

                }
                break;
            case 53 :
                // bst.g:1:263: INTEGER
                {
                mINTEGER(); 

                }
                break;
            case 54 :
                // bst.g:1:271: WS
                {
                mWS(); 

                }
                break;
            case 55 :
                // bst.g:1:274: LINE_COMMENT
                {
                mLINE_COMMENT(); 

                }
                break;

        }

    }


 

}
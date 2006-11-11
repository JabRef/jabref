package net.sf.jabref.bst;
// $ANTLR 3.0b4 bst.g 2006-09-17 01:45:41

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;


import org.antlr.runtime.tree.*;

public class Bst extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "IDLIST", "STACK", "ENTRY", "COMMANDS", "STRINGS", "INTEGERS", "FUNCTION", "MACRO", "STRING", "READ", "EXECUTE", "ITERATE", "REVERSE", "SORT", "IDENTIFIER", "INTEGER", "QUOTED", "LETTER", "NUMERAL", "WS", "LINE_COMMENT", "'{'", "'}'", "'<'", "'>'", "'='", "'+'", "'-'", "':='", "'*'", "'add.period$'", "'call.type$'", "'change.case$'", "'chr.to.int$'", "'cite$'", "'duplicat$'", "'empty$'", "'format.name$'", "'if$'", "'int.to.chr$'", "'int.to.str$'", "'missing$'", "'newline$'", "'num.names$'", "'pop$'", "'preamble$'", "'purify$'", "'quote$'", "'skip$'", "'stack$'", "'substring$'", "'swap$'", "'text.length$'", "'text.prefix$'", "'top$'", "'type$'", "'warning$'", "'while$'", "'width$'", "'write$'"
    };
    public static final int LETTER=21;
    public static final int ENTRY=6;
    public static final int INTEGERS=9;
    public static final int WS=23;
    public static final int COMMANDS=7;
    public static final int STRING=12;
    public static final int EXECUTE=14;
    public static final int LINE_COMMENT=24;
    public static final int SORT=17;
    public static final int STACK=5;
    public static final int REVERSE=16;
    public static final int QUOTED=20;
    public static final int INTEGER=19;
    public static final int ITERATE=15;
    public static final int FUNCTION=10;
    public static final int EOF=-1;
    public static final int STRINGS=8;
    public static final int IDENTIFIER=18;
    public static final int MACRO=11;
    public static final int IDLIST=4;
    public static final int NUMERAL=22;
    public static final int READ=13;

        public Bst(TokenStream input) {
            super(input);
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return tokenNames; }
    public String getGrammarFileName() { return "bst.g"; }


    public static class program_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start program
    // bst.g:14:1: program : ( commands )+ -> ^( COMMANDS ( commands )+ ) ;
    public program_return program() throws RecognitionException {   
        program_return retval = new program_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        commands_return commands1 = null;

        List list_commands=new ArrayList();

        try {
            // bst.g:14:11: ( ( commands )+ -> ^( COMMANDS ( commands )+ ) )
            // bst.g:14:11: ( commands )+
            {
            // bst.g:14:11: ( commands )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);
                if ( (LA1_0==ENTRY||(LA1_0>=STRINGS && LA1_0<=MACRO)||(LA1_0>=READ && LA1_0<=SORT)) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // bst.g:14:11: commands
            	    {
            	    pushFollow(FOLLOW_commands_in_program45);
            	    commands1=commands();
            	    _fsp--;

            	    list_commands.add(commands1.tree);

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = (Object)adaptor.nil();
            // 14:21: -> ^( COMMANDS ( commands )+ )
            {
                // bst.g:14:24: ^( COMMANDS ( commands )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(COMMANDS, "COMMANDS"), root_1);

                // bst.g:14:35: ( commands )+
                {
                int n_1 = list_commands == null ? 0 : list_commands.size();
                 


                if ( n_1==0 ) throw new RuntimeException("Must have more than one element for (...)+ loops");
                for (int i_1=0; i_1<n_1; i_1++) {
                    adaptor.addChild(root_1, list_commands.get(i_1));

                }
                }

                adaptor.addChild(root_0, root_1);
                }

            }



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end program

    public static class commands_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start commands
    // bst.g:16:1: commands : ( STRINGS^^ idList | INTEGERS^^ idList | FUNCTION^^ id stack | MACRO^^ id '{'! STRING '}'! | READ^^ | EXECUTE^^ '{'! function '}'! | ITERATE^^ '{'! function '}'! | REVERSE^^ '{'! function '}'! | ENTRY^^ idList0 idList0 idList0 | SORT^^ );
    public commands_return commands() throws RecognitionException {   
        commands_return retval = new commands_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRINGS2=null;
        Token INTEGERS4=null;
        Token FUNCTION6=null;
        Token MACRO9=null;
        Token char_literal11=null;
        Token STRING12=null;
        Token char_literal13=null;
        Token READ14=null;
        Token EXECUTE15=null;
        Token char_literal16=null;
        Token char_literal18=null;
        Token ITERATE19=null;
        Token char_literal20=null;
        Token char_literal22=null;
        Token REVERSE23=null;
        Token char_literal24=null;
        Token char_literal26=null;
        Token ENTRY27=null;
        Token SORT31=null;
        idList_return idList3 = null;

        idList_return idList5 = null;

        id_return id7 = null;

        stack_return stack8 = null;

        id_return id10 = null;

        function_return function17 = null;

        function_return function21 = null;

        function_return function25 = null;

        idList0_return idList028 = null;

        idList0_return idList029 = null;

        idList0_return idList030 = null;


        Object STRINGS2_tree=null;
        Object INTEGERS4_tree=null;
        Object FUNCTION6_tree=null;
        Object MACRO9_tree=null;
        Object char_literal11_tree=null;
        Object STRING12_tree=null;
        Object char_literal13_tree=null;
        Object READ14_tree=null;
        Object EXECUTE15_tree=null;
        Object char_literal16_tree=null;
        Object char_literal18_tree=null;
        Object ITERATE19_tree=null;
        Object char_literal20_tree=null;
        Object char_literal22_tree=null;
        Object REVERSE23_tree=null;
        Object char_literal24_tree=null;
        Object char_literal26_tree=null;
        Object ENTRY27_tree=null;
        Object SORT31_tree=null;

        try {
            // bst.g:17:4: ( STRINGS^^ idList | INTEGERS^^ idList | FUNCTION^^ id stack | MACRO^^ id '{'! STRING '}'! | READ^^ | EXECUTE^^ '{'! function '}'! | ITERATE^^ '{'! function '}'! | REVERSE^^ '{'! function '}'! | ENTRY^^ idList0 idList0 idList0 | SORT^^ )
            int alt2=10;
            switch ( input.LA(1) ) {
            case STRINGS:
                alt2=1;
                break;
            case INTEGERS:
                alt2=2;
                break;
            case FUNCTION:
                alt2=3;
                break;
            case MACRO:
                alt2=4;
                break;
            case READ:
                alt2=5;
                break;
            case EXECUTE:
                alt2=6;
                break;
            case ITERATE:
                alt2=7;
                break;
            case REVERSE:
                alt2=8;
                break;
            case ENTRY:
                alt2=9;
                break;
            case SORT:
                alt2=10;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("16:1: commands : ( STRINGS^^ idList | INTEGERS^^ idList | FUNCTION^^ id stack | MACRO^^ id '{'! STRING '}'! | READ^^ | EXECUTE^^ '{'! function '}'! | ITERATE^^ '{'! function '}'! | REVERSE^^ '{'! function '}'! | ENTRY^^ idList0 idList0 idList0 | SORT^^ );", 2, 0, input);

                throw nvae;
            }

            switch (alt2) {
                case 1 :
                    // bst.g:17:4: STRINGS^^ idList
                    {
                    root_0 = (Object)adaptor.nil();

                    STRINGS2=(Token)input.LT(1);
                    match(input,STRINGS,FOLLOW_STRINGS_in_commands65); 
                    STRINGS2_tree = (Object)adaptor.create(STRINGS2);
                    root_0 = (Object)adaptor.becomeRoot(STRINGS2_tree, root_0);

                    pushFollow(FOLLOW_idList_in_commands68);
                    idList3=idList();
                    _fsp--;

                    adaptor.addChild(root_0, idList3.tree);

                    }
                    break;
                case 2 :
                    // bst.g:18:4: INTEGERS^^ idList
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGERS4=(Token)input.LT(1);
                    match(input,INTEGERS,FOLLOW_INTEGERS_in_commands73); 
                    INTEGERS4_tree = (Object)adaptor.create(INTEGERS4);
                    root_0 = (Object)adaptor.becomeRoot(INTEGERS4_tree, root_0);

                    pushFollow(FOLLOW_idList_in_commands76);
                    idList5=idList();
                    _fsp--;

                    adaptor.addChild(root_0, idList5.tree);

                    }
                    break;
                case 3 :
                    // bst.g:19:4: FUNCTION^^ id stack
                    {
                    root_0 = (Object)adaptor.nil();

                    FUNCTION6=(Token)input.LT(1);
                    match(input,FUNCTION,FOLLOW_FUNCTION_in_commands81); 
                    FUNCTION6_tree = (Object)adaptor.create(FUNCTION6);
                    root_0 = (Object)adaptor.becomeRoot(FUNCTION6_tree, root_0);

                    pushFollow(FOLLOW_id_in_commands84);
                    id7=id();
                    _fsp--;

                    adaptor.addChild(root_0, id7.tree);
                    pushFollow(FOLLOW_stack_in_commands86);
                    stack8=stack();
                    _fsp--;

                    adaptor.addChild(root_0, stack8.tree);

                    }
                    break;
                case 4 :
                    // bst.g:20:4: MACRO^^ id '{'! STRING '}'!
                    {
                    root_0 = (Object)adaptor.nil();

                    MACRO9=(Token)input.LT(1);
                    match(input,MACRO,FOLLOW_MACRO_in_commands91); 
                    MACRO9_tree = (Object)adaptor.create(MACRO9);
                    root_0 = (Object)adaptor.becomeRoot(MACRO9_tree, root_0);

                    pushFollow(FOLLOW_id_in_commands94);
                    id10=id();
                    _fsp--;

                    adaptor.addChild(root_0, id10.tree);
                    char_literal11=(Token)input.LT(1);
                    match(input,25,FOLLOW_25_in_commands96); 
                    STRING12=(Token)input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_commands99); 
                    STRING12_tree = (Object)adaptor.create(STRING12);
                    adaptor.addChild(root_0, STRING12_tree);

                    char_literal13=(Token)input.LT(1);
                    match(input,26,FOLLOW_26_in_commands101); 

                    }
                    break;
                case 5 :
                    // bst.g:21:4: READ^^
                    {
                    root_0 = (Object)adaptor.nil();

                    READ14=(Token)input.LT(1);
                    match(input,READ,FOLLOW_READ_in_commands107); 
                    READ14_tree = (Object)adaptor.create(READ14);
                    root_0 = (Object)adaptor.becomeRoot(READ14_tree, root_0);


                    }
                    break;
                case 6 :
                    // bst.g:22:4: EXECUTE^^ '{'! function '}'!
                    {
                    root_0 = (Object)adaptor.nil();

                    EXECUTE15=(Token)input.LT(1);
                    match(input,EXECUTE,FOLLOW_EXECUTE_in_commands113); 
                    EXECUTE15_tree = (Object)adaptor.create(EXECUTE15);
                    root_0 = (Object)adaptor.becomeRoot(EXECUTE15_tree, root_0);

                    char_literal16=(Token)input.LT(1);
                    match(input,25,FOLLOW_25_in_commands116); 
                    pushFollow(FOLLOW_function_in_commands119);
                    function17=function();
                    _fsp--;

                    adaptor.addChild(root_0, function17.tree);
                    char_literal18=(Token)input.LT(1);
                    match(input,26,FOLLOW_26_in_commands121); 

                    }
                    break;
                case 7 :
                    // bst.g:23:4: ITERATE^^ '{'! function '}'!
                    {
                    root_0 = (Object)adaptor.nil();

                    ITERATE19=(Token)input.LT(1);
                    match(input,ITERATE,FOLLOW_ITERATE_in_commands127); 
                    ITERATE19_tree = (Object)adaptor.create(ITERATE19);
                    root_0 = (Object)adaptor.becomeRoot(ITERATE19_tree, root_0);

                    char_literal20=(Token)input.LT(1);
                    match(input,25,FOLLOW_25_in_commands130); 
                    pushFollow(FOLLOW_function_in_commands133);
                    function21=function();
                    _fsp--;

                    adaptor.addChild(root_0, function21.tree);
                    char_literal22=(Token)input.LT(1);
                    match(input,26,FOLLOW_26_in_commands135); 

                    }
                    break;
                case 8 :
                    // bst.g:24:4: REVERSE^^ '{'! function '}'!
                    {
                    root_0 = (Object)adaptor.nil();

                    REVERSE23=(Token)input.LT(1);
                    match(input,REVERSE,FOLLOW_REVERSE_in_commands141); 
                    REVERSE23_tree = (Object)adaptor.create(REVERSE23);
                    root_0 = (Object)adaptor.becomeRoot(REVERSE23_tree, root_0);

                    char_literal24=(Token)input.LT(1);
                    match(input,25,FOLLOW_25_in_commands144); 
                    pushFollow(FOLLOW_function_in_commands147);
                    function25=function();
                    _fsp--;

                    adaptor.addChild(root_0, function25.tree);
                    char_literal26=(Token)input.LT(1);
                    match(input,26,FOLLOW_26_in_commands149); 

                    }
                    break;
                case 9 :
                    // bst.g:25:4: ENTRY^^ idList0 idList0 idList0
                    {
                    root_0 = (Object)adaptor.nil();

                    ENTRY27=(Token)input.LT(1);
                    match(input,ENTRY,FOLLOW_ENTRY_in_commands155); 
                    ENTRY27_tree = (Object)adaptor.create(ENTRY27);
                    root_0 = (Object)adaptor.becomeRoot(ENTRY27_tree, root_0);

                    pushFollow(FOLLOW_idList0_in_commands158);
                    idList028=idList0();
                    _fsp--;

                    adaptor.addChild(root_0, idList028.tree);
                    pushFollow(FOLLOW_idList0_in_commands160);
                    idList029=idList0();
                    _fsp--;

                    adaptor.addChild(root_0, idList029.tree);
                    pushFollow(FOLLOW_idList0_in_commands162);
                    idList030=idList0();
                    _fsp--;

                    adaptor.addChild(root_0, idList030.tree);

                    }
                    break;
                case 10 :
                    // bst.g:26:4: SORT^^
                    {
                    root_0 = (Object)adaptor.nil();

                    SORT31=(Token)input.LT(1);
                    match(input,SORT,FOLLOW_SORT_in_commands167); 
                    SORT31_tree = (Object)adaptor.create(SORT31);
                    root_0 = (Object)adaptor.becomeRoot(SORT31_tree, root_0);


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end commands

    public static class identifier_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start identifier
    // bst.g:28:1: identifier : IDENTIFIER ;
    public identifier_return identifier() throws RecognitionException {   
        identifier_return retval = new identifier_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token IDENTIFIER32=null;

        Object IDENTIFIER32_tree=null;

        try {
            // bst.g:29:4: ( IDENTIFIER )
            // bst.g:29:4: IDENTIFIER
            {
            root_0 = (Object)adaptor.nil();

            IDENTIFIER32=(Token)input.LT(1);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_identifier178); 
            IDENTIFIER32_tree = (Object)adaptor.create(IDENTIFIER32);
            adaptor.addChild(root_0, IDENTIFIER32_tree);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end identifier

    public static class id_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start id
    // bst.g:31:1: id : '{'! identifier '}'! ;
    public id_return id() throws RecognitionException {   
        id_return retval = new id_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal33=null;
        Token char_literal35=null;
        identifier_return identifier34 = null;


        Object char_literal33_tree=null;
        Object char_literal35_tree=null;

        try {
            // bst.g:32:4: ( '{'! identifier '}'! )
            // bst.g:32:4: '{'! identifier '}'!
            {
            root_0 = (Object)adaptor.nil();

            char_literal33=(Token)input.LT(1);
            match(input,25,FOLLOW_25_in_id188); 
            pushFollow(FOLLOW_identifier_in_id191);
            identifier34=identifier();
            _fsp--;

            adaptor.addChild(root_0, identifier34.tree);
            char_literal35=(Token)input.LT(1);
            match(input,26,FOLLOW_26_in_id193); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end id

    public static class idList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start idList
    // bst.g:34:1: idList : '{' ( identifier )+ '}' -> ^( IDLIST ( identifier )+ ) ;
    public idList_return idList() throws RecognitionException {   
        idList_return retval = new idList_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal36=null;
        Token char_literal38=null;
        identifier_return identifier37 = null;

        List list_identifier=new ArrayList();
        List list_26=new ArrayList();
        List list_25=new ArrayList();
        Object char_literal36_tree=null;
        Object char_literal38_tree=null;

        try {
            // bst.g:35:4: ( '{' ( identifier )+ '}' -> ^( IDLIST ( identifier )+ ) )
            // bst.g:35:4: '{' ( identifier )+ '}'
            {
            char_literal36=(Token)input.LT(1);
            match(input,25,FOLLOW_25_in_idList205); 
            list_25.add(char_literal36);

            // bst.g:35:8: ( identifier )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);
                if ( (LA3_0==IDENTIFIER) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // bst.g:35:8: identifier
            	    {
            	    pushFollow(FOLLOW_identifier_in_idList207);
            	    identifier37=identifier();
            	    _fsp--;

            	    list_identifier.add(identifier37.tree);

            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);

            char_literal38=(Token)input.LT(1);
            match(input,26,FOLLOW_26_in_idList210); 
            list_26.add(char_literal38);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = (Object)adaptor.nil();
            // 35:24: -> ^( IDLIST ( identifier )+ )
            {
                // bst.g:35:27: ^( IDLIST ( identifier )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(IDLIST, "IDLIST"), root_1);

                // bst.g:35:36: ( identifier )+
                {
                int n_1 = list_identifier == null ? 0 : list_identifier.size();
                 


                if ( n_1==0 ) throw new RuntimeException("Must have more than one element for (...)+ loops");
                for (int i_1=0; i_1<n_1; i_1++) {
                    adaptor.addChild(root_1, list_identifier.get(i_1));

                }
                }

                adaptor.addChild(root_0, root_1);
                }

            }



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end idList

    public static class idList0_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start idList0
    // bst.g:37:1: idList0 : '{' ( identifier )* '}' -> ^( IDLIST ( identifier )* ) ;
    public idList0_return idList0() throws RecognitionException {   
        idList0_return retval = new idList0_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal39=null;
        Token char_literal41=null;
        identifier_return identifier40 = null;

        List list_identifier=new ArrayList();
        List list_26=new ArrayList();
        List list_25=new ArrayList();
        Object char_literal39_tree=null;
        Object char_literal41_tree=null;

        try {
            // bst.g:38:4: ( '{' ( identifier )* '}' -> ^( IDLIST ( identifier )* ) )
            // bst.g:38:4: '{' ( identifier )* '}'
            {
            char_literal39=(Token)input.LT(1);
            match(input,25,FOLLOW_25_in_idList0230); 
            list_25.add(char_literal39);

            // bst.g:38:8: ( identifier )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);
                if ( (LA4_0==IDENTIFIER) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // bst.g:38:8: identifier
            	    {
            	    pushFollow(FOLLOW_identifier_in_idList0232);
            	    identifier40=identifier();
            	    _fsp--;

            	    list_identifier.add(identifier40.tree);

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);

            char_literal41=(Token)input.LT(1);
            match(input,26,FOLLOW_26_in_idList0235); 
            list_26.add(char_literal41);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = (Object)adaptor.nil();
            // 38:24: -> ^( IDLIST ( identifier )* )
            {
                // bst.g:38:27: ^( IDLIST ( identifier )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(IDLIST, "IDLIST"), root_1);

                // bst.g:38:36: ( identifier )*
                {
                int n_1 = list_identifier == null ? 0 : list_identifier.size();
                 


                for (int i_1=0; i_1<n_1; i_1++) {
                    adaptor.addChild(root_1, list_identifier.get(i_1));

                }
                }

                adaptor.addChild(root_0, root_1);
                }

            }



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end idList0

    public static class function_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start function
    // bst.g:40:1: function : ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | 'add.period$' | 'call.type$' | 'change.case$' | 'chr.to.int$' | 'cite$' | 'duplicat$' | 'empty$' | 'format.name$' | 'if$' | 'int.to.chr$' | 'int.to.str$' | 'missing$' | 'newline$' | 'num.names$' | 'pop$' | 'preamble$' | 'purify$' | 'quote$' | 'skip$' | 'stack$' | 'substring$' | 'swap$' | 'text.length$' | 'text.prefix$' | 'top$' | 'type$' | 'warning$' | 'while$' | 'width$' | 'write$' | identifier );
    public function_return function() throws RecognitionException {   
        function_return retval = new function_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal42=null;
        Token char_literal43=null;
        Token char_literal44=null;
        Token char_literal45=null;
        Token char_literal46=null;
        Token string_literal47=null;
        Token char_literal48=null;
        Token string_literal49=null;
        Token string_literal50=null;
        Token string_literal51=null;
        Token string_literal52=null;
        Token string_literal53=null;
        Token string_literal54=null;
        Token string_literal55=null;
        Token string_literal56=null;
        Token string_literal57=null;
        Token string_literal58=null;
        Token string_literal59=null;
        Token string_literal60=null;
        Token string_literal61=null;
        Token string_literal62=null;
        Token string_literal63=null;
        Token string_literal64=null;
        Token string_literal65=null;
        Token string_literal66=null;
        Token string_literal67=null;
        Token string_literal68=null;
        Token string_literal69=null;
        Token string_literal70=null;
        Token string_literal71=null;
        Token string_literal72=null;
        Token string_literal73=null;
        Token string_literal74=null;
        Token string_literal75=null;
        Token string_literal76=null;
        Token string_literal77=null;
        Token string_literal78=null;
        identifier_return identifier79 = null;


        Object char_literal42_tree=null;
        Object char_literal43_tree=null;
        Object char_literal44_tree=null;
        Object char_literal45_tree=null;
        Object char_literal46_tree=null;
        Object string_literal47_tree=null;
        Object char_literal48_tree=null;
        Object string_literal49_tree=null;
        Object string_literal50_tree=null;
        Object string_literal51_tree=null;
        Object string_literal52_tree=null;
        Object string_literal53_tree=null;
        Object string_literal54_tree=null;
        Object string_literal55_tree=null;
        Object string_literal56_tree=null;
        Object string_literal57_tree=null;
        Object string_literal58_tree=null;
        Object string_literal59_tree=null;
        Object string_literal60_tree=null;
        Object string_literal61_tree=null;
        Object string_literal62_tree=null;
        Object string_literal63_tree=null;
        Object string_literal64_tree=null;
        Object string_literal65_tree=null;
        Object string_literal66_tree=null;
        Object string_literal67_tree=null;
        Object string_literal68_tree=null;
        Object string_literal69_tree=null;
        Object string_literal70_tree=null;
        Object string_literal71_tree=null;
        Object string_literal72_tree=null;
        Object string_literal73_tree=null;
        Object string_literal74_tree=null;
        Object string_literal75_tree=null;
        Object string_literal76_tree=null;
        Object string_literal77_tree=null;
        Object string_literal78_tree=null;

        try {
            // bst.g:41:4: ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | 'add.period$' | 'call.type$' | 'change.case$' | 'chr.to.int$' | 'cite$' | 'duplicat$' | 'empty$' | 'format.name$' | 'if$' | 'int.to.chr$' | 'int.to.str$' | 'missing$' | 'newline$' | 'num.names$' | 'pop$' | 'preamble$' | 'purify$' | 'quote$' | 'skip$' | 'stack$' | 'substring$' | 'swap$' | 'text.length$' | 'text.prefix$' | 'top$' | 'type$' | 'warning$' | 'while$' | 'width$' | 'write$' | identifier )
            int alt5=38;
            switch ( input.LA(1) ) {
            case 27:
                alt5=1;
                break;
            case 28:
                alt5=2;
                break;
            case 29:
                alt5=3;
                break;
            case 30:
                alt5=4;
                break;
            case 31:
                alt5=5;
                break;
            case 32:
                alt5=6;
                break;
            case 33:
                alt5=7;
                break;
            case 34:
                alt5=8;
                break;
            case 35:
                alt5=9;
                break;
            case 36:
                alt5=10;
                break;
            case 37:
                alt5=11;
                break;
            case 38:
                alt5=12;
                break;
            case 39:
                alt5=13;
                break;
            case 40:
                alt5=14;
                break;
            case 41:
                alt5=15;
                break;
            case 42:
                alt5=16;
                break;
            case 43:
                alt5=17;
                break;
            case 44:
                alt5=18;
                break;
            case 45:
                alt5=19;
                break;
            case 46:
                alt5=20;
                break;
            case 47:
                alt5=21;
                break;
            case 48:
                alt5=22;
                break;
            case 49:
                alt5=23;
                break;
            case 50:
                alt5=24;
                break;
            case 51:
                alt5=25;
                break;
            case 52:
                alt5=26;
                break;
            case 53:
                alt5=27;
                break;
            case 54:
                alt5=28;
                break;
            case 55:
                alt5=29;
                break;
            case 56:
                alt5=30;
                break;
            case 57:
                alt5=31;
                break;
            case 58:
                alt5=32;
                break;
            case 59:
                alt5=33;
                break;
            case 60:
                alt5=34;
                break;
            case 61:
                alt5=35;
                break;
            case 62:
                alt5=36;
                break;
            case 63:
                alt5=37;
                break;
            case IDENTIFIER:
                alt5=38;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("40:1: function : ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | 'add.period$' | 'call.type$' | 'change.case$' | 'chr.to.int$' | 'cite$' | 'duplicat$' | 'empty$' | 'format.name$' | 'if$' | 'int.to.chr$' | 'int.to.str$' | 'missing$' | 'newline$' | 'num.names$' | 'pop$' | 'preamble$' | 'purify$' | 'quote$' | 'skip$' | 'stack$' | 'substring$' | 'swap$' | 'text.length$' | 'text.prefix$' | 'top$' | 'type$' | 'warning$' | 'while$' | 'width$' | 'write$' | identifier );", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // bst.g:41:4: '<'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal42=(Token)input.LT(1);
                    match(input,27,FOLLOW_27_in_function254); 
                    char_literal42_tree = (Object)adaptor.create(char_literal42);
                    adaptor.addChild(root_0, char_literal42_tree);


                    }
                    break;
                case 2 :
                    // bst.g:41:10: '>'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal43=(Token)input.LT(1);
                    match(input,28,FOLLOW_28_in_function258); 
                    char_literal43_tree = (Object)adaptor.create(char_literal43);
                    adaptor.addChild(root_0, char_literal43_tree);


                    }
                    break;
                case 3 :
                    // bst.g:41:16: '='
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal44=(Token)input.LT(1);
                    match(input,29,FOLLOW_29_in_function262); 
                    char_literal44_tree = (Object)adaptor.create(char_literal44);
                    adaptor.addChild(root_0, char_literal44_tree);


                    }
                    break;
                case 4 :
                    // bst.g:41:22: '+'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal45=(Token)input.LT(1);
                    match(input,30,FOLLOW_30_in_function266); 
                    char_literal45_tree = (Object)adaptor.create(char_literal45);
                    adaptor.addChild(root_0, char_literal45_tree);


                    }
                    break;
                case 5 :
                    // bst.g:41:28: '-'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal46=(Token)input.LT(1);
                    match(input,31,FOLLOW_31_in_function270); 
                    char_literal46_tree = (Object)adaptor.create(char_literal46);
                    adaptor.addChild(root_0, char_literal46_tree);


                    }
                    break;
                case 6 :
                    // bst.g:41:34: ':='
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal47=(Token)input.LT(1);
                    match(input,32,FOLLOW_32_in_function274); 
                    string_literal47_tree = (Object)adaptor.create(string_literal47);
                    adaptor.addChild(root_0, string_literal47_tree);


                    }
                    break;
                case 7 :
                    // bst.g:41:41: '*'
                    {
                    root_0 = (Object)adaptor.nil();

                    char_literal48=(Token)input.LT(1);
                    match(input,33,FOLLOW_33_in_function278); 
                    char_literal48_tree = (Object)adaptor.create(char_literal48);
                    adaptor.addChild(root_0, char_literal48_tree);


                    }
                    break;
                case 8 :
                    // bst.g:41:47: 'add.period$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal49=(Token)input.LT(1);
                    match(input,34,FOLLOW_34_in_function282); 
                    string_literal49_tree = (Object)adaptor.create(string_literal49);
                    adaptor.addChild(root_0, string_literal49_tree);


                    }
                    break;
                case 9 :
                    // bst.g:41:63: 'call.type$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal50=(Token)input.LT(1);
                    match(input,35,FOLLOW_35_in_function286); 
                    string_literal50_tree = (Object)adaptor.create(string_literal50);
                    adaptor.addChild(root_0, string_literal50_tree);


                    }
                    break;
                case 10 :
                    // bst.g:41:78: 'change.case$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal51=(Token)input.LT(1);
                    match(input,36,FOLLOW_36_in_function290); 
                    string_literal51_tree = (Object)adaptor.create(string_literal51);
                    adaptor.addChild(root_0, string_literal51_tree);


                    }
                    break;
                case 11 :
                    // bst.g:41:95: 'chr.to.int$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal52=(Token)input.LT(1);
                    match(input,37,FOLLOW_37_in_function294); 
                    string_literal52_tree = (Object)adaptor.create(string_literal52);
                    adaptor.addChild(root_0, string_literal52_tree);


                    }
                    break;
                case 12 :
                    // bst.g:42:4: 'cite$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal53=(Token)input.LT(1);
                    match(input,38,FOLLOW_38_in_function299); 
                    string_literal53_tree = (Object)adaptor.create(string_literal53);
                    adaptor.addChild(root_0, string_literal53_tree);


                    }
                    break;
                case 13 :
                    // bst.g:42:14: 'duplicat$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal54=(Token)input.LT(1);
                    match(input,39,FOLLOW_39_in_function303); 
                    string_literal54_tree = (Object)adaptor.create(string_literal54);
                    adaptor.addChild(root_0, string_literal54_tree);


                    }
                    break;
                case 14 :
                    // bst.g:42:28: 'empty$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal55=(Token)input.LT(1);
                    match(input,40,FOLLOW_40_in_function307); 
                    string_literal55_tree = (Object)adaptor.create(string_literal55);
                    adaptor.addChild(root_0, string_literal55_tree);


                    }
                    break;
                case 15 :
                    // bst.g:42:39: 'format.name$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal56=(Token)input.LT(1);
                    match(input,41,FOLLOW_41_in_function311); 
                    string_literal56_tree = (Object)adaptor.create(string_literal56);
                    adaptor.addChild(root_0, string_literal56_tree);


                    }
                    break;
                case 16 :
                    // bst.g:42:56: 'if$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal57=(Token)input.LT(1);
                    match(input,42,FOLLOW_42_in_function315); 
                    string_literal57_tree = (Object)adaptor.create(string_literal57);
                    adaptor.addChild(root_0, string_literal57_tree);


                    }
                    break;
                case 17 :
                    // bst.g:42:64: 'int.to.chr$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal58=(Token)input.LT(1);
                    match(input,43,FOLLOW_43_in_function319); 
                    string_literal58_tree = (Object)adaptor.create(string_literal58);
                    adaptor.addChild(root_0, string_literal58_tree);


                    }
                    break;
                case 18 :
                    // bst.g:42:80: 'int.to.str$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal59=(Token)input.LT(1);
                    match(input,44,FOLLOW_44_in_function323); 
                    string_literal59_tree = (Object)adaptor.create(string_literal59);
                    adaptor.addChild(root_0, string_literal59_tree);


                    }
                    break;
                case 19 :
                    // bst.g:42:96: 'missing$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal60=(Token)input.LT(1);
                    match(input,45,FOLLOW_45_in_function327); 
                    string_literal60_tree = (Object)adaptor.create(string_literal60);
                    adaptor.addChild(root_0, string_literal60_tree);


                    }
                    break;
                case 20 :
                    // bst.g:43:4: 'newline$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal61=(Token)input.LT(1);
                    match(input,46,FOLLOW_46_in_function332); 
                    string_literal61_tree = (Object)adaptor.create(string_literal61);
                    adaptor.addChild(root_0, string_literal61_tree);


                    }
                    break;
                case 21 :
                    // bst.g:43:17: 'num.names$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal62=(Token)input.LT(1);
                    match(input,47,FOLLOW_47_in_function336); 
                    string_literal62_tree = (Object)adaptor.create(string_literal62);
                    adaptor.addChild(root_0, string_literal62_tree);


                    }
                    break;
                case 22 :
                    // bst.g:43:32: 'pop$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal63=(Token)input.LT(1);
                    match(input,48,FOLLOW_48_in_function340); 
                    string_literal63_tree = (Object)adaptor.create(string_literal63);
                    adaptor.addChild(root_0, string_literal63_tree);


                    }
                    break;
                case 23 :
                    // bst.g:43:41: 'preamble$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal64=(Token)input.LT(1);
                    match(input,49,FOLLOW_49_in_function344); 
                    string_literal64_tree = (Object)adaptor.create(string_literal64);
                    adaptor.addChild(root_0, string_literal64_tree);


                    }
                    break;
                case 24 :
                    // bst.g:43:55: 'purify$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal65=(Token)input.LT(1);
                    match(input,50,FOLLOW_50_in_function348); 
                    string_literal65_tree = (Object)adaptor.create(string_literal65);
                    adaptor.addChild(root_0, string_literal65_tree);


                    }
                    break;
                case 25 :
                    // bst.g:43:67: 'quote$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal66=(Token)input.LT(1);
                    match(input,51,FOLLOW_51_in_function352); 
                    string_literal66_tree = (Object)adaptor.create(string_literal66);
                    adaptor.addChild(root_0, string_literal66_tree);


                    }
                    break;
                case 26 :
                    // bst.g:43:78: 'skip$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal67=(Token)input.LT(1);
                    match(input,52,FOLLOW_52_in_function356); 
                    string_literal67_tree = (Object)adaptor.create(string_literal67);
                    adaptor.addChild(root_0, string_literal67_tree);


                    }
                    break;
                case 27 :
                    // bst.g:43:88: 'stack$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal68=(Token)input.LT(1);
                    match(input,53,FOLLOW_53_in_function360); 
                    string_literal68_tree = (Object)adaptor.create(string_literal68);
                    adaptor.addChild(root_0, string_literal68_tree);


                    }
                    break;
                case 28 :
                    // bst.g:43:99: 'substring$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal69=(Token)input.LT(1);
                    match(input,54,FOLLOW_54_in_function364); 
                    string_literal69_tree = (Object)adaptor.create(string_literal69);
                    adaptor.addChild(root_0, string_literal69_tree);


                    }
                    break;
                case 29 :
                    // bst.g:44:4: 'swap$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal70=(Token)input.LT(1);
                    match(input,55,FOLLOW_55_in_function369); 
                    string_literal70_tree = (Object)adaptor.create(string_literal70);
                    adaptor.addChild(root_0, string_literal70_tree);


                    }
                    break;
                case 30 :
                    // bst.g:44:14: 'text.length$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal71=(Token)input.LT(1);
                    match(input,56,FOLLOW_56_in_function373); 
                    string_literal71_tree = (Object)adaptor.create(string_literal71);
                    adaptor.addChild(root_0, string_literal71_tree);


                    }
                    break;
                case 31 :
                    // bst.g:44:31: 'text.prefix$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal72=(Token)input.LT(1);
                    match(input,57,FOLLOW_57_in_function377); 
                    string_literal72_tree = (Object)adaptor.create(string_literal72);
                    adaptor.addChild(root_0, string_literal72_tree);


                    }
                    break;
                case 32 :
                    // bst.g:44:48: 'top$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal73=(Token)input.LT(1);
                    match(input,58,FOLLOW_58_in_function381); 
                    string_literal73_tree = (Object)adaptor.create(string_literal73);
                    adaptor.addChild(root_0, string_literal73_tree);


                    }
                    break;
                case 33 :
                    // bst.g:44:57: 'type$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal74=(Token)input.LT(1);
                    match(input,59,FOLLOW_59_in_function385); 
                    string_literal74_tree = (Object)adaptor.create(string_literal74);
                    adaptor.addChild(root_0, string_literal74_tree);


                    }
                    break;
                case 34 :
                    // bst.g:44:67: 'warning$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal75=(Token)input.LT(1);
                    match(input,60,FOLLOW_60_in_function389); 
                    string_literal75_tree = (Object)adaptor.create(string_literal75);
                    adaptor.addChild(root_0, string_literal75_tree);


                    }
                    break;
                case 35 :
                    // bst.g:44:80: 'while$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal76=(Token)input.LT(1);
                    match(input,61,FOLLOW_61_in_function393); 
                    string_literal76_tree = (Object)adaptor.create(string_literal76);
                    adaptor.addChild(root_0, string_literal76_tree);


                    }
                    break;
                case 36 :
                    // bst.g:44:91: 'width$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal77=(Token)input.LT(1);
                    match(input,62,FOLLOW_62_in_function397); 
                    string_literal77_tree = (Object)adaptor.create(string_literal77);
                    adaptor.addChild(root_0, string_literal77_tree);


                    }
                    break;
                case 37 :
                    // bst.g:45:4: 'write$'
                    {
                    root_0 = (Object)adaptor.nil();

                    string_literal78=(Token)input.LT(1);
                    match(input,63,FOLLOW_63_in_function402); 
                    string_literal78_tree = (Object)adaptor.create(string_literal78);
                    adaptor.addChild(root_0, string_literal78_tree);


                    }
                    break;
                case 38 :
                    // bst.g:45:15: identifier
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_identifier_in_function406);
                    identifier79=identifier();
                    _fsp--;

                    adaptor.addChild(root_0, identifier79.tree);

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end function

    public static class stack_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start stack
    // bst.g:47:1: stack : '{' ( stackitem )+ '}' -> ^( STACK ( stackitem )+ ) ;
    public stack_return stack() throws RecognitionException {   
        stack_return retval = new stack_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal80=null;
        Token char_literal82=null;
        stackitem_return stackitem81 = null;

        List list_stackitem=new ArrayList();
        List list_26=new ArrayList();
        List list_25=new ArrayList();
        Object char_literal80_tree=null;
        Object char_literal82_tree=null;

        try {
            // bst.g:48:4: ( '{' ( stackitem )+ '}' -> ^( STACK ( stackitem )+ ) )
            // bst.g:48:4: '{' ( stackitem )+ '}'
            {
            char_literal80=(Token)input.LT(1);
            match(input,25,FOLLOW_25_in_stack417); 
            list_25.add(char_literal80);

            // bst.g:48:8: ( stackitem )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);
                if ( (LA6_0==STRING||(LA6_0>=IDENTIFIER && LA6_0<=QUOTED)||LA6_0==25||(LA6_0>=27 && LA6_0<=63)) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // bst.g:48:8: stackitem
            	    {
            	    pushFollow(FOLLOW_stackitem_in_stack419);
            	    stackitem81=stackitem();
            	    _fsp--;

            	    list_stackitem.add(stackitem81.tree);

            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);

            char_literal82=(Token)input.LT(1);
            match(input,26,FOLLOW_26_in_stack422); 
            list_26.add(char_literal82);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = (Object)adaptor.nil();
            // 48:23: -> ^( STACK ( stackitem )+ )
            {
                // bst.g:48:26: ^( STACK ( stackitem )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(adaptor.create(STACK, "STACK"), root_1);

                // bst.g:48:34: ( stackitem )+
                {
                int n_1 = list_stackitem == null ? 0 : list_stackitem.size();
                 


                if ( n_1==0 ) throw new RuntimeException("Must have more than one element for (...)+ loops");
                for (int i_1=0; i_1<n_1; i_1++) {
                    adaptor.addChild(root_1, list_stackitem.get(i_1));

                }
                }

                adaptor.addChild(root_0, root_1);
                }

            }



            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end stack

    public static class stackitem_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start stackitem
    // bst.g:50:1: stackitem : ( function | STRING | INTEGER | QUOTED | stack );
    public stackitem_return stackitem() throws RecognitionException {   
        stackitem_return retval = new stackitem_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRING84=null;
        Token INTEGER85=null;
        Token QUOTED86=null;
        function_return function83 = null;

        stack_return stack87 = null;


        Object STRING84_tree=null;
        Object INTEGER85_tree=null;
        Object QUOTED86_tree=null;

        try {
            // bst.g:51:4: ( function | STRING | INTEGER | QUOTED | stack )
            int alt7=5;
            switch ( input.LA(1) ) {
            case IDENTIFIER:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
                alt7=1;
                break;
            case STRING:
                alt7=2;
                break;
            case INTEGER:
                alt7=3;
                break;
            case QUOTED:
                alt7=4;
                break;
            case 25:
                alt7=5;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("50:1: stackitem : ( function | STRING | INTEGER | QUOTED | stack );", 7, 0, input);

                throw nvae;
            }

            switch (alt7) {
                case 1 :
                    // bst.g:51:4: function
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_function_in_stackitem441);
                    function83=function();
                    _fsp--;

                    adaptor.addChild(root_0, function83.tree);

                    }
                    break;
                case 2 :
                    // bst.g:52:4: STRING
                    {
                    root_0 = (Object)adaptor.nil();

                    STRING84=(Token)input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_stackitem446); 
                    STRING84_tree = (Object)adaptor.create(STRING84);
                    adaptor.addChild(root_0, STRING84_tree);


                    }
                    break;
                case 3 :
                    // bst.g:53:4: INTEGER
                    {
                    root_0 = (Object)adaptor.nil();

                    INTEGER85=(Token)input.LT(1);
                    match(input,INTEGER,FOLLOW_INTEGER_in_stackitem452); 
                    INTEGER85_tree = (Object)adaptor.create(INTEGER85);
                    adaptor.addChild(root_0, INTEGER85_tree);


                    }
                    break;
                case 4 :
                    // bst.g:54:4: QUOTED
                    {
                    root_0 = (Object)adaptor.nil();

                    QUOTED86=(Token)input.LT(1);
                    match(input,QUOTED,FOLLOW_QUOTED_in_stackitem458); 
                    QUOTED86_tree = (Object)adaptor.create(QUOTED86);
                    adaptor.addChild(root_0, QUOTED86_tree);


                    }
                    break;
                case 5 :
                    // bst.g:55:4: stack
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_stack_in_stackitem463);
                    stack87=stack();
                    _fsp--;

                    adaptor.addChild(root_0, stack87.tree);

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = (Object)adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        return retval;
    }
    // $ANTLR end stackitem


 

    public static final BitSet FOLLOW_commands_in_program45 = new BitSet(new long[]{0x000000000003EF42L});
    public static final BitSet FOLLOW_STRINGS_in_commands65 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_idList_in_commands68 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INTEGERS_in_commands73 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_idList_in_commands76 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FUNCTION_in_commands81 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_id_in_commands84 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_stack_in_commands86 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MACRO_in_commands91 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_id_in_commands94 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands96 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_STRING_in_commands99 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_READ_in_commands107 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EXECUTE_in_commands113 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands116 = new BitSet(new long[]{0xFFFFFFFFF8040000L});
    public static final BitSet FOLLOW_function_in_commands119 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands121 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ITERATE_in_commands127 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands130 = new BitSet(new long[]{0xFFFFFFFFF8040000L});
    public static final BitSet FOLLOW_function_in_commands133 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands135 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REVERSE_in_commands141 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands144 = new BitSet(new long[]{0xFFFFFFFFF8040000L});
    public static final BitSet FOLLOW_function_in_commands147 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands149 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ENTRY_in_commands155 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_idList0_in_commands158 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_idList0_in_commands160 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_idList0_in_commands162 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SORT_in_commands167 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_identifier178 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_id188 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_identifier_in_id191 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_id193 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_idList205 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_identifier_in_idList207 = new BitSet(new long[]{0x0000000004040000L});
    public static final BitSet FOLLOW_26_in_idList210 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_idList0230 = new BitSet(new long[]{0x0000000004040000L});
    public static final BitSet FOLLOW_identifier_in_idList0232 = new BitSet(new long[]{0x0000000004040000L});
    public static final BitSet FOLLOW_26_in_idList0235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_function254 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_function258 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_function262 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_function266 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_function270 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_function274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_function278 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_function282 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_function286 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_function290 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_function294 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_function299 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_function303 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_function307 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_function311 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_function315 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_function319 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_function323 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_function327 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_46_in_function332 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_47_in_function336 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_48_in_function340 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_49_in_function344 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_50_in_function348 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_51_in_function352 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_52_in_function356 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_function360 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_54_in_function364 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_55_in_function369 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_56_in_function373 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_57_in_function377 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_58_in_function381 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_59_in_function385 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_60_in_function389 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_function393 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_function397 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_63_in_function402 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_identifier_in_function406 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_stack417 = new BitSet(new long[]{0xFFFFFFFFFA1C1000L});
    public static final BitSet FOLLOW_stackitem_in_stack419 = new BitSet(new long[]{0xFFFFFFFFFE1C1000L});
    public static final BitSet FOLLOW_26_in_stack422 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_stackitem441 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stackitem446 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INTEGER_in_stackitem452 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QUOTED_in_stackitem458 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_stack_in_stackitem463 = new BitSet(new long[]{0x0000000000000002L});

}
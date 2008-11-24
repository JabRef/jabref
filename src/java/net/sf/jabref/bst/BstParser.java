package net.sf.jabref.bst;

// $ANTLR 3.0b5 Bst.g 2006-11-23 23:20:24

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.TreeAdaptor;

@SuppressWarnings({"unused", "unchecked"})
public class BstParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "IDLIST", "STACK", "ENTRY", "COMMANDS", "STRINGS", "INTEGERS", "FUNCTION", "MACRO", "STRING", "READ", "EXECUTE", "ITERATE", "REVERSE", "SORT", "IDENTIFIER", "INTEGER", "QUOTED", "LETTER", "NUMERAL", "WS", "LINE_COMMENT", "'{'", "'}'", "'<'", "'>'", "'='", "'+'", "'-'", "':='", "'*'"
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

        public BstParser(TokenStream input) {
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
    public String getGrammarFileName() { return "Bst.g"; }


    public static class program_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start program
    // Bst.g:14:1: program : ( commands )+ -> ^( COMMANDS ( commands )+ ) ;
    public program_return program() throws RecognitionException {   
        program_return retval = new program_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        commands_return commands1 = null;

        List list_commands=new ArrayList();

        try {
            // Bst.g:14:11: ( ( commands )+ -> ^( COMMANDS ( commands )+ ) )
            // Bst.g:14:11: ( commands )+
            {
            // Bst.g:14:11: ( commands )+
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
            	    // Bst.g:14:11: commands
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
            root_0 = adaptor.nil();
            // 14:21: -> ^( COMMANDS ( commands )+ )
            {
                // Bst.g:14:24: ^( COMMANDS ( commands )+ )
                {
                Object root_1 = adaptor.nil();
                root_1 = adaptor.becomeRoot(adaptor.create(COMMANDS, "COMMANDS"), root_1);

                // Bst.g:14:35: ( commands )+
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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end program

    public static class commands_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start commands
    // Bst.g:16:1: commands : ( STRINGS^^ idList | INTEGERS^^ idList | FUNCTION^^ id stack | MACRO^^ id '{'! STRING '}'! | READ^^ | EXECUTE^^ '{'! function '}'! | ITERATE^^ '{'! function '}'! | REVERSE^^ '{'! function '}'! | ENTRY^^ idList0 idList0 idList0 | SORT^^ );
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
            // Bst.g:17:4: ( STRINGS^^ idList | INTEGERS^^ idList | FUNCTION^^ id stack | MACRO^^ id '{'! STRING '}'! | READ^^ | EXECUTE^^ '{'! function '}'! | ITERATE^^ '{'! function '}'! | REVERSE^^ '{'! function '}'! | ENTRY^^ idList0 idList0 idList0 | SORT^^ )
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
                    // Bst.g:17:4: STRINGS^^ idList
                    {
                    root_0 = adaptor.nil();

                    STRINGS2=input.LT(1);
                    match(input,STRINGS,FOLLOW_STRINGS_in_commands65); 
                    STRINGS2_tree = adaptor.create(STRINGS2);
                    root_0 = adaptor.becomeRoot(STRINGS2_tree, root_0);

                    pushFollow(FOLLOW_idList_in_commands68);
                    idList3=idList();
                    _fsp--;

                    adaptor.addChild(root_0, idList3.tree);

                    }
                    break;
                case 2 :
                    // Bst.g:18:4: INTEGERS^^ idList
                    {
                    root_0 = adaptor.nil();

                    INTEGERS4=input.LT(1);
                    match(input,INTEGERS,FOLLOW_INTEGERS_in_commands73); 
                    INTEGERS4_tree = adaptor.create(INTEGERS4);
                    root_0 = adaptor.becomeRoot(INTEGERS4_tree, root_0);

                    pushFollow(FOLLOW_idList_in_commands76);
                    idList5=idList();
                    _fsp--;

                    adaptor.addChild(root_0, idList5.tree);

                    }
                    break;
                case 3 :
                    // Bst.g:19:4: FUNCTION^^ id stack
                    {
                    root_0 = adaptor.nil();

                    FUNCTION6=input.LT(1);
                    match(input,FUNCTION,FOLLOW_FUNCTION_in_commands81); 
                    FUNCTION6_tree = adaptor.create(FUNCTION6);
                    root_0 = adaptor.becomeRoot(FUNCTION6_tree, root_0);

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
                    // Bst.g:20:4: MACRO^^ id '{'! STRING '}'!
                    {
                    root_0 = adaptor.nil();

                    MACRO9=input.LT(1);
                    match(input,MACRO,FOLLOW_MACRO_in_commands91); 
                    MACRO9_tree = adaptor.create(MACRO9);
                    root_0 = adaptor.becomeRoot(MACRO9_tree, root_0);

                    pushFollow(FOLLOW_id_in_commands94);
                    id10=id();
                    _fsp--;

                    adaptor.addChild(root_0, id10.tree);
                    char_literal11=input.LT(1);
                    match(input,25,FOLLOW_25_in_commands96); 
                    STRING12=input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_commands99); 
                    STRING12_tree = adaptor.create(STRING12);
                    adaptor.addChild(root_0, STRING12_tree);

                    char_literal13=input.LT(1);
                    match(input,26,FOLLOW_26_in_commands101); 

                    }
                    break;
                case 5 :
                    // Bst.g:21:4: READ^^
                    {
                    root_0 = adaptor.nil();

                    READ14=input.LT(1);
                    match(input,READ,FOLLOW_READ_in_commands107); 
                    READ14_tree = adaptor.create(READ14);
                    root_0 = adaptor.becomeRoot(READ14_tree, root_0);


                    }
                    break;
                case 6 :
                    // Bst.g:22:4: EXECUTE^^ '{'! function '}'!
                    {
                    root_0 = adaptor.nil();

                    EXECUTE15=input.LT(1);
                    match(input,EXECUTE,FOLLOW_EXECUTE_in_commands113); 
                    EXECUTE15_tree = adaptor.create(EXECUTE15);
                    root_0 = adaptor.becomeRoot(EXECUTE15_tree, root_0);

                    char_literal16=input.LT(1);
                    match(input,25,FOLLOW_25_in_commands116); 
                    pushFollow(FOLLOW_function_in_commands119);
                    function17=function();
                    _fsp--;

                    adaptor.addChild(root_0, function17.tree);
                    char_literal18=input.LT(1);
                    match(input,26,FOLLOW_26_in_commands121); 

                    }
                    break;
                case 7 :
                    // Bst.g:23:4: ITERATE^^ '{'! function '}'!
                    {
                    root_0 = adaptor.nil();

                    ITERATE19=input.LT(1);
                    match(input,ITERATE,FOLLOW_ITERATE_in_commands127); 
                    ITERATE19_tree = adaptor.create(ITERATE19);
                    root_0 = adaptor.becomeRoot(ITERATE19_tree, root_0);

                    char_literal20=input.LT(1);
                    match(input,25,FOLLOW_25_in_commands130); 
                    pushFollow(FOLLOW_function_in_commands133);
                    function21=function();
                    _fsp--;

                    adaptor.addChild(root_0, function21.tree);
                    char_literal22=input.LT(1);
                    match(input,26,FOLLOW_26_in_commands135); 

                    }
                    break;
                case 8 :
                    // Bst.g:24:4: REVERSE^^ '{'! function '}'!
                    {
                    root_0 = adaptor.nil();

                    REVERSE23=input.LT(1);
                    match(input,REVERSE,FOLLOW_REVERSE_in_commands141); 
                    REVERSE23_tree = adaptor.create(REVERSE23);
                    root_0 = adaptor.becomeRoot(REVERSE23_tree, root_0);

                    char_literal24=input.LT(1);
                    match(input,25,FOLLOW_25_in_commands144); 
                    pushFollow(FOLLOW_function_in_commands147);
                    function25=function();
                    _fsp--;

                    adaptor.addChild(root_0, function25.tree);
                    char_literal26=input.LT(1);
                    match(input,26,FOLLOW_26_in_commands149); 

                    }
                    break;
                case 9 :
                    // Bst.g:25:4: ENTRY^^ idList0 idList0 idList0
                    {
                    root_0 = adaptor.nil();

                    ENTRY27=input.LT(1);
                    match(input,ENTRY,FOLLOW_ENTRY_in_commands155); 
                    ENTRY27_tree = adaptor.create(ENTRY27);
                    root_0 = adaptor.becomeRoot(ENTRY27_tree, root_0);

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
                    // Bst.g:26:4: SORT^^
                    {
                    root_0 = adaptor.nil();

                    SORT31=input.LT(1);
                    match(input,SORT,FOLLOW_SORT_in_commands167); 
                    SORT31_tree = adaptor.create(SORT31);
                    root_0 = adaptor.becomeRoot(SORT31_tree, root_0);


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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end commands

    public static class identifier_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start identifier
    // Bst.g:28:1: identifier : IDENTIFIER ;
    public identifier_return identifier() throws RecognitionException {   
        identifier_return retval = new identifier_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token IDENTIFIER32=null;

        Object IDENTIFIER32_tree=null;

        try {
            // Bst.g:29:4: ( IDENTIFIER )
            // Bst.g:29:4: IDENTIFIER
            {
            root_0 = adaptor.nil();

            IDENTIFIER32=input.LT(1);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_identifier178); 
            IDENTIFIER32_tree = adaptor.create(IDENTIFIER32);
            adaptor.addChild(root_0, IDENTIFIER32_tree);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end identifier

    public static class id_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start id
    // Bst.g:31:1: id : '{'! identifier '}'! ;
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
            // Bst.g:32:4: ( '{'! identifier '}'! )
            // Bst.g:32:4: '{'! identifier '}'!
            {
            root_0 = adaptor.nil();

            char_literal33=input.LT(1);
            match(input,25,FOLLOW_25_in_id188); 
            pushFollow(FOLLOW_identifier_in_id191);
            identifier34=identifier();
            _fsp--;

            adaptor.addChild(root_0, identifier34.tree);
            char_literal35=input.LT(1);
            match(input,26,FOLLOW_26_in_id193); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            retval.stop = input.LT(-1);

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end id

    public static class idList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start idList
    // Bst.g:34:1: idList : '{' ( identifier )+ '}' -> ^( IDLIST ( identifier )+ ) ;
    
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
            // Bst.g:35:4: ( '{' ( identifier )+ '}' -> ^( IDLIST ( identifier )+ ) )
            // Bst.g:35:4: '{' ( identifier )+ '}'
            {
            char_literal36=input.LT(1);
            match(input,25,FOLLOW_25_in_idList205); 
            list_25.add(char_literal36);

            // Bst.g:35:8: ( identifier )+
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
            	    // Bst.g:35:8: identifier
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

            char_literal38=input.LT(1);
            match(input,26,FOLLOW_26_in_idList210); 
            list_26.add(char_literal38);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = adaptor.nil();
            // 35:24: -> ^( IDLIST ( identifier )+ )
            {
                // Bst.g:35:27: ^( IDLIST ( identifier )+ )
                {
                Object root_1 = adaptor.nil();
                root_1 = adaptor.becomeRoot(adaptor.create(IDLIST, "IDLIST"), root_1);

                // Bst.g:35:36: ( identifier )+
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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end idList

    public static class idList0_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start idList0
    // Bst.g:37:1: idList0 : '{' ( identifier )* '}' -> ^( IDLIST ( identifier )* ) ;
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
            // Bst.g:38:4: ( '{' ( identifier )* '}' -> ^( IDLIST ( identifier )* ) )
            // Bst.g:38:4: '{' ( identifier )* '}'
            {
            char_literal39=input.LT(1);
            match(input,25,FOLLOW_25_in_idList0230); 
            list_25.add(char_literal39);

            // Bst.g:38:8: ( identifier )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);
                if ( (LA4_0==IDENTIFIER) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // Bst.g:38:8: identifier
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

            char_literal41=input.LT(1);
            match(input,26,FOLLOW_26_in_idList0235); 
            list_26.add(char_literal41);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = adaptor.nil();
            // 38:24: -> ^( IDLIST ( identifier )* )
            {
                // Bst.g:38:27: ^( IDLIST ( identifier )* )
                {
                Object root_1 = adaptor.nil();
                root_1 = adaptor.becomeRoot(adaptor.create(IDLIST, "IDLIST"), root_1);

                // Bst.g:38:36: ( identifier )*
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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end idList0

    public static class function_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start function
    // Bst.g:40:1: function : ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | identifier );
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
        identifier_return identifier49 = null;


        Object char_literal42_tree=null;
        Object char_literal43_tree=null;
        Object char_literal44_tree=null;
        Object char_literal45_tree=null;
        Object char_literal46_tree=null;
        Object string_literal47_tree=null;
        Object char_literal48_tree=null;

        try {
            // Bst.g:41:4: ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | identifier )
            int alt5=8;
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
            case IDENTIFIER:
                alt5=8;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("40:1: function : ( '<' | '>' | '=' | '+' | '-' | ':=' | '*' | identifier );", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // Bst.g:41:4: '<'
                    {
                    root_0 = adaptor.nil();

                    char_literal42=input.LT(1);
                    match(input,27,FOLLOW_27_in_function254); 
                    char_literal42_tree = adaptor.create(char_literal42);
                    adaptor.addChild(root_0, char_literal42_tree);


                    }
                    break;
                case 2 :
                    // Bst.g:41:10: '>'
                    {
                    root_0 = adaptor.nil();

                    char_literal43=input.LT(1);
                    match(input,28,FOLLOW_28_in_function258); 
                    char_literal43_tree = adaptor.create(char_literal43);
                    adaptor.addChild(root_0, char_literal43_tree);


                    }
                    break;
                case 3 :
                    // Bst.g:41:16: '='
                    {
                    root_0 = adaptor.nil();

                    char_literal44=input.LT(1);
                    match(input,29,FOLLOW_29_in_function262); 
                    char_literal44_tree = adaptor.create(char_literal44);
                    adaptor.addChild(root_0, char_literal44_tree);


                    }
                    break;
                case 4 :
                    // Bst.g:41:22: '+'
                    {
                    root_0 = adaptor.nil();

                    char_literal45=input.LT(1);
                    match(input,30,FOLLOW_30_in_function266); 
                    char_literal45_tree = adaptor.create(char_literal45);
                    adaptor.addChild(root_0, char_literal45_tree);


                    }
                    break;
                case 5 :
                    // Bst.g:41:28: '-'
                    {
                    root_0 = adaptor.nil();

                    char_literal46=input.LT(1);
                    match(input,31,FOLLOW_31_in_function270); 
                    char_literal46_tree = adaptor.create(char_literal46);
                    adaptor.addChild(root_0, char_literal46_tree);


                    }
                    break;
                case 6 :
                    // Bst.g:41:34: ':='
                    {
                    root_0 = adaptor.nil();

                    string_literal47=input.LT(1);
                    match(input,32,FOLLOW_32_in_function274); 
                    string_literal47_tree = adaptor.create(string_literal47);
                    adaptor.addChild(root_0, string_literal47_tree);


                    }
                    break;
                case 7 :
                    // Bst.g:41:41: '*'
                    {
                    root_0 = adaptor.nil();

                    char_literal48=input.LT(1);
                    match(input,33,FOLLOW_33_in_function278); 
                    char_literal48_tree = adaptor.create(char_literal48);
                    adaptor.addChild(root_0, char_literal48_tree);


                    }
                    break;
                case 8 :
                    // Bst.g:41:47: identifier
                    {
                    root_0 = adaptor.nil();

                    pushFollow(FOLLOW_identifier_in_function282);
                    identifier49=identifier();
                    _fsp--;

                    adaptor.addChild(root_0, identifier49.tree);

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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end function

    public static class stack_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start stack
    // Bst.g:43:1: stack : '{' ( stackitem )+ '}' -> ^( STACK ( stackitem )+ ) ;
    public stack_return stack() throws RecognitionException {   
        stack_return retval = new stack_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal50=null;
        Token char_literal52=null;
        stackitem_return stackitem51 = null;

        List list_stackitem=new ArrayList();
        List list_26=new ArrayList();
        List list_25=new ArrayList();
        Object char_literal50_tree=null;
        Object char_literal52_tree=null;

        try {
            // Bst.g:44:4: ( '{' ( stackitem )+ '}' -> ^( STACK ( stackitem )+ ) )
            // Bst.g:44:4: '{' ( stackitem )+ '}'
            {
            char_literal50=input.LT(1);
            match(input,25,FOLLOW_25_in_stack293); 
            list_25.add(char_literal50);

            // Bst.g:44:8: ( stackitem )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);
                if ( (LA6_0==STRING||(LA6_0>=IDENTIFIER && LA6_0<=QUOTED)||LA6_0==25||(LA6_0>=27 && LA6_0<=33)) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // Bst.g:44:8: stackitem
            	    {
            	    pushFollow(FOLLOW_stackitem_in_stack295);
            	    stackitem51=stackitem();
            	    _fsp--;

            	    list_stackitem.add(stackitem51.tree);

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

            char_literal52=input.LT(1);
            match(input,26,FOLLOW_26_in_stack298); 
            list_26.add(char_literal52);


            // AST REWRITE
            int i_0 = 0;
            retval.tree = root_0;
            root_0 = adaptor.nil();
            // 44:23: -> ^( STACK ( stackitem )+ )
            {
                // Bst.g:44:26: ^( STACK ( stackitem )+ )
                {
                Object root_1 = adaptor.nil();
                root_1 = adaptor.becomeRoot(adaptor.create(STACK, "STACK"), root_1);

                // Bst.g:44:34: ( stackitem )+
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

                retval.tree = adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

       }
        return retval;
    }
    // $ANTLR end stack

    public static class stackitem_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    }

    // $ANTLR start stackitem
    // Bst.g:46:1: stackitem : ( function | STRING | INTEGER | QUOTED | stack );
    public stackitem_return stackitem() throws RecognitionException {   
        stackitem_return retval = new stackitem_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRING54=null;
        Token INTEGER55=null;
        Token QUOTED56=null;
        function_return function53 = null;

        stack_return stack57 = null;


        Object STRING54_tree=null;
        Object INTEGER55_tree=null;
        Object QUOTED56_tree=null;

        try {
            // Bst.g:47:4: ( function | STRING | INTEGER | QUOTED | stack )
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
                    new NoViableAltException("46:1: stackitem : ( function | STRING | INTEGER | QUOTED | stack );", 7, 0, input);

                throw nvae;
            }

            switch (alt7) {
                case 1 :
                    // Bst.g:47:4: function
                    {
                    root_0 = adaptor.nil();

                    pushFollow(FOLLOW_function_in_stackitem317);
                    function53=function();
                    _fsp--;

                    adaptor.addChild(root_0, function53.tree);

                    }
                    break;
                case 2 :
                    // Bst.g:48:4: STRING
                    {
                    root_0 = adaptor.nil();

                    STRING54=input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_stackitem322); 
                    STRING54_tree = adaptor.create(STRING54);
                    adaptor.addChild(root_0, STRING54_tree);


                    }
                    break;
                case 3 :
                    // Bst.g:49:4: INTEGER
                    {
                    root_0 = adaptor.nil();

                    INTEGER55=input.LT(1);
                    match(input,INTEGER,FOLLOW_INTEGER_in_stackitem328); 
                    INTEGER55_tree = adaptor.create(INTEGER55);
                    adaptor.addChild(root_0, INTEGER55_tree);


                    }
                    break;
                case 4 :
                    // Bst.g:50:4: QUOTED
                    {
                    root_0 = adaptor.nil();

                    QUOTED56=input.LT(1);
                    match(input,QUOTED,FOLLOW_QUOTED_in_stackitem334); 
                    QUOTED56_tree = adaptor.create(QUOTED56);
                    adaptor.addChild(root_0, QUOTED56_tree);


                    }
                    break;
                case 5 :
                    // Bst.g:51:4: stack
                    {
                    root_0 = adaptor.nil();

                    pushFollow(FOLLOW_stack_in_stackitem339);
                    stack57=stack();
                    _fsp--;

                    adaptor.addChild(root_0, stack57.tree);

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

                retval.tree = adaptor.rulePostProcessing(root_0);
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
    public static final BitSet FOLLOW_25_in_commands116 = new BitSet(new long[]{0x00000003F8040000L});
    public static final BitSet FOLLOW_function_in_commands119 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands121 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ITERATE_in_commands127 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands130 = new BitSet(new long[]{0x00000003F8040000L});
    public static final BitSet FOLLOW_function_in_commands133 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_commands135 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REVERSE_in_commands141 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_commands144 = new BitSet(new long[]{0x00000003F8040000L});
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
    public static final BitSet FOLLOW_identifier_in_function282 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_stack293 = new BitSet(new long[]{0x00000003FA1C1000L});
    public static final BitSet FOLLOW_stackitem_in_stack295 = new BitSet(new long[]{0x00000003FE1C1000L});
    public static final BitSet FOLLOW_26_in_stack298 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_stackitem317 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stackitem322 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INTEGER_in_stackitem328 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QUOTED_in_stackitem334 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_stack_in_stackitem339 = new BitSet(new long[]{0x0000000000000002L});

}
package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.Hashtable;

import antlr.collections.impl.BitSet;

/**DefineGrammarSymbols is a behavior for the ANTLRParser that adds all
 * the token and rule symbols to the grammar symbol table.
 *
 * Token types are assigned to token symbols in this class also.
 * The token type for a token is done in the order seen (lexically).
 */
public class DefineGrammarSymbols implements ANTLRGrammarParseBehavior {
    // Contains all of the defined parser and lexer Grammar's indexed by name
    protected Hashtable grammars = new Hashtable();
    // Contains all the TokenManagers indexed by name
    protected Hashtable tokenManagers = new Hashtable();
    // Current grammar (parser or lexer)
    protected Grammar grammar;
    // The tool under which this is invoked
    protected Tool tool;
    // The grammar analyzer object
    LLkAnalyzer analyzer;
    // The command-line arguments passed to the tool.
    // This allows each grammar to parse the arguments as it is created
    String[] args;
    // Name for default token manager does not match any valid name
    static final String DEFAULT_TOKENMANAGER_NAME = "*default";
    // Header actions apply to all parsers unless redefined
    // Contains all of the header actions indexed by name
    protected Hashtable headerActions = new Hashtable();
    // Place where preamble is stored until a grammar is defined
    Token thePreambleAction = new CommonToken(Token.INVALID_TYPE, ""); // init to empty token
    // The target language
    String language = "Java";

    protected int numLexers = 0;
    protected int numParsers = 0;
    protected int numTreeParsers = 0;

    public DefineGrammarSymbols(Tool tool_, String[] args_, LLkAnalyzer analyzer_) {
        tool = tool_;
        args = args_;
        analyzer = analyzer_;
    }

    public void _refStringLiteral(Token lit, Token label, int autoGenType, boolean lastInRule) {
        if (!(grammar instanceof LexerGrammar)) {
            // String literals are treated like tokens except by the lexer
            String str = lit.getText();
            if (grammar.tokenManager.getTokenSymbol(str) != null) {
                // string symbol is already defined
                return;
            }
            StringLiteralSymbol sl = new StringLiteralSymbol(str);
            int tt = grammar.tokenManager.nextTokenType();
            sl.setTokenType(tt);
            grammar.tokenManager.define(sl);
        }
    }

    /** Reference a token */
    public void _refToken(Token assignId,
                          Token t,
                          Token label,
                          Token args,
                          boolean inverted,
                          int autoGenType,
                          boolean lastInRule) {
        String id = t.getText();
        if (!grammar.tokenManager.tokenDefined(id)) {
            /*
            // RK: dish out a warning if the token was not defined before.
            tool.warning("Token '" + id + "' defined outside tokens section",
                         tool.grammarFile, t.getLine(), t.getColumn());
            */
            int tt = grammar.tokenManager.nextTokenType();
            TokenSymbol ts = new TokenSymbol(id);
            ts.setTokenType(tt);
            grammar.tokenManager.define(ts);
        }
    }

    /** Abort the processing of a grammar due to syntax errors */
    public void abortGrammar() {
        if (grammar != null && grammar.getClassName() != null) {
            grammars.remove(grammar.getClassName());
        }
        grammar = null;
    }

    public void beginAlt(boolean doAST_) {
    }

    public void beginChildList() {
    }

    // Exception handling
    public void beginExceptionGroup() {
    }

    public void beginExceptionSpec(Token label) {
    }

    public void beginSubRule(Token label, Token start, boolean not) {
    }

    public void beginTree(Token tok) throws SemanticException {
    }

    /** Define a lexer or parser rule */
    public void defineRuleName(Token r,
                               String access,
                               boolean ruleAutoGen,
                               String docComment)
        throws SemanticException {
        String id = r.getText();

        //		if ( Character.isUpperCase(id.charAt(0)) ) {
        if (r.type == ANTLRTokenTypes.TOKEN_REF) {
            // lexer rule
            id = CodeGenerator.encodeLexerRuleName(id);
            // make sure we define it as token identifier also
            if (!grammar.tokenManager.tokenDefined(r.getText())) {
                int tt = grammar.tokenManager.nextTokenType();
                TokenSymbol ts = new TokenSymbol(r.getText());
                ts.setTokenType(tt);
                grammar.tokenManager.define(ts);
            }
        }

        RuleSymbol rs;
        if (grammar.isDefined(id)) {
            // symbol seen before?
            rs = (RuleSymbol)grammar.getSymbol(id);
            // rule just referenced or has it been defined yet?
            if (rs.isDefined()) {
                tool.error("redefinition of rule " + id, grammar.getFilename(), r.getLine(), r.getColumn());
            }
        }
        else {
            rs = new RuleSymbol(id);
            grammar.define(rs);
        }
        rs.setDefined();
        rs.access = access;
        rs.comment = docComment;
    }

    /** Define a token from tokens {...}.
     *  Must be label and literal or just label or just a literal.
     */
    public void defineToken(Token tokname, Token tokliteral) {
        String name = null;
        String literal = null;
        if (tokname != null) {
            name = tokname.getText();
        }
        if (tokliteral != null) {
            literal = tokliteral.getText();
        }
        // System.out.println("defining " + name + " with literal " + literal);
        //
        if (literal != null) {
            StringLiteralSymbol sl = (StringLiteralSymbol)grammar.tokenManager.getTokenSymbol(literal);
            if (sl != null) {
                // This literal is known already.
                // If the literal has no label already, but we can provide
                // one here, then no problem, just map the label to the literal
                // and don't change anything else.
                // Otherwise, labels conflict: error.
                if (name == null || sl.getLabel() != null) {
                    tool.warning("Redefinition of literal in tokens {...}: " + literal, grammar.getFilename(), tokliteral.getLine(), tokliteral.getColumn());
                    return;
                }
                else if (name != null) {
                    // The literal had no label, but new def does.  Set it.
                    sl.setLabel(name);
                    // Also, map the label to the literal.
                    grammar.tokenManager.mapToTokenSymbol(name, sl);
                }
            }
            // if they provide a name/label and that name/label already
            // exists, just hook this literal onto old token.
            if (name != null) {
                TokenSymbol ts = (TokenSymbol)grammar.tokenManager.getTokenSymbol(name);
                if (ts != null) {
                    // watch out that the label is not more than just a token.
                    // If it already has a literal attached, then: conflict.
                    if (ts instanceof StringLiteralSymbol) {
                        tool.warning("Redefinition of token in tokens {...}: " + name, grammar.getFilename(), tokliteral.getLine(), tokliteral.getColumn());
                        return;
                    }
                    // a simple token symbol such as DECL is defined
                    // must convert it to a StringLiteralSymbol with a
                    // label by co-opting token type and killing old
                    // TokenSymbol.  Kill mapping and entry in vector
                    // of token manager.
                    // First, claim token type.
                    int ttype = ts.getTokenType();
                    // now, create string literal with label
                    sl = new StringLiteralSymbol(literal);
                    sl.setTokenType(ttype);
                    sl.setLabel(name);
                    // redefine this critter as a string literal
                    grammar.tokenManager.define(sl);
                    // make sure the label can be used also.
                    grammar.tokenManager.mapToTokenSymbol(name, sl);
                    return;
                }
                // here, literal was labeled but not by a known token symbol.
            }
            sl = new StringLiteralSymbol(literal);
            int tt = grammar.tokenManager.nextTokenType();
            sl.setTokenType(tt);
            sl.setLabel(name);
            grammar.tokenManager.define(sl);
            if (name != null) {
                // make the label point at token symbol too
                grammar.tokenManager.mapToTokenSymbol(name, sl);
            }
        }

        // create a token in the token manager not a literal
        else {
            if (grammar.tokenManager.tokenDefined(name)) {
                tool.warning("Redefinition of token in tokens {...}: " + name, grammar.getFilename(), tokname.getLine(), tokname.getColumn());
                return;
            }
            int tt = grammar.tokenManager.nextTokenType();
            TokenSymbol ts = new TokenSymbol(name);
            ts.setTokenType(tt);
            grammar.tokenManager.define(ts);
        }
    }

    public void endAlt() {
    }

    public void endChildList() {
    }

    public void endExceptionGroup() {
    }

    public void endExceptionSpec() {
    }

    public void endGrammar() {
    }

    /** Called after the optional options section, to compensate for
     * options that may not have been set.
     * This method is bigger than it needs to be, but is much more
     * clear if I delineate all the cases.
     */
    public void endOptions() {
        // NO VOCAB OPTIONS
        if (grammar.exportVocab == null && grammar.importVocab == null) {
            grammar.exportVocab = grammar.getClassName();
            // Can we get initial vocab from default shared vocab?
            if (tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                // Use the already-defined token manager
                grammar.exportVocab = DEFAULT_TOKENMANAGER_NAME;
                TokenManager tm = (TokenManager)tokenManagers.get(DEFAULT_TOKENMANAGER_NAME);
                // System.out.println("No tokenVocabulary for '" + grammar.getClassName() + "', using default '" + tm.getName() + "'");
                grammar.setTokenManager(tm);
                return;
            }
            // no shared vocab for file, make new one
            // System.out.println("No exportVocab for '" + grammar.getClassName() + "', creating default '" + grammar.exportVocab + "'");
            TokenManager tm = new SimpleTokenManager(grammar.exportVocab, tool);
            grammar.setTokenManager(tm);
            // Add the token manager to the list of token managers
            tokenManagers.put(grammar.exportVocab, tm);
            // no default vocab, so make this the default vocab
            tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            return;
        }

        // NO OUTPUT, BUT HAS INPUT VOCAB
        if (grammar.exportVocab == null && grammar.importVocab != null) {
            grammar.exportVocab = grammar.getClassName();
            // first make sure input!=output
            if (grammar.importVocab.equals(grammar.exportVocab)) {
                tool.warning("Grammar " + grammar.getClassName() +
                             " cannot have importVocab same as default output vocab (grammar name); ignored.");
                // kill importVocab option and try again: use default vocab
                grammar.importVocab = null;
                endOptions();
                return;
            }
            // check to see if the vocab is already in memory
            // (defined by another grammar in the file).  Not normal situation.
            if (tokenManagers.containsKey(grammar.importVocab)) {
                // make a copy since we'll be generating a new output vocab
                // and we don't want to affect this one.  Set the name to
                // the default output vocab==classname.
                TokenManager tm = (TokenManager)tokenManagers.get(grammar.importVocab);
                // System.out.println("Duping importVocab of " + grammar.importVocab);
                TokenManager dup = (TokenManager)tm.clone();
                dup.setName(grammar.exportVocab);
                // System.out.println("Setting name to " + grammar.exportVocab);
                dup.setReadOnly(false);
                grammar.setTokenManager(dup);
                tokenManagers.put(grammar.exportVocab, dup);
                return;
            }
            // System.out.println("reading in vocab "+grammar.importVocab);
            // Must be a file, go get it.
            ImportVocabTokenManager tm =
                new ImportVocabTokenManager(grammar,
                                            grammar.importVocab + CodeGenerator.TokenTypesFileSuffix + CodeGenerator.TokenTypesFileExt,
                                            grammar.exportVocab,
                                            tool);
            tm.setReadOnly(false); // since renamed, can write out
            // Add this token manager to the list so its tokens will be generated
            tokenManagers.put(grammar.exportVocab, tm);
            // System.out.println("vocab renamed to default output vocab of "+tm.getName());
            // Assign the token manager to this grammar.
            grammar.setTokenManager(tm);

            // set default vocab if none
            if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            }

            return;
        }

        // OUTPUT VOCAB, BUT NO INPUT VOCAB
        if (grammar.exportVocab != null && grammar.importVocab == null) {
            // share with previous vocab if it exists
            if (tokenManagers.containsKey(grammar.exportVocab)) {
                // Use the already-defined token manager
                TokenManager tm = (TokenManager)tokenManagers.get(grammar.exportVocab);
                // System.out.println("Sharing exportVocab of " + grammar.exportVocab);
                grammar.setTokenManager(tm);
                return;
            }
            // create new output vocab
            // System.out.println("Creating exportVocab " + grammar.exportVocab);
            TokenManager tm = new SimpleTokenManager(grammar.exportVocab, tool);
            grammar.setTokenManager(tm);
            // Add the token manager to the list of token managers
            tokenManagers.put(grammar.exportVocab, tm);
            // set default vocab if none
            if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            }
            return;
        }

        // BOTH INPUT AND OUTPUT VOCAB
        if (grammar.exportVocab != null && grammar.importVocab != null) {
            // don't want input==output
            if (grammar.importVocab.equals(grammar.exportVocab)) {
                tool.error("exportVocab of " + grammar.exportVocab + " same as importVocab; probably not what you want");
            }
            // does the input vocab already exist in memory?
            if (tokenManagers.containsKey(grammar.importVocab)) {
                // make a copy since we'll be generating a new output vocab
                // and we don't want to affect this one.
                TokenManager tm = (TokenManager)tokenManagers.get(grammar.importVocab);
                // System.out.println("Duping importVocab of " + grammar.importVocab);
                TokenManager dup = (TokenManager)tm.clone();
                dup.setName(grammar.exportVocab);
                // System.out.println("Setting name to " + grammar.exportVocab);
                dup.setReadOnly(false);
                grammar.setTokenManager(dup);
                tokenManagers.put(grammar.exportVocab, dup);
                return;
            }
            // Must be a file, go get it.
            ImportVocabTokenManager tm =
                new ImportVocabTokenManager(grammar,
                                            grammar.importVocab + CodeGenerator.TokenTypesFileSuffix + CodeGenerator.TokenTypesFileExt,
                                            grammar.exportVocab,
                                            tool);
            tm.setReadOnly(false); // write it out as we've changed name
            // Add this token manager to the list so its tokens will be generated
            tokenManagers.put(grammar.exportVocab, tm);
            // Assign the token manager to this grammar.
            grammar.setTokenManager(tm);

            // set default vocab if none
            if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            }

            return;
        }
    }

    public void endRule(String r) {
    }

    public void endSubRule() {
    }

    public void endTree() {
    }

    public void hasError() {
    }

    public void noASTSubRule() {
    }

    public void oneOrMoreSubRule() {
    }

    public void optionalSubRule() {
    }

    public void setUserExceptions(String thr) {
    }

    public void refAction(Token action) {
    }

    public void refArgAction(Token action) {
    }

    public void refCharLiteral(Token lit, Token label, boolean inverted, int autoGenType, boolean lastInRule) {
    }

    public void refCharRange(Token t1, Token t2, Token label, int autoGenType, boolean lastInRule) {
    }

    public void refElementOption(Token option, Token value) {
    }

    public void refTokensSpecElementOption(Token tok, Token option, Token value) {
    }

    public void refExceptionHandler(Token exTypeAndName, Token action) {
    }

    // Header action applies to all parsers and lexers.
    public void refHeaderAction(Token name, Token act) {
        String key;

        if (name == null)
            key = "";
        else
            key = StringUtils.stripFrontBack(name.getText(), "\"", "\"");

        // FIXME: depending on the mode the inserted header actions should
        // be checked for sanity.
        if (headerActions.containsKey(key)) {
            if (key.equals(""))
                tool.error(act.getLine() + ": header action already defined");
            else
                tool.error(act.getLine() + ": header action '" + key + "' already defined");
        }
        headerActions.put(key, act);
    }

    public String getHeaderAction(String name) {
        Token t = (Token)headerActions.get(name);
        if (t == null) {
            return "";
        }
        return t.getText();
    }

    public void refInitAction(Token action) {
    }

    public void refMemberAction(Token act) {
    }

    public void refPreambleAction(Token act) {
        thePreambleAction = act;
    }

    public void refReturnAction(Token returnAction) {
    }

    public void refRule(Token idAssign,
                        Token r,
                        Token label,
                        Token args,
                        int autoGenType) {
        String id = r.getText();
        //		if ( Character.isUpperCase(id.charAt(0)) ) { // lexer rule?
        if (r.type == ANTLRTokenTypes.TOKEN_REF) {
            // lexer rule?
            id = CodeGenerator.encodeLexerRuleName(id);
        }
        if (!grammar.isDefined(id)) {
            grammar.define(new RuleSymbol(id));
        }
    }

    public void refSemPred(Token pred) {
    }

    public void refStringLiteral(Token lit,
                                 Token label,
                                 int autoGenType,
                                 boolean lastInRule) {
        _refStringLiteral(lit, label, autoGenType, lastInRule);
    }

    /** Reference a token */
    public void refToken(Token assignId, Token t, Token label, Token args,
                         boolean inverted, int autoGenType, boolean lastInRule) {
        _refToken(assignId, t, label, args, inverted, autoGenType, lastInRule);
    }

    public void refTokenRange(Token t1, Token t2, Token label, int autoGenType, boolean lastInRule) {
        // ensure that the DefineGrammarSymbols methods are called; otherwise a range addes more
        // token refs to the alternative by calling MakeGrammar.refToken etc...
        if (t1.getText().charAt(0) == '"') {
            refStringLiteral(t1, null, GrammarElement.AUTO_GEN_NONE, lastInRule);
        }
        else {
            _refToken(null, t1, null, null, false, GrammarElement.AUTO_GEN_NONE, lastInRule);
        }
        if (t2.getText().charAt(0) == '"') {
            _refStringLiteral(t2, null, GrammarElement.AUTO_GEN_NONE, lastInRule);
        }
        else {
            _refToken(null, t2, null, null, false, GrammarElement.AUTO_GEN_NONE, lastInRule);
        }
    }

    public void refTreeSpecifier(Token treeSpec) {
    }

    public void refWildcard(Token t, Token label, int autoGenType) {
    }

    /** Get ready to process a new grammar */
    public void reset() {
        grammar = null;
    }

    public void setArgOfRuleRef(Token argaction) {
    }

    /** Set the character vocabulary for a lexer */
    public void setCharVocabulary(BitSet b) {
        // grammar should enforce that this is only called for lexer
        ((LexerGrammar)grammar).setCharVocabulary(b);
    }

    /** setFileOption: Associate an option value with a key.
     * This applies to options for an entire grammar file.
     * @param key The token containing the option name
     * @param value The token containing the option value.
     */
    public void setFileOption(Token key, Token value, String filename) {
        if (key.getText().equals("language")) {
            if (value.getType() == ANTLRParser.STRING_LITERAL) {
                language = StringUtils.stripBack(StringUtils.stripFront(value.getText(), '"'), '"');
            }
            else if (value.getType() == ANTLRParser.TOKEN_REF || value.getType() == ANTLRParser.RULE_REF) {
                language = value.getText();
            }
            else {
                tool.error("language option must be string or identifier", filename, value.getLine(), value.getColumn());
            }
        }
        else if (key.getText().equals("mangleLiteralPrefix")) {
            if (value.getType() == ANTLRParser.STRING_LITERAL) {
                tool.literalsPrefix = StringUtils.stripFrontBack(value.getText(), "\"", "\"");
            }
            else {
                tool.error("mangleLiteralPrefix option must be string", filename, value.getLine(), value.getColumn());
            }
        }
        else if (key.getText().equals("upperCaseMangledLiterals")) {
            if (value.getText().equals("true")) {
                tool.upperCaseMangledLiterals = true;
            }
            else if (value.getText().equals("false")) {
                tool.upperCaseMangledLiterals = false;
            }
            else {
                grammar.antlrTool.error("Value for upperCaseMangledLiterals must be true or false", filename, key.getLine(), key.getColumn());
            }
        }
        else if (	key.getText().equals("namespaceStd")   ||
            	   key.getText().equals("namespaceAntlr") ||
            	   key.getText().equals("genHashLines")
            	  ) {
            if (!language.equals("Cpp")) {
                tool.error(key.getText() + " option only valid for C++", filename, key.getLine(), key.getColumn());
            }
            else {
                if (key.getText().equals("noConstructors")) {
                    if (!(value.getText().equals("true") || value.getText().equals("false")))
                        tool.error("noConstructors option must be true or false", filename, value.getLine(), value.getColumn());
                    tool.noConstructors = value.getText().equals("true");
                } else if (key.getText().equals("genHashLines")) {
                    if (!(value.getText().equals("true") || value.getText().equals("false")))
                        tool.error("genHashLines option must be true or false", filename, value.getLine(), value.getColumn());
                    tool.genHashLines = value.getText().equals("true");
                }
                else {
                    if (value.getType() != ANTLRParser.STRING_LITERAL) {
                        tool.error(key.getText() + " option must be a string", filename, value.getLine(), value.getColumn());
                    }
                    else {
                        if (key.getText().equals("namespaceStd"))
                            tool.namespaceStd = value.getText();
                        else if (key.getText().equals("namespaceAntlr"))
                            tool.namespaceAntlr = value.getText();
                    }
                }
            }
        }
        else if ( key.getText().equals("namespace") ) {
            if ( !language.equals("Cpp") && !language.equals("CSharp") )
            {
                tool.error(key.getText() + " option only valid for C++ and C# (a.k.a CSharp)", filename, key.getLine(), key.getColumn());
            }
            else
            {
                 if (value.getType() != ANTLRParser.STRING_LITERAL)
                 {
                 		tool.error(key.getText() + " option must be a string", filename, value.getLine(), value.getColumn());
                 }
                 else {
                     if (key.getText().equals("namespace"))
                         tool.setNameSpace(value.getText());
                 }
            }
        }
        else {
            tool.error("Invalid file-level option: " + key.getText(), filename, key.getLine(), value.getColumn());
        }
    }

    /** setGrammarOption: Associate an option value with a key.
     * This function forwards to Grammar.setOption for some options.
     * @param key The token containing the option name
     * @param value The token containing the option value.
     */
    public void setGrammarOption(Token key, Token value) {
        if (key.getText().equals("tokdef") || key.getText().equals("tokenVocabulary")) {
            tool.error("tokdef/tokenVocabulary options are invalid >= ANTLR 2.6.0.\n" +
                       "  Use importVocab/exportVocab instead.  Please see the documentation.\n" +
                       "  The previous options were so heinous that Terence changed the whole\n" +
                       "  vocabulary mechanism; it was better to change the names rather than\n" +
                       "  subtly change the functionality of the known options.  Sorry!", grammar.getFilename(), value.getLine(), value.getColumn());
        }
        else if (key.getText().equals("literal") &&
            grammar instanceof LexerGrammar) {
            tool.error("the literal option is invalid >= ANTLR 2.6.0.\n" +
                       "  Use the \"tokens {...}\" mechanism instead.",
                       grammar.getFilename(), value.getLine(), value.getColumn());
        }
        else if (key.getText().equals("exportVocab")) {
            // Set the token manager associated with the parser
            if (value.getType() == ANTLRParser.RULE_REF || value.getType() == ANTLRParser.TOKEN_REF) {
                grammar.exportVocab = value.getText();
            }
            else {
                tool.error("exportVocab must be an identifier", grammar.getFilename(), value.getLine(), value.getColumn());
            }
        }
        else if (key.getText().equals("importVocab")) {
            if (value.getType() == ANTLRParser.RULE_REF || value.getType() == ANTLRParser.TOKEN_REF) {
                grammar.importVocab = value.getText();
            }
            else {
                tool.error("importVocab must be an identifier", grammar.getFilename(), value.getLine(), value.getColumn());
            }
        }
        else {
            // Forward all unrecognized options to the grammar
            grammar.setOption(key.getText(), value);
        }
    }

    public void setRuleOption(Token key, Token value) {
    }

    public void setSubruleOption(Token key, Token value) {
    }

    /** Start a new lexer */
    public void startLexer(String file, Token name, String superClass, String doc) {
        if (numLexers > 0) {
            tool.panic("You may only have one lexer per grammar file: class " + name.getText());
        }
        numLexers++;
        reset();
        //System.out.println("Processing lexer '" + name.getText() + "'");
        // Does the lexer already exist?
        Grammar g = (Grammar)grammars.get(name);
        if (g != null) {
            if (!(g instanceof LexerGrammar)) {
                tool.panic("'" + name.getText() + "' is already defined as a non-lexer");
            }
            else {
                tool.panic("Lexer '" + name.getText() + "' is already defined");
            }
        }
        else {
            // Create a new lexer grammar
            LexerGrammar lg = new LexerGrammar(name.getText(), tool, superClass);
            lg.comment = doc;
            lg.processArguments(args);
            lg.setFilename(file);
            grammars.put(lg.getClassName(), lg);
            // Use any preamble action
            lg.preambleAction = thePreambleAction;
            thePreambleAction = new CommonToken(Token.INVALID_TYPE, "");
            // This is now the current grammar
            grammar = lg;
        }
    }

    /** Start a new parser */
    public void startParser(String file, Token name, String superClass, String doc) {
        if (numParsers > 0) {
            tool.panic("You may only have one parser per grammar file: class " + name.getText());
        }
        numParsers++;
        reset();
        //System.out.println("Processing parser '" + name.getText() + "'");
        // Is this grammar already defined?
        Grammar g = (Grammar)grammars.get(name);
        if (g != null) {
            if (!(g instanceof ParserGrammar)) {
                tool.panic("'" + name.getText() + "' is already defined as a non-parser");
            }
            else {
                tool.panic("Parser '" + name.getText() + "' is already defined");
            }
        }
        else {
            // Create a new grammar
            grammar = new ParserGrammar(name.getText(), tool, superClass);
            grammar.comment = doc;
            grammar.processArguments(args);
            grammar.setFilename(file);
            grammars.put(grammar.getClassName(), grammar);
            // Use any preamble action
            grammar.preambleAction = thePreambleAction;
            thePreambleAction = new CommonToken(Token.INVALID_TYPE, "");
        }
    }

    /** Start a new tree-walker */
    public void startTreeWalker(String file, Token name, String superClass, String doc) {
        if (numTreeParsers > 0) {
            tool.panic("You may only have one tree parser per grammar file: class " + name.getText());
        }
        numTreeParsers++;
        reset();
        //System.out.println("Processing tree-walker '" + name.getText() + "'");
        // Is this grammar already defined?
        Grammar g = (Grammar)grammars.get(name);
        if (g != null) {
            if (!(g instanceof TreeWalkerGrammar)) {
                tool.panic("'" + name.getText() + "' is already defined as a non-tree-walker");
            }
            else {
                tool.panic("Tree-walker '" + name.getText() + "' is already defined");
            }
        }
        else {
            // Create a new grammar
            grammar = new TreeWalkerGrammar(name.getText(), tool, superClass);
            grammar.comment = doc;
            grammar.processArguments(args);
            grammar.setFilename(file);
            grammars.put(grammar.getClassName(), grammar);
            // Use any preamble action
            grammar.preambleAction = thePreambleAction;
            thePreambleAction = new CommonToken(Token.INVALID_TYPE, "");
        }
    }

    public void synPred() {
    }

    public void zeroOrMoreSubRule() {
    }
}

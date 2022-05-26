package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

public class BstVM {

    protected static final int FALSE = 0;
    protected static final int TRUE = 1;

    private final ParseTree tree;

    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Integer> integers = new HashMap<>();
    private final Map<String, BstFunctions.BstFunction> functions = new HashMap<>();
    private final Stack<Object> stack = new Stack<>();

    private final BstFunctions bstFunctions;
    private final StringBuilder bbl;

    private Path path;

    public BstVM(Path path) throws RecognitionException, IOException {
        this(CharStreams.fromPath(path));
        this.path = path;
    }

    public BstVM(String s) throws RecognitionException {
        this(CharStreams.fromString(s));
    }

    private BstVM(CharStream bst) throws RecognitionException {
        this(charStream2CommonTree(bst));
    }

    private BstVM(ParseTree tree) {
        this.tree = tree;
        bbl = new StringBuilder();
        bstFunctions = new BstFunctions(strings, integers, functions, stack, bbl);
    }

    private static ParseTree charStream2CommonTree(CharStream query) {
        BstLexer lexer = new BstLexer(query);
        lexer.removeErrorListeners();
        lexer.addErrorListener(VM.ThrowingErrorListener.INSTANCE);
        BstParser parser = new BstParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.start();
    }

    private static class ThrowingErrorListener extends BaseErrorListener {
        public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    protected static class Identifier {

        public final String name;

        public Identifier(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private class BstVMVisitor extends BstBaseVisitor<Integer> {
        @Override
        public Integer visitStringsCommand(BstParser.StringsCommandContext ctx) {
            if (ctx.ids.getChildCount() > 20) {
                throw new ParseCancellationException("Strings limit reached");
            }

            for (int i = 0; i < ctx.ids.getChildCount(); i++) {
                strings.put(ctx.ids.getChild(i).getText(), null);
            }
            return BstVM.TRUE;
        }

        @Override
        public Integer visitIntegersCommand(BstParser.IntegersCommandContext ctx) {
            for (int i = 0; i < ctx.ids.getChildCount(); i++) {
                integers.put(ctx.ids.getChild(i).getText(), 0);
            }
            return BstVM.TRUE;
        }

        @Override
        public Integer visitMacroCommand(BstParser.MacroCommandContext ctx) {
            functions.put(ctx.id.getText(), new MacroFunction(ctx.repl.getText()));
            return BstVM.TRUE;
        }

        private class MacroFunction implements BstFunctions.BstFunction {
            private final String replacement;

            MacroFunction(String replacement) {
                this.replacement = replacement;
            }

            @Override
            public void execute(BstEntry context) {
                stack.push(replacement);
            }
        }
    }
}

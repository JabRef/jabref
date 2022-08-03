package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

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

    protected static final Integer FALSE = 0;
    protected static final Integer TRUE = 1;

    protected final ParseTree tree;
    protected BstVMContext latestContext; // for testing

    private Path path = null;

    public BstVM(Path path) throws RecognitionException, IOException {
        this(CharStreams.fromPath(path));
        this.path = path;
    }

    public BstVM(String s) throws RecognitionException {
        this(CharStreams.fromString(s));
    }

    protected BstVM(CharStream bst) throws RecognitionException {
        this(charStream2CommonTree(bst));
    }

    private BstVM(ParseTree tree) {
        this.tree = tree;
    }

    private static ParseTree charStream2CommonTree(CharStream query) {
        BstLexer lexer = new BstLexer(query);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        BstParser parser = new BstParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.bstFile();
    }

    /**
     * Transforms the given list of BibEntries to a rendered list of references using the parsed bst program
     *
     * @param bibEntries  list of entries to convert
     * @param bibDatabase (may be null) the bibDatabase used for resolving strings / crossref
     * @return list of references in plain text form
     */
    public String render(Collection<BibEntry> bibEntries, BibDatabase bibDatabase) {
        Objects.requireNonNull(bibEntries);

        List<BstEntry> entries = new ArrayList<>(bibEntries.size());
        for (BibEntry entry : bibEntries) {
            entries.add(new BstEntry(entry));
        }

        StringBuilder resultBuffer = new StringBuilder();

        BstVMContext bstVMContext = new BstVMContext(entries, bibDatabase, path);
        bstVMContext.functions().putAll(new BstFunctions(bstVMContext, resultBuffer).getBuiltInFunctions());
        bstVMContext.integers().put("entry.max$", Integer.MAX_VALUE);
        bstVMContext.integers().put("global.max$", Integer.MAX_VALUE);

        BstVMVisitor bstVMVisitor = new BstVMVisitor(bstVMContext, resultBuffer);
        bstVMVisitor.visit(tree);

        latestContext = bstVMContext;

        return resultBuffer.toString();
    }

    public String render(Collection<BibEntry> bibEntries) {
        return render(bibEntries, null);
    }

    protected Stack<Object> getStack() {
        if (latestContext != null) {
            return latestContext.stack();
        } else {
            throw new BstVMException("BstVM must have rendered at least once to provide the latest stack");
        }
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
}

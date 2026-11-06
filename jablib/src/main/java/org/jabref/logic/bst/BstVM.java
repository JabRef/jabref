package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Parsed executable program of one specific `.bst` file.
///
/// [BstVM] cannot be used as a static, style-agnostic machine because each instance holds the
/// parse tree of one concrete `.bst` program. It also keeps the latest execution context for
/// testing/debugging, so callers should treat it as style-specific runtime state rather than a
/// global utility.
@NullMarked
public class BstVM {

    protected static final Integer FALSE = 0;
    protected static final Integer TRUE = 1;

    protected final ParseTree tree;
    @Nullable private BstVMContext latestContext; // for testing

    @Nullable private Path path;

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

    /// Transforms the given list of BibEntries to a rendered list of references using the parsed bst program
    ///
    /// @param bibEntries  list of entries to convert
    /// @param bibDatabase the bibDatabase used for resolving strings / crossref
    /// @return list of references in plain text form
    public String render(List<BibEntry> bibEntries, BibDatabase bibDatabase) {
        // needs to be modifiable due to sort operations later
        List<BstEntry> entries = bibEntries.stream().map(BstEntry::new).collect(Collectors.toList());

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

    public String render(List<BibEntry> bibEntries) {
        return render(bibEntries, new BibDatabase(bibEntries));
    }

    public boolean hasSortCommand() {
        return Trees.getDescendants(tree).stream().anyMatch(BstParser.SortCommandContext.class::isInstance);
    }

    /// Returns the names of the fields declared in the `ENTRY` command, i.e., the fields the style reads.
    /// Names are lower-cased, because BibTeX field names are case-insensitive.
    public Set<String> getEntryFields() {
        return Trees.getDescendants(tree).stream()
                    .filter(BstParser.EntryCommandContext.class::isInstance)
                    .map(BstParser.EntryCommandContext.class::cast)
                    .flatMap(entryCommand -> entryCommand.idListOpt(0).identifier().stream())
                    .map(identifier -> identifier.getText().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
    }

    /// Selects fields that can supply values to this style, including aliases and date parts.
    public Optional<Predicate<Field>> getFieldSelection() {
        Set<String> entryFields = getEntryFields();
        if (entryFields.isEmpty()) {
            return Optional.empty();
        }
        Set<Field> dateFields = Set.of(StandardField.DATE, StandardField.YEAR, StandardField.MONTH, StandardField.DAY);
        return Optional.of(field -> {
            if (field == StandardField.CROSSREF) {
                return true;
            }
            Stream<Field> candidates = dateFields.contains(field)
                                       ? dateFields.stream()
                                       : Stream.concat(Stream.of(field), Optional.ofNullable(EntryConverter.FIELD_ALIASES.get(field)).stream());
            return candidates.map(candidate -> candidate.getName().toLowerCase(Locale.ROOT)).anyMatch(entryFields::contains);
        });
    }

    @VisibleForTesting
    BstVMContext getContext() {
        if (latestContext != null) {
            return latestContext;
        } else {
            throw new BstVMException("BstVM must have rendered at least once to provide the latest context");
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

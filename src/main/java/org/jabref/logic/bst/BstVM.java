package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

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
    private List<BstEntry> entries = new ArrayList<>();

    private StringBuilder bbl;
    private Path path;
    private BibDatabase bibDatabase;

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

    /**
     * Transforms the given list of BibEntries to a rendered list of references using the underlying bst file
     *
     * @param bibEntries  list of entries to convert
     * @param bibDatabase (may be null) the bibDatabase used for resolving strings / crossref
     * @return list of references in plain text form
     */
    public String render(Collection<BibEntry> bibEntries, BibDatabase bibDatabase) {
        Objects.requireNonNull(bibEntries);
        this.bibDatabase = bibDatabase;

        reset();

        entries = new ArrayList<>(bibEntries.size());
        for (BibEntry entry : bibEntries) {
            entries.add(new BstEntry(entry));
        }

        return bbl.toString();
    }

    private void reset() {
        bbl = new StringBuilder();

        strings.clear();

        integers.clear();
        integers.put("entry.max$", Integer.MAX_VALUE);
        integers.put("global.max$", Integer.MAX_VALUE);

        functions.clear();
        functions.putAll(new BstFunctions(strings, integers, functions, stack, bbl).getBuiltInFunction());

        stack.clear();
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

        @Override
        public Integer visitReadCommand(BstParser.ReadCommandContext ctx) {
            FieldWriter fieldWriter = new FieldWriter(new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences()));
            for (BstEntry e : entries) {
                for (Map.Entry<String, String> mEntry : e.fields.entrySet()) {
                    Field field = FieldFactory.parseField(mEntry.getKey());
                    String fieldValue = e.entry.getResolvedFieldOrAlias(field, bibDatabase)
                                               .map(content -> {
                                                   try {
                                                       String result = fieldWriter.write(field, content);
                                                       if (result.startsWith("{")) {
                                                           // Strip enclosing {} from the output
                                                           return result.substring(1, result.length() - 1);
                                                       }
                                                       if (field == StandardField.MONTH) {
                                                           // We don't have the internal BibTeX strings at hand.
                                                           // Thus, we look up the full month name in the generic table.
                                                           return Month.parse(result)
                                                                       .map(Month::getFullName)
                                                                       .orElse(result);
                                                       }
                                                       return result;
                                                   } catch (
                                                           InvalidFieldValueException invalidFieldValueException) {
                                                       // in case there is something wrong with the content, just return the content itself
                                                       return content;
                                                   }
                                               })
                                               .orElse(null);
                    mEntry.setValue(fieldValue);
                }
            }

            for (BstEntry e : entries) {
                if (!e.fields.containsKey(StandardField.CROSSREF.getName())) {
                    e.fields.put(StandardField.CROSSREF.getName(), null);
                }
            }

            return BstVM.TRUE;
        }

        @Override
        public Integer visitEntryCommand(BstParser.EntryCommandContext ctx) {
            // Entry contains 3 optionally filled identifier lists:
            // Fields, Integers and Strings

            ParseTree entryFields = ctx.getChild(1);
            for (int i = 0; i < entryFields.getChildCount(); i++) {
                for (BstEntry entry : entries) {
                    entry.fields.put(entryFields.getChild(i).getText(), null);
                }
            }

            ParseTree entryIntegers = ctx.getChild(2);
            for (int i = 0; i < entryIntegers.getChildCount(); i++) {
                for (BstEntry entry : entries) {
                    entry.localIntegers.put(entryIntegers.getChild(i).getText(), 0);
                }
            }

            ParseTree entryStrings = ctx.getChild(3);
            for (int i = 0; i < entryStrings.getChildCount(); i++) {
                for (BstEntry entry : entries) {
                    entry.fields.put(entryStrings.getChild(i).getText(), null);
                }
            }

            for (BstEntry entry : entries) {
                entry.localStrings.put("sort.key$", null);
            }

            return BstVM.TRUE;
        }

        @Override
        public Integer visitSortCommand(BstParser.SortCommandContext ctx) {
            entries.sort(Comparator.comparing(o -> (o.localStrings.get("sort.key$"))));
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

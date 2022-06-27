package org.jabref.logic.bst;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

class BstVMVisitor extends BstBaseVisitor<Integer> {
    private final BstVMContext bstVMContext;
    private final StringBuilder bbl;

    private BstEntry selectedBstEntry = null;

    public record Identifier(String name) {
    }

    public BstVMVisitor(BstVMContext bstVMContext, StringBuilder bbl) {
        this.bstVMContext = bstVMContext;
        this.bbl = bbl;
    }

    @Override
    public Integer visitStringsCommand(BstParser.StringsCommandContext ctx) {
        if (ctx.ids.identifier().size() > 20) {
            throw new BstVMException("Strings limit reached");
        }

        for (BstParser.IdentifierContext identifierContext : ctx.ids.identifier()) {
            bstVMContext.strings().put(identifierContext.getText(), null);
        }
        return BstVM.TRUE;
    }

    @Override
    public Integer visitIntegersCommand(BstParser.IntegersCommandContext ctx) {
        for (BstParser.IdentifierContext identifierContext : ctx.ids.identifier()) {
            bstVMContext.integers().put(identifierContext.getText(), 0);
        }
        return BstVM.TRUE;
    }

    @Override
    public Integer visitFunctionCommand(BstParser.FunctionCommandContext ctx) {
        bstVMContext.functions().put(ctx.id.getText(), new BstVMFunction<>(ctx.function));
        return BstVM.TRUE;
    }

    @Override
    public Integer visitMacroCommand(BstParser.MacroCommandContext ctx) {
        bstVMContext.functions().put(ctx.id.getText(), new BstVMFunction<>(ctx.repl.getText()));
        return BstVM.TRUE;
    }

    @Override
    public Integer visitReadCommand(BstParser.ReadCommandContext ctx) {
        FieldWriter fieldWriter = new FieldWriter(new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences()));
        for (BstEntry e : bstVMContext.entries()) {
            for (Map.Entry<String, String> mEntry : e.fields.entrySet()) {
                Field field = FieldFactory.parseField(mEntry.getKey());
                String fieldValue = e.entry.getResolvedFieldOrAlias(field, bstVMContext.bibDatabase())
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

        for (BstEntry e : bstVMContext.entries()) {
            if (!e.fields.containsKey(StandardField.CROSSREF.getName())) {
                e.fields.put(StandardField.CROSSREF.getName(), null);
            }
        }

        return BstVM.TRUE;
    }

    @Override
    public Integer visitExecuteCommand(BstParser.ExecuteCommandContext ctx) {
        this.selectedBstEntry = null;
        visit(ctx.bstFunction());

        return BstVM.TRUE;
    }

    @Override
    public Integer visitIterateCommand(BstParser.IterateCommandContext ctx) {
        for (BstEntry entry : bstVMContext.entries()) {
            this.selectedBstEntry = entry;
            visit(ctx.bstFunction());
        }

        return BstVM.TRUE;
    }

    @Override
    public Integer visitReverseCommand(BstParser.ReverseCommandContext ctx) {
        ListIterator<BstEntry> i = bstVMContext.entries().listIterator(bstVMContext.entries().size());
        while (i.hasPrevious()) {
            this.selectedBstEntry = i.previous();
            visit(ctx.bstFunction());
        }

        return BstVM.TRUE;
    }

    @Override
    public Integer visitEntryCommand(BstParser.EntryCommandContext ctx) {
        // ENTRY command contains 3 optionally filled identifier lists:
        // Fields, Integers and Strings

        BstParser.IdListOptContext entryFields = ctx.idListOpt(0);
        for (BstParser.IdentifierContext identifierContext : entryFields.identifier()) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.fields.put(identifierContext.getText(), null);
            }
        }

        BstParser.IdListOptContext entryIntegers = ctx.idListOpt(1);
        for (BstParser.IdentifierContext identifierContext : entryIntegers.identifier()) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.localIntegers.put(identifierContext.getText(), 0);
            }
        }

        BstParser.IdListOptContext entryStrings = ctx.idListOpt(2);
        for (BstParser.IdentifierContext identifierContext : entryStrings.identifier()) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.localStrings.put(identifierContext.getText(), null);
            }
        }

        for (BstEntry entry : bstVMContext.entries()) {
            entry.localStrings.put("sort.key$", null);
        }

        return BstVM.TRUE;
    }

    @Override
    public Integer visitSortCommand(BstParser.SortCommandContext ctx) {
        bstVMContext.entries().sort(Comparator.comparing(o -> (o.localStrings.get("sort.key$"))));
        return BstVM.TRUE;
    }

    @Override
    public Integer visitIdentifier(BstParser.IdentifierContext ctx) {
        String name = ctx.IDENTIFIER().getText();

        if (selectedBstEntry != null) {
            if (selectedBstEntry.fields.containsKey(name)) {
                bstVMContext.stack().push(selectedBstEntry.fields.get(name));
                return BstVM.TRUE;
            }
            if (selectedBstEntry.localStrings.containsKey(name)) {
                bstVMContext.stack().push(selectedBstEntry.localStrings.get(name));
                return BstVM.TRUE;
            }
            if (selectedBstEntry.localIntegers.containsKey(name)) {
                bstVMContext.stack().push(selectedBstEntry.localIntegers.get(name));
                return BstVM.TRUE;
            }
        }
        if (bstVMContext.strings().containsKey(name)) {
            bstVMContext.stack().push(bstVMContext.strings().get(name));
            return BstVM.TRUE;
        }
        if (bstVMContext.integers().containsKey(name)) {
            bstVMContext.stack().push(bstVMContext.integers().get(name));
            return BstVM.TRUE;
        }

        if (bstVMContext.functions().containsKey(name)) {
            bstVMContext.functions().get(name).execute(this, ctx);
        }

        throw new BstVMException("No matching identifier found: " + name);
    }

    @Override
    public Integer visitBstFunction(BstParser.BstFunctionContext ctx) {
        String name = ctx.getChild(0).getText();
        if (bstVMContext.functions().containsKey(name)) {
            bstVMContext.functions().get(name).execute(this, ctx, selectedBstEntry);
        } else {
            visit(ctx.getChild(0));
        }

        return BstVM.TRUE;
    }

    @Override
    public Integer visitStackitem(BstParser.StackitemContext ctx) {
        for (ParseTree childNode : ctx.children) {
            if (childNode instanceof TerminalNode token) {
                switch (token.getSymbol().getType()) {
                    case BstParser.STRING -> {
                        String s = token.getText();
                        bstVMContext.stack().push(s.substring(1, s.length() - 1));
                    }
                    case BstParser.INTEGER ->
                            bstVMContext.stack().push(Integer.parseInt(token.getText().substring(1)));
                    case BstParser.QUOTED ->
                            bstVMContext.stack().push(new Identifier(token.getText().substring(1)));
                }
            } else {
                visit(childNode);
            }
        }
        return BstVM.TRUE;
    }

    public record BstVMFunction<T>(T expression) implements BstFunctions.BstFunction {

        @Override
        public void execute(BstVMVisitor visitor, ParserRuleContext bstFunctionContext) {
            if (expression instanceof String str) {
                visitor.bstVMContext.stack().push(str.substring(1, str.length() - 1)); // Macro
            } else if (expression instanceof ParserRuleContext ctx) {
                visitor.visit(ctx); // Function
            } else if (expression instanceof Integer integer) {
                visitor.bstVMContext.stack().push(integer);
            } else if (expression instanceof Identifier identifier) {
                // Fixme: should push parsed identifier
            }
        }
    }
}

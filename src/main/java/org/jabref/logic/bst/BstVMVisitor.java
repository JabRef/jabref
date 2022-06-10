package org.jabref.logic.bst;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

class BstVMVisitor extends BstBaseVisitor<Integer> {
    private final BstVMContext bstVMContext;

    private final Stack<Object> stack = new Stack<>();
    private final StringBuilder bbl;

    private BstEntry currentBstEntry;

    public record Identifier(String name) {
    }

    public BstVMVisitor(BstVMContext bstVMContext, StringBuilder bbl) {
        this.bstVMContext = bstVMContext;
        this.bbl = bbl;
    }

    @Override
    public Integer visitStringsCommand(BstParser.StringsCommandContext ctx) {
        if (ctx.ids.getChildCount() > 20) {
            throw new ParseCancellationException("Strings limit reached");
        }

        for (int i = 0; i < ctx.ids.getChildCount(); i++) {
            bstVMContext.strings().put(ctx.ids.getChild(i).getText(), null);
        }
        return BstVM.TRUE;
    }

    @Override
    public Integer visitIntegersCommand(BstParser.IntegersCommandContext ctx) {
        for (int i = 0; i < ctx.ids.getChildCount(); i++) {
            bstVMContext.integers().put(ctx.ids.getChild(i).getText(), 0);
        }
        return BstVM.TRUE;
    }

    @Override
    public Integer visitMacroCommand(BstParser.MacroCommandContext ctx) {
        bstVMContext.functions().put(ctx.id.getText(), new MacroFunction(ctx.repl.getText()));
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
    public Integer visitEntryCommand(BstParser.EntryCommandContext ctx) {
        // ENTRY command contains 3 optionally filled identifier lists:
        // Fields, Integers and Strings

        ParseTree entryFields = ctx.getChild(1);
        for (int i = 0; i < entryFields.getChildCount(); i++) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.fields.put(entryFields.getChild(i).getText(), null);
            }
        }

        ParseTree entryIntegers = ctx.getChild(2);
        for (int i = 0; i < entryIntegers.getChildCount(); i++) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.localIntegers.put(entryIntegers.getChild(i).getText(), 0);
            }
        }

        ParseTree entryStrings = ctx.getChild(3);
        for (int i = 0; i < entryStrings.getChildCount(); i++) {
            for (BstEntry entry : bstVMContext.entries()) {
                entry.localStrings.put(entryStrings.getChild(i).getText(), null);
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
    public Integer visitBstFunction(BstParser.BstFunctionContext ctx) {
        bstVMContext.functions().get(ctx.getChild(0).getText()).execute(this, ctx, currentBstEntry);
        return BstVM.TRUE;
    }

    private class MacroFunction implements BstFunctions.BstFunction {
        private final String replacement;

        MacroFunction(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public void execute(BstVMVisitor visitor, BstParser.BstFunctionContext functionContext) {
            stack.push(replacement);
        }
    }
}

package org.jabref.logic.bst;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.antlr.v4.runtime.RecognitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestBstVMVisitor {

    @Test
    public void testVisitStringsCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("STRINGS { test.string1 test.string2 test.string3 }");

        vm.render(Collections.emptyList());

        Map<String, String> strList = vm.getStrings();
        assertTrue(strList.containsKey("test.string1"));
        assertNull(strList.get("test.string1"));
        assertTrue(strList.containsKey("test.string2"));
        assertNull(strList.get("test.string2"));
        assertTrue(strList.containsKey("test.string3"));
        assertNull(strList.get("test.string3"));
    }

    @Test
    public void testVisitIntegersCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("INTEGERS { variable.a variable.b variable.c }");

        vm.render(Collections.emptyList());

        Map<String, Integer> integersList = vm.getIntegers();
        assertTrue(integersList.containsKey("variable.a"));
        assertEquals(0, integersList.get("variable.a"));
        assertTrue(integersList.containsKey("variable.b"));
        assertEquals(0, integersList.get("variable.b"));
        assertTrue(integersList.containsKey("variable.c"));
        assertEquals(0, integersList.get("variable.c"));
    }

    @Test
    void testVisitFunctionCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                FUNCTION { test.func } { #1 'test.var := }
                EXECUTE { test.func }
                """);

        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.getFunctions();
        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));
    }

    @Test
    void testVisitMacroCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                MACRO { jan } { "January" }
                EXECUTE { jan }
                """);

        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.getFunctions();
        assertTrue(functions.containsKey("jan"));
        assertNotNull(functions.get("jan"));
        assertEquals("January", vm.latestContext.stack().pop());
        assertTrue(vm.latestContext.stack().isEmpty());
    }

    @Test
    void testVisitEntryCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("ENTRY { address author title type } { variable } { label }");
        List<BibEntry> testEntries = List.of(TestBstVM.defaultTestEntry());

        vm.render(testEntries);

        BstEntry bstEntry = vm.getEntries().get(0);
        assertTrue(bstEntry.fields.containsKey("address"));
        assertTrue(bstEntry.fields.containsKey("author"));
        assertTrue(bstEntry.fields.containsKey("title"));
        assertTrue(bstEntry.fields.containsKey("type"));
        assertTrue(bstEntry.localIntegers.containsKey("variable"));
        assertTrue(bstEntry.localStrings.containsKey("label"));
        assertTrue(bstEntry.localStrings.containsKey("sort.key$"));
    }

    @Test
    void testVisitReadCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                ENTRY { author title booktitle year owner timestamp url } { } { }
                READ
                """);
        List<BibEntry> testEntries = List.of(TestBstVM.defaultTestEntry());

        vm.render(testEntries);

        Map<String, String> fields = vm.getEntries().get(0).fields;
        assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", fields.get("author"));
        assertEquals("Effective work practices for floss development: A model and propositions", fields.get("title"));
        assertEquals("Hawaii International Conference On System Sciences (HICSS)", fields.get("booktitle"));
        assertEquals("2005", fields.get("year"));
        assertEquals("oezbek", fields.get("owner"));
        assertEquals("2006.05.29", fields.get("timestamp"));
        assertEquals("http://james.howison.name/publications.html", fields.get("url"));
    }

    @Test
    public void testVisitExecuteCommand() throws RecognitionException {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                INTEGERS { variable.a }
                FUNCTION { init.state.consts } { #5 'variable.a := }
                EXECUTE { init.state.consts }
                """);

        vm.render(Collections.emptyList());

        assertEquals(5, vm.getIntegers().get("variable.a"));
    }

    // ToDo: testVisitReverseCommand

    @Test
    public void testVisitIterateCommand() throws RecognitionException {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                ENTRY { } { } { }
                FUNCTION { test } { cite$ }
                READ
                ITERATE { test }
                """);
        List<BibEntry> testEntries = List.of(
                TestBstVM.defaultTestEntry(),
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("test"));

        vm.render(testEntries);

        assertEquals(2, vm.getStack().size());
        assertEquals("test", vm.getStack().pop());
        assertEquals("canh05", vm.getStack().pop());
    }

    @Test
    public void testVisitSortCommand() throws RecognitionException {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                ENTRY { } { } { }
                FUNCTION { presort } { cite$ 'sort.key$ := }
                ITERATE { presort }
                SORT
                """);
        List<BibEntry> testEntries = List.of(
                new BibEntry(StandardEntryType.Article).withCitationKey("c"),
                new BibEntry(StandardEntryType.Article).withCitationKey("b"),
                new BibEntry(StandardEntryType.Article).withCitationKey("d"),
                new BibEntry(StandardEntryType.Article).withCitationKey("a"));

        vm.render(testEntries);

        List<BstEntry> sortedEntries = vm.getEntries();
        assertEquals(Optional.of("a"), sortedEntries.get(0).entry.getCitationKey());
        assertEquals(Optional.of("b"), sortedEntries.get(1).entry.getCitationKey());
        assertEquals(Optional.of("c"), sortedEntries.get(2).entry.getCitationKey());
        assertEquals(Optional.of("d"), sortedEntries.get(3).entry.getCitationKey());
    }

    @Test
    void testVisitIdentifier() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                ENTRY { } { local.variable } { local.label }
                READ
                STRINGS { label }
                INTEGERS { variable }
                FUNCTION { test } {
                    #1 'local.variable :=
                    #2 'variable :=
                    "TEST" 'local.label :=
                    "TEST-GLOBAL" 'label :=
                    local.label local.variable
                    label variable
                }
                ITERATE { test }
                """);
        List<BibEntry> testEntries = List.of(TestBstVM.defaultTestEntry());

        vm.render(testEntries);

        assertEquals(2, vm.getStack().pop());
        assertEquals("TEST-GLOBAL", vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals("TEST", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    // stackitem
}

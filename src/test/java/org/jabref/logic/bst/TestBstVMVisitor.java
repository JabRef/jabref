package org.jabref.logic.bst;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

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
        TestBstVM.TestVM vm = new TestBstVM.TestVM("FUNCTION {test.func} { #1 'test.var := } EXECUTE { test.func }");

        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.getFunctions();
        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));
    }

    // ToDo: Belongs in testBstFunction
    @Test
    void testAssignFunction() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                INTEGERS { test.var }
                FUNCTION { test.func } { #1 'test.var := }
                EXECUTE { test.func }
                """);

        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.getFunctions();
        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));
        assertEquals(1, vm.latestContext.integers().get("test.var"));
    }

    @Test
    void testVisitMacroCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("MACRO {jan} { \"January\" } EXECUTE {jan}");

        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.getFunctions();
        assertTrue(functions.containsKey("jan"));
        assertNotNull(functions.get("jan"));
        assertEquals("January", vm.latestContext.stack().pop());
        assertTrue(vm.latestContext.stack().isEmpty());
    }

    @Test
    void testVisitEntryCommand() throws IOException {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("ENTRY {address author title type}{variable}{label}");
        List<BibEntry> testEntries = List.of(TestBstVM.t1BibtexEntry());

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
    void testVisitReadCommand() throws IOException {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("ENTRY {author title booktitle year owner timestamp url}{}{} READ");
        List<BibEntry> testEntries = List.of(TestBstVM.t1BibtexEntry());

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

    // execute iterate reverse

    // sort

    @Test
    void testVisitIdentifier() throws IOException {
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
        List<BibEntry> testEntries = List.of(TestBstVM.t1BibtexEntry());

        vm.render(testEntries);

        assertEquals(2, vm.getStack().pop());
        assertEquals("TEST-GLOBAL", vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals("TEST", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void testIf() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("""
                FUNCTION { path1 } { #1 }
                FUNCTION { path0 } { #0 }
                FUNCTION { test } {
                    #1 path1 path0 if$
                    #0 path1 path0 if$
                }
                EXECUTE { test }
                """);

        vm.render(Collections.emptyList());

        assertEquals(0, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    // bstFunction

    // stackitem
}

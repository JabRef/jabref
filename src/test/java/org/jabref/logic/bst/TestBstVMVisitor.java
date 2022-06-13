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

        Map<String, String> strList = vm.latestContext.strings();

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

        Map<String, Integer> integersList = vm.latestContext.integers();

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

        Map<String, BstFunctions.BstFunction> functions = vm.latestContext.functions();

        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));
    }

    // ToDo: Belongs in testBstFunction
    @Test
    void testAssignFunction() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("INTEGERS { test.var }" +
                "FUNCTION {test.func} { #1 'test.var := }" +
                "EXECUTE { test.func }");
        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.latestContext.functions();

        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));

        assertEquals(1, vm.latestContext.integers().get("test.var"));
    }

    @Test
    void testVisitMacroCommand() {
        TestBstVM.TestVM vm = new TestBstVM.TestVM("MACRO {jan} { \"January\" } EXECUTE {jan}");
        vm.render(Collections.emptyList());

        Map<String, BstFunctions.BstFunction> functions = vm.latestContext.functions();
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
        BstEntry bstEntry = vm.latestContext.entries().get(0);

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

        Map<String, String> fields = vm.latestContext.entries().get(0).fields;

        assertTrue(fields.containsKey("author"));
        assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", fields.get("author"));
        assertTrue(fields.containsKey("title"));
        assertEquals("Effective work practices for floss development: A model and propositions", fields.get("title"));
        assertTrue(fields.containsKey("booktitle"));
        assertEquals("Hawaii International Conference On System Sciences (HICSS)", fields.get("booktitle"));
        assertTrue(fields.containsKey("year"));
        assertEquals("2005", fields.get("year"));
        assertTrue(fields.containsKey("owner"));
        assertEquals("oezbek", fields.get("owner"));
        assertTrue(fields.containsKey("timestamp"));
        assertEquals("2006.05.29", fields.get("timestamp"));
        assertTrue(fields.containsKey("url"));
        assertEquals("http://james.howison.name/publications.html", fields.get("url"));
    }
}

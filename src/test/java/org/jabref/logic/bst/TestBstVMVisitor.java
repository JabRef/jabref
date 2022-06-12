package org.jabref.logic.bst;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestBstVMVisitor {
    private List<BibEntry> testEntry;

    @BeforeEach
    void beforeAll() throws IOException {
        // this.testEntry = List.of(TestBstVM.t1BibtexEntry());
    }

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
    }
}

package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBstVM {

    public static BibEntry defaultTestEntry() {
        /*
            @inproceedings{canh05,
            author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},
            title = {Effective work practices for floss development: A model and propositions},
            booktitle = {Hawaii International Conference On System Sciences (HICSS)},
            year = {2005},
            owner = {oezbek},
            timestamp = {2006.05.29},
            url = {http://james.howison.name/publications.html}}
        */

        return new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("canh05")
                .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H. and Howison, J. and Masango, C.")
                .withField(StandardField.TITLE, "Effective work practices for floss development: A model and propositions")
                .withField(StandardField.BOOKTITLE, "Hawaii International Conference On System Sciences (HICSS)")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.OWNER, "oezbek")
                .withField(StandardField.TIMESTAMP, "2006.05.29")
                .withField(StandardField.URL, "http://james.howison.name/publications.html");
    }

    @Test
    public void testAbbrv() throws RecognitionException, IOException {
        TestVM vm = new TestVM(Path.of("src/test/resources/org/jabref/logic/bst/abbrv.bst"));
        List<BibEntry> testEntries = List.of(defaultTestEntry());

        String expected = "\\begin{thebibliography}{1}\\bibitem{canh05}K.~Crowston, H.~Annabi, J.~Howison, and C.~Masango.\\newblock Effective work practices for floss development: A model and  propositions.\\newblock In {\\em Hawaii International Conference On System Sciences (HICSS)}, 2005.\\end{thebibliography}";
        String result = vm.render(testEntries);

        assertEquals(
                expected.replaceAll("\\s", ""),
                result.replaceAll("\\s", ""));
    }

    @Test
    public void testSimple() throws RecognitionException {
        TestVM vm = new TestVM("""
               ENTRY { address author title type } { } { label }
               INTEGERS { output.state before.all mid.sentence after.sentence after.block }
               FUNCTION { init.state.consts }{
                  #0 'before.all :=
                  #1 'mid.sentence :=
                  #2 'after.sentence :=
                  #3 'after.block :=
               }
               STRINGS { s t }
               READ
               """);
        List<BibEntry> testEntries = List.of(defaultTestEntry());

        vm.render(testEntries);

        assertEquals(2, vm.getStrings().size());
        assertEquals(7, vm.getIntegers().size());
        assertEquals(1, vm.getEntries().size());
        assertEquals(5, vm.getEntries().get(0).fields.size());
        assertEquals(38, vm.getFunctions().size());
    }

    @Test
    public void testLabel() throws RecognitionException {
        TestVM vm = new TestVM("""
                ENTRY { title } {} { label }
                FUNCTION { test } {
                    label #0 =
                    title 'label :=
                    #5 label #6 pop$ }
                READ
                ITERATE { test }
                """);
        List<BibEntry> testEntries = List.of(defaultTestEntry());

        vm.render(testEntries);

        assertEquals(
                "Effective work practices for floss development: A model and propositions",
                vm.getStack().pop());
    }

    @Test
    public void testQuote() throws RecognitionException {
        TestVM vm = new TestVM("FUNCTION { a }{ quote$ quote$ * } EXECUTE { a }");

        vm.render(Collections.emptyList());
        assertEquals("\"\"", vm.getStack().pop());
    }

    @Test
    public void testBuildIn() throws RecognitionException {
        TestVM vm = new TestVM("EXECUTE { global.max$ }");

        vm.render(Collections.emptyList());

        assertEquals(Integer.MAX_VALUE, vm.getStack().pop());
        assertTrue(vm.getStack().empty());
    }

    @Test
    public void testVariables() throws RecognitionException {
        TestVM vm = new TestVM("""
                STRINGS { t }
                FUNCTION { not } {
                    { #0 } { #1 } if$
                }
                FUNCTION { n.dashify } {
                    "HELLO-WORLD" 't :=
                    t empty$ not
                }
                EXECUTE { n.dashify }
                """);

        vm.render(Collections.emptyList());

        assertEquals(BstVM.TRUE, vm.getStack().pop());
    }

    @Test
    public void testHypthenatedName() throws RecognitionException, IOException {
        TestVM vm = new TestVM(Path.of("src/test/resources/org/jabref/logic/bst/abbrv.bst"));
        List<BibEntry> testEntries = List.of(
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("canh05")
                        .withField(StandardField.AUTHOR, "Jean-Paul Sartre")
        );

        String result = vm.render(testEntries);

        assertTrue(result.contains("J.-P. Sartre"));
    }

    public static class TestVM extends BstVM {

        private TestVM(Path path) throws RecognitionException, IOException {
            super(CharStreams.fromPath(path));
        }

        protected TestVM(String s) throws RecognitionException {
            super(s);
        }

        protected Map<String, String> getStrings() {
            return latestContext.strings();
        }

        protected Map<String, Integer> getIntegers() {
            return latestContext.integers();
        }

        protected List<BstEntry> getEntries() {
            return latestContext.entries();
        }

        protected Map<String, BstFunctions.BstFunction> getFunctions() {
            return latestContext.functions();
        }

        protected Stack<Object> getStack() {
            return latestContext.stack();
        }
    }
}

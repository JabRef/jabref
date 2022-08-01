package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.antlr.v4.runtime.RecognitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BstVMTest {

    public static BibEntry defaultTestEntry() {
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
        BstVM vm = new BstVM(Path.of("src/test/resources/org/jabref/logic/bst/abbrv.bst"));
        List<BibEntry> testEntries = List.of(defaultTestEntry());

        String expected = "\\begin{thebibliography}{1}\\bibitem{canh05}K.~Crowston, H.~Annabi, J.~Howison, and C.~Masango.\\newblock Effective work practices for floss development: A model and  propositions.\\newblock In {\\em Hawaii International Conference On System Sciences (HICSS)}, 2005.\\end{thebibliography}";
        String result = vm.render(testEntries);

        assertEquals(
                expected.replaceAll("\\s", ""),
                result.replaceAll("\\s", ""));
    }

    @Test
    public void testSimple() throws RecognitionException {
        BstVM vm = new BstVM("""
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

        assertEquals(2, vm.latestContext.strings().size());
        assertEquals(7, vm.latestContext.integers().size());
        assertEquals(1, vm.latestContext.entries().size());
        assertEquals(5, vm.latestContext.entries().get(0).fields.size());
        assertEquals(38, vm.latestContext.functions().size());
    }

    @Test
    public void testLabel() throws RecognitionException {
        BstVM vm = new BstVM("""
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
                vm.latestContext.stack().pop());
    }

    @Test
    public void testQuote() throws RecognitionException {
        BstVM vm = new BstVM("FUNCTION { a }{ quote$ quote$ * } EXECUTE { a }");

        vm.render(Collections.emptyList());
        assertEquals("\"\"", vm.latestContext.stack().pop());
    }

    @Test
    public void testBuildIn() throws RecognitionException {
        BstVM vm = new BstVM("EXECUTE { global.max$ }");

        vm.render(Collections.emptyList());

        assertEquals(Integer.MAX_VALUE, vm.latestContext.stack().pop());
        assertTrue(vm.latestContext.stack().empty());
    }

    @Test
    public void testVariables() throws RecognitionException {
        BstVM vm = new BstVM("""
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

        assertEquals(BstVM.TRUE, vm.latestContext.stack().pop());
    }

    @Test
    public void testHypthenatedName() throws RecognitionException, IOException {
        BstVM vm = new BstVM(Path.of("src/test/resources/org/jabref/logic/bst/abbrv.bst"));
        List<BibEntry> testEntries = List.of(
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("canh05")
                        .withField(StandardField.AUTHOR, "Jean-Paul Sartre")
        );

        String result = vm.render(testEntries);

        assertTrue(result.contains("J.-P. Sartre"));
    }

    @Test
    void testAbbrevStyleChopWord() {
        BstVM vm = new BstVM("""
                STRINGS { s }
                INTEGERS { len }

                FUNCTION { chop.word }
                {
                    's :=
                        'len :=
                        s #1 len substring$ =
                            { s len #1 + global.max$ substring$ }
                        's
                        if$
                }

                FUNCTION { test } {
                    "A " #2
                    "A Colorful Morning"
                    chop.word

                    "An " #3
                    "A Colorful Morning"
                    chop.word
                }

                EXECUTE { test }
                """);

        vm.render(Collections.emptyList());

        assertEquals("A Colorful Morning", vm.latestContext.stack().pop());
        assertEquals("Colorful Morning", vm.latestContext.stack().pop());
        assertEquals(0, vm.latestContext.stack().size());
    }

    @Test
    void testAbbrevStyleSortFormatTitle() {
        BstVM vm = new BstVM("""
                STRINGS { s t }
                INTEGERS { len }
                FUNCTION { sortify } {
                    purify$
                    "l" change.case$
                }

                FUNCTION { chop.word }
                {
                    's :=
                        'len :=
                        s #1 len substring$ =
                            { s len #1 + global.max$ substring$ }
                        's
                        if$
                }

                FUNCTION { sort.format.title }
                { 't :=
                   "A " #2
                    "An " #3
                      "The " #4 t chop.word
                    chop.word
                   chop.word
                  sortify
                  #1 global.max$ substring$
                }

                FUNCTION { test } {
                    "A Colorful Morning"
                    sort.format.title
                }

                EXECUTE {test}
                """);

        vm.render(Collections.emptyList());

        assertEquals("colorful morning", vm.latestContext.stack().pop());
    }
}

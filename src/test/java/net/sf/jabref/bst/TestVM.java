package net.sf.jabref.bst;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.bst.VM.BstEntry;
import net.sf.jabref.bst.VM.StackFunction;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ParserResult;

import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestVM {

    @Before
    public void setPreferences() {
        Globals.prefs = JabRefPreferences.getInstance();
    }


    @Test
    public void testAbbrv() throws RecognitionException, IOException {
        VM vm = new VM(new File("src/test/resources/net/sf/jabref/bst/abbrv.bst"));
        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());

        String expected = "\\begin{thebibliography}{1}\\bibitem{canh05}K.~Crowston, H.~Annabi, J.~Howison, and C.~Masango.\\newblock Effective work practices for floss development: A model and  propositions.\\newblock In {\\em Hawaii International Conference On System Sciences (HICSS)}, 2005.\\end{thebibliography}";

        Assert.assertEquals(expected.replaceAll("\\s", ""), vm.run(v).replaceAll("\\s", ""));
    }

    @Test
    public void testVMSimple() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { " + "  address " + "  author " + "  title " + "  type "
                + "}  {}  { label }" + "INTEGERS { output.state before.all"
                + " mid.sentence after.sentence after.block }"
                + "FUNCTION {init.state.consts}{ #0 'before.all := "
                + " #1 'mid.sentence :=  #2 'after.sentence :=  #3 'after.block := } "
                + "STRINGS { s t } " + "READ");

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());

        vm.run(v);

        Assert.assertEquals(2, vm.getStrings().size());
        Assert.assertEquals(7, vm.getIntegers().size());
        Assert.assertEquals(1, vm.getEntries().size());
        Assert.assertEquals(5, vm.getEntries().get(0).getFields().size());
        Assert.assertEquals(38, vm.getFunctions().size());

    }

    @Test
    public void testLabel() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { title }  {}  { label } "
                + "FUNCTION { test } { label #0 = title 'label := #5 label #6 pop$ } " + "READ "
                + "ITERATE { test }");

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());

        vm.run(v);

        Assert.assertEquals("Effective work practices for floss development: A model and propositions", vm
                .getStack().pop());

    }

    @Test
    public void testQuote() throws RecognitionException {

        VM vm = new VM("FUNCTION {a}{ quote$ quote$ * } EXECUTE {a}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals("\"\"", vm.getStack().pop());
    }

    @Test
    public void testVMFunction1() throws RecognitionException {

        VM vm = new VM("FUNCTION {init.state.consts}{ #0 'before.all := } ");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals(38, vm.getFunctions().size());

        Assert.assertTrue(vm.getFunctions().get("init.state.consts") instanceof StackFunction);

        StackFunction fun = (StackFunction) vm.getFunctions().get("init.state.consts");
        Assert.assertEquals(3, fun.getTree().getChildCount());
    }

    @Test
    public void testVMExecuteSimple() throws RecognitionException {
        VM vm = new VM("INTEGERS { variable.a } " + "FUNCTION {init.state.consts}{ #5 'variable.a := } "
                + "EXECUTE {init.state.consts}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(Integer.valueOf(5), vm.getIntegers().get("variable.a"));
    }

    @Test
    public void testVMExecuteSimple2() throws RecognitionException {
        VM vm = new VM("FUNCTION {a}{ #5 #5 = " + "#1 #2 = " + "#3 #4 < " + "#4 #3 < "
                + "#4 #4 < " + "#3 #4 > " + "#4 #3 > " + "#4 #4 > " + "\"H\" \"H\" = "
                + "\"H\" \"Ha\" = } " + "EXECUTE {a}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMIfSkipPop() throws RecognitionException {
        VM vm = new VM("FUNCTION {not}	{   { #0 }	    { #1 }  if$	}"
                + "FUNCTION {and}	{   'skip$	    { pop$ #0 }	  if$	}"
                + "FUNCTION {or}	{   { pop$ #1 }	    'skip$	  if$	}" + "FUNCTION {test} { "
                + "#1 #1 and #0 #1 and #1 #0 and #0 #0 and " + "#0 not #1 not "
                + "#1 #1 or #0 #1 or #1 #0 or #0 #0 or }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMArithmetic() throws RecognitionException {

        VM vm = new VM("FUNCTION {test} { " + "#1 #1 + #5 #2 - }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(3, vm.getStack().pop());
        Assert.assertEquals(2, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());

    }

    @Test
    public void testVMArithmetic2() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { " + "#1 \"HELLO\" + #5 #2 - }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();

        try {
            vm.run(v);
            Assert.fail();
        } catch (VMException ignored) {
            // Ignored
        }
    }

    @Test
    public void testNumNames() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { \"Johnny Foo and Mary Bar\" num.names$ }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(2, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testNumNames2() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { \"Johnny Foo { and } Mary Bar\" num.names$ }"
                + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(1, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMStringOps1() throws RecognitionException {
        VM vm = new VM(
                "FUNCTION {test} { \"H\" \"allo\" * \"Johnny\" add.period$ \"Johnny.\" add.period$"
                        + "\"Johnny!\" add.period$ \"Johnny?\" add.period$ \"Johnny} }}}\" add.period$"
                        + "\"Johnny!}\" add.period$ \"Johnny?}\" add.period$ \"Johnny.}\" add.period$ }"
                        + "EXECUTE {test}"
                );

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("Johnny.}", vm.getStack().pop());
        Assert.assertEquals("Johnny?}", vm.getStack().pop());
        Assert.assertEquals("Johnny!}", vm.getStack().pop());
        Assert.assertEquals("Johnny.}", vm.getStack().pop());
        Assert.assertEquals("Johnny?", vm.getStack().pop());
        Assert.assertEquals("Johnny!", vm.getStack().pop());
        Assert.assertEquals("Johnny.", vm.getStack().pop());
        Assert.assertEquals("Johnny.", vm.getStack().pop());
        Assert.assertEquals("Hallo", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testSubstring() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} " + "{ \"123456789\" #2  #1  substring$ " + // 2
        "  \"123456789\" #4 global.max$ substring$ " + // 456789
        "  \"123456789\" #1  #9  substring$ " + // 123456789
        "  \"123456789\" #1  #10 substring$ " + // 123456789
        "  \"123456789\" #1  #99 substring$ " + // 123456789

        "  \"123456789\" #-7 #3  substring$ " + // 123
        "  \"123456789\" #-1 #1  substring$ " + // 9
        "  \"123456789\" #-1 #3  substring$ " + // 789
        "  \"123456789\" #-2 #2  substring$ " + // 78

        "} EXECUTE {test} ");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("78", vm.getStack().pop());
        Assert.assertEquals("789", vm.getStack().pop());
        Assert.assertEquals("9", vm.getStack().pop());
        Assert.assertEquals("123", vm.getStack().pop());

        Assert.assertEquals("123456789", vm.getStack().pop());
        Assert.assertEquals("123456789", vm.getStack().pop());
        Assert.assertEquals("123456789", vm.getStack().pop());
        Assert.assertEquals("456789", vm.getStack().pop());
        Assert.assertEquals("2", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testEmpty() throws RecognitionException, IOException {
        VM vm = new VM("ENTRY {title}{}{} READ STRINGS { s } FUNCTION {test} " + "{ s empty$ " + // FALSE
        "\"\" empty$ " + // FALSE
        "\"   \" empty$ " + // FALSE
        " title empty$ " + // FALSE
        " \" HALLO \" empty$ } ITERATE {test} ");

        List<BibEntry> v = new ArrayList<>();
        v.add(TestVM.bibtexString2BibtexEntry("@article{a, author=\"AAA\"}"));
        vm.run(v);
        Assert.assertEquals(VM.FALSE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testDuplicateEmptyPopSwapIf() throws RecognitionException {
        VM vm = new VM("FUNCTION {emphasize} " + "{ duplicate$ empty$ " + "  { pop$ \"\" } "
                + "  { \"{\\em \" swap$ * \"}\" * } " + "  if$ " + "} " + "FUNCTION {test} {"
                + "  \"\" emphasize " + "  \"Hello\" emphasize " + "}" + "EXECUTE {test} ");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("{\\em Hello}", vm.getStack().pop());
        Assert.assertEquals("", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testChangeCase() throws RecognitionException {
        VM vm = new VM(
                "STRINGS { title } "
                        + "READ "
                        + "FUNCTION {format.title}"
                        + " { duplicate$ empty$ "
                        + "    { pop$ \"\" } "
                        + "    { \"t\" change.case$ } "
                        + "  if$ "
                        + "} "
                        + "FUNCTION {test} {"
                        + "  \"hello world\" \"u\" change.case$ format.title "
                        + "  \"Hello World\" format.title "
                        + "  \"\" format.title "
                        + "  \"{A}{D}/{C}ycle: {I}{B}{M}'s {F}ramework for {A}pplication {D}evelopment and {C}ase\" \"u\" change.case$ format.title "
                        + "}" + "EXECUTE {test} "
                );

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(
                "{A}{D}/{C}ycle: {I}{B}{M}'s {F}ramework for {A}pplication {D}evelopment and {C}ase",
                vm.getStack().pop());
        Assert.assertEquals("", vm.getStack().pop());
        Assert.assertEquals("Hello world", vm.getStack().pop());
        Assert.assertEquals("Hello world", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testTextLength() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} {" + "  \"hello world\" text.length$ "
                + "  \"Hello {W}orld\" text.length$ " + "  \"\" text.length$ "
                + "  \"{A}{D}/{Cycle}\" text.length$ "
                + "  \"{\\This is one character}\" text.length$ "
                + "  \"{\\This {is} {one} {c{h}}aracter as well}\" text.length$ "
                + "  \"{\\And this too\" text.length$ " + "  \"These are {\\11}\" text.length$ " + "} "
                + "EXECUTE {test} ");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(11, vm.getStack().pop());
        Assert.assertEquals(1, vm.getStack().pop());
        Assert.assertEquals(1, vm.getStack().pop());
        Assert.assertEquals(1, vm.getStack().pop());
        Assert.assertEquals(8, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().pop());
        Assert.assertEquals(11, vm.getStack().pop());
        Assert.assertEquals(11, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMIntToStr() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { #3 int.to.str$ #9999 int.to.str$}" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("9999", vm.getStack().pop());
        Assert.assertEquals("3", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMChrToInt() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { \"H\" chr.to.int$ }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals(72, vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testVMChrToIntIntToChr() throws RecognitionException {
        VM vm = new VM("FUNCTION {test} { \"H\" chr.to.int$ int.to.chr$ }" + "EXECUTE {test}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("H", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testSort() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { title }  { }  { label }"
                + "FUNCTION {presort} { cite$ 'sort.key$ := } ITERATE { presort } SORT");

        List<BibEntry> v = new ArrayList<>();
        v.add(TestVM.bibtexString2BibtexEntry("@article{a, author=\"AAA\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@article{b, author=\"BBB\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@article{d, author=\"DDD\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@article{c, author=\"CCC\"}"));
        vm.run(v);

        List<BstEntry> v2 = vm.getEntries();
        Assert.assertEquals("a", v2.get(0).getBibtexEntry().getCiteKey());
        Assert.assertEquals("b", v2.get(1).getBibtexEntry().getCiteKey());
        Assert.assertEquals("c", v2.get(2).getBibtexEntry().getCiteKey());
        Assert.assertEquals("d", v2.get(3).getBibtexEntry().getCiteKey());
    }

    @Test
    public void testBuildIn() throws RecognitionException {
        VM vm = new VM("EXECUTE {global.max$}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals(Integer.MAX_VALUE, vm.getStack().pop());
        Assert.assertTrue(vm.getStack().empty());
    }

    @Test
    public void testVariables() throws RecognitionException {

        VM vm = new VM(" STRINGS { t }                          "
                + " FUNCTION {not}	{ { #0 } { #1 }  if$ } "
                + " FUNCTION {n.dashify} { \"HELLO-WORLD\" 't := t empty$ not } "
                + " EXECUTE {n.dashify}                    ");

        vm.run(new ArrayList<>());

        Assert.assertEquals(VM.TRUE, vm.getStack().pop());
    }

    @Test
    public void testWhile() throws RecognitionException {

        VM vm = new VM(
                "STRINGS { t }            "
                        + "FUNCTION {not}	{   "
                        + " { #0 } { #1 }  if$ } "
                        + "FUNCTION {n.dashify}              "
                        + "{ \"HELLO-WORLD\"                 "
                        + "  't :=                           "
                        + " \"\"                                                 "
                        + "	   { t empty$ not }                 "
                        + "	   { t #1 #1 substring$ \"-\" =                      "
                        + "	     { t #1 #2 substring$ \"--\" = not "
                        + "	          { \"--\" *                                       "
                        + "	            t #2 global.max$ substring$ 't :=                 "
                        + "	          }                                                    "
                        + "	          {   { t #1 #1 substring$ \"-\" = }                "
                        + "	              { \"-\" *                                         "
                        + "	                t #2 global.max$ substring$ 't :=               "
                        + "	              }                                                  "
                        + "	            while$                                                                  "
                        + "	          }                                                                  "
                        + "	        if$                                                                  "
                        + "	      }                                                                  "
                        + "	      { t #1 #1 substring$ *                                       "
                        + "	        t #2 global.max$ substring$ 't :=                          "
                        + "	      }                                                                  "
                        + "	      if$                                                                  "
                        + "	    }                                                                  "
                        + "	  while$                                                                  "
                        + "	}                                                                  "
                        + " EXECUTE {n.dashify} "
                );

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals(1, vm.getStack().size());
        Assert.assertEquals("HELLO--WORLD", vm.getStack().pop());
    }

    @Test
    public void testType() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { title }  { }  { label }"
                        + "FUNCTION {presort} { cite$ 'sort.key$ := } ITERATE { presort } SORT FUNCTION {test} { type$ } ITERATE { test }"
                );

        List<BibEntry> v = new ArrayList<>();
        v.add(TestVM.bibtexString2BibtexEntry("@article{a, author=\"AAA\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@book{b, author=\"BBB\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@misc{c, author=\"CCC\"}"));
        v.add(TestVM.bibtexString2BibtexEntry("@inproceedings{d, author=\"DDD\"}"));
        vm.run(v);

        Assert.assertEquals(4, vm.getStack().size());
        Assert.assertEquals("inproceedings", vm.getStack().pop());
        Assert.assertEquals("misc", vm.getStack().pop());
        Assert.assertEquals("book", vm.getStack().pop());
        Assert.assertEquals("article", vm.getStack().pop());
    }

    @Test
    public void testMissing() throws RecognitionException, IOException {

        VM vm = new VM( //
                "ENTRY    { title }  { }  { label } " + //
                "FUNCTION {presort} { cite$ 'sort.key$ := } " + //
                "ITERATE  {presort} " + //
                "READ SORT " + //
                "FUNCTION {test}{ title missing$ cite$ } " + //
                "ITERATE  { test }"
                );

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());
        v.add(TestVM.bibtexString2BibtexEntry("@article{test, author=\"No title\"}"));
        vm.run(v);

        Assert.assertEquals(4, vm.getStack().size());

        Assert.assertEquals("test", vm.getStack().pop());
        Assert.assertEquals(1, vm.getStack().pop());
        Assert.assertEquals("canh05", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().pop());
    }

    @Test
    public void testFormatName() throws RecognitionException {
        VM vm = new VM(
                "FUNCTION {format}{ \"Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin\" #1 \"{vv~}{ll}{, jj}{, f}?\" format.name$ }"
                        + "EXECUTE {format}");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);
        Assert.assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testFormatName2() throws RecognitionException, IOException {
        VM vm = new VM("ENTRY  { author }  { }  { label } " + "FUNCTION {presort} { cite$ 'sort.key$ := } "
                + "ITERATE { presort } " + "READ " + "SORT "
                + "FUNCTION {format}{ author #2 \"{vv~}{ll}{, jj}{, f}?\" format.name$ }" + "ITERATE {format}");

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());
        v.add(TestVM.bibtexString2BibtexEntry(
                "@book{test, author=\"Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin\"}"));
        vm.run(v);
        Assert.assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?", vm.getStack().pop());
        Assert.assertEquals("Annabi, H?", vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testCallType() throws RecognitionException, IOException {

        VM vm = new VM(
                "ENTRY  { title }  { }  { label } FUNCTION {presort} { cite$ 'sort.key$ := } ITERATE { presort } READ SORT "
                        + "FUNCTION {inproceedings}{ \"InProceedings called on \" title * } "
                        + "FUNCTION {book}{ \"Book called on \" title * } " + " ITERATE { call.type$ }"
                );

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());
        v.add(TestVM.bibtexString2BibtexEntry("@book{test, title=\"Test\"}"));
        vm.run(v);

        Assert.assertEquals(2, vm.getStack().size());

        Assert.assertEquals("Book called on Test", vm.getStack().pop());
        Assert.assertEquals(
                "InProceedings called on Effective work practices for floss development: A model and propositions",
                vm.getStack().pop());
        Assert.assertEquals(0, vm.getStack().size());
    }

    @Test
    public void testIterate() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { " + "  address " + "  author " + "  title " + "  type "
                + "}  {}  { label } " + "FUNCTION {test}{ cite$ } " + "READ " + "ITERATE { test }");

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());

        v.add(TestVM.bibtexString2BibtexEntry("@article{test, title=\"BLA\"}"));

        vm.run(v);

        Assert.assertEquals(2, vm.getStack().size());

        String s1 = (String) vm.getStack().pop();
        String s2 = (String) vm.getStack().pop();

        if ("canh05".equals(s1)) {
            Assert.assertEquals("test", s2);
        } else {
            Assert.assertEquals("canh05", s2);
            Assert.assertEquals("test", s1);
        }
    }

    @Test
    public void testWidth() throws RecognitionException, IOException {

        VM vm = new VM("ENTRY  { " + "  address " + "  author " + "  title " + "  type "
                + "}  {}  { label } " + //
                "STRINGS { longest.label } " + //
                "INTEGERS { number.label longest.label.width } " + //
                "FUNCTION {initialize.longest.label} " + //
                "{ \"\" 'longest.label := " + //
                "  #1 'number.label := " + //
                "  #0 'longest.label.width := " + //
                "} " + //
                " " + //
                "		FUNCTION {longest.label.pass} " + //
                "		{ number.label int.to.str$ 'label := " + //
                "		  number.label #1 + 'number.label := " + //
                "		  label width$ longest.label.width > " + //
                "		    { label 'longest.label := " + //
                "		      label width$ 'longest.label.width := " + //
                "		    } " + //
                "		    'skip$ " + //
                "		  if$ " + //
                "		} " + //
                " " + //
                "		EXECUTE {initialize.longest.label} " + //
                " " + //
                "		ITERATE {longest.label.pass} " + //
                "FUNCTION {begin.bib} " + //
                "{ preamble$ empty$" + //
                "    'skip$" + //
                "    { preamble$ write$ newline$ }" + //
                "  if$" + //
                "  \"\\begin{thebibliography}{\"  longest.label  * \"}\" *" + //
                "}" + //
                "EXECUTE {begin.bib}");//

        List<BibEntry> v = new ArrayList<>();
        v.add(t1BibtexEntry());

        vm.run(v);

        Assert.assertTrue(vm.getIntegers().containsKey("longest.label.width"));
        Assert.assertEquals("\\begin{thebibliography}{1}", vm.getStack().pop());
    }

    @Test
    public void testVMSwap() throws RecognitionException {

        VM vm = new VM("FUNCTION {a}{ #3 \"Hallo\" swap$ } EXECUTE { a }");

        List<BibEntry> v = new ArrayList<>();
        vm.run(v);

        Assert.assertEquals(2, vm.getStack().size());
        Assert.assertEquals(3, vm.getStack().pop());
        Assert.assertEquals("Hallo", vm.getStack().pop());
    }

    private static BibEntry bibtexString2BibtexEntry(String s) throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(s));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());
        return c.iterator().next();
    }

    /* TEST DATA */
    private String t1BibtexString() {
        return "@inproceedings{canh05,\n"
                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},\n"
                + "  title = {Effective work practices for floss development: A model and propositions},\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},\n"
                + "  year = {2005},\n" + "  owner = {oezbek},\n" + "  timestamp = {2006.05.29},\n"
                + "  url = {http://james.howison.name/publications.html}}\n";
    }

    @Test
    public void testHypthenatedName() throws RecognitionException, IOException {
        VM vm = new VM(new File("src/test/resources/net/sf/jabref/bst/abbrv.bst"));
        List<BibEntry> v = new ArrayList<>();
        v.add(TestVM.bibtexString2BibtexEntry("@article{canh05, author = \"Jean-Paul Sartre\" }"));
        Assert.assertTrue(vm.run(v).contains("J.-P. Sartre"));
    }

    private BibEntry t1BibtexEntry() throws IOException {
        return TestVM.bibtexString2BibtexEntry(t1BibtexString());
    }

}

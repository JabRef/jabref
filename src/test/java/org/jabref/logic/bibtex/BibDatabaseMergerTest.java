package org.jabref.logic.bibtex;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibDatabaseMergerTest {

    @Test
    void mergeAddsNonDuplicateEntries() {
        // Entries 2 and 3 are identical
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.AUTHOR, "Stephen Blaha")
                .withField(StandardField.TITLE, "Quantum Computers and Quantum Computer Languages: Quantum Assembly Language and Quantum C Language")
                .withField(StandardField.DATE, "2002-01-18")
                .withField(StandardField.ABSTRACT, "We show a representation of Quantum Computers defines Quantum Turing Machines with associated Quantum Grammars. We then create examples of Quantum Grammars. Lastly we develop an algebraic approach to high level Quantum Languages using Quantum Assembly language and Quantum C language as examples.")
                .withField(StandardField.EPRINT, "quant-ph/0201082")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/quant-ph/0201082v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "quant-ph")
                .withField(StandardField.KEYWORDS, "quant-ph, cs.PL");
        entry1.setType(StandardEntryType.Article);
        BibEntry entry2 = new BibEntry()
                .withField(StandardField.AUTHOR, "Phillip Kaye and Michele Mosca")
                .withField(StandardField.TITLE, "Quantum Networks for Generating Arbitrary Quantum States")
                .withField(StandardField.DATE, "2004-07-14")
                .withField(StandardField.ABSTRACT, "Quantum protocols often require the generation of specific quantum states. We describe a quantum algorithm for generating any prescribed quantum state. For an important subclass of states, including pure symmetric states, this algorithm is efficient.")
                .withField(StandardField.EPRINT, "quant-ph/0407102")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/quant-ph/0407102v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "quant-ph")
                .withField(StandardField.KEYWORDS, "quant-ph")
                .withField(StandardField.JOURNALTITLE, "Phillip Kaye, Michele Mosca, \"Quantum Networks for Generating Arbitrary Quantum States\", Proceedings, International Conference on Quantum Information (ICQI). Rochester, New York, USA, 2001");
        entry2.setType(StandardEntryType.Article);
        BibEntry entry3 = new BibEntry()
                .withField(StandardField.AUTHOR, "Phillip Kaye and Michele Mosca")
                .withField(StandardField.TITLE, "Quantum Networks for Generating Arbitrary Quantum States")
                .withField(StandardField.DATE, "2004-07-14")
                .withField(StandardField.ABSTRACT, "Quantum protocols often require the generation of specific quantum states. We describe a quantum algorithm for generating any prescribed quantum state. For an important subclass of states, including pure symmetric states, this algorithm is efficient.")
                .withField(StandardField.EPRINT, "quant-ph/0407102")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/quant-ph/0407102v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "quant-ph")
                .withField(StandardField.KEYWORDS, "quant-ph")
                .withField(StandardField.JOURNALTITLE, "Phillip Kaye, Michele Mosca, \"Quantum Networks for Generating Arbitrary Quantum States\", Proceedings, International Conference on Quantum Information (ICQI). Rochester, New York, USA, 2001");
        entry3.setType(StandardEntryType.Article);
        BibEntry entry4 = new BibEntry()
                .withField(StandardField.AUTHOR, "John Watrous")
                .withField(StandardField.TITLE, "Quantum Computational Complexity")
                .withField(StandardField.DATE, "2008-04-21")
                .withField(StandardField.ABSTRACT, "This article surveys quantum computational complexity, with a focus on three fundamental notions: polynomial-time quantum computations, the efficient verification of quantum proofs, and quantum interactive proof systems. Properties of quantum complexity classes based on these notions, such as BQP, QMA, and QIP, are presented. Other topics in quantum complexity, including quantum advice, space-bounded quantum computation, and bounded-depth quantum circuits, are also discussed.")
                .withField(StandardField.EPRINT, "0804.3401")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/0804.3401v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "quant-ph")
                .withField(StandardField.KEYWORDS, "quant-ph");
        entry4.setType(StandardEntryType.Article);

        BibDatabase database = new BibDatabase(List.of(entry1, entry2));
        BibDatabase other = new BibDatabase(List.of(entry3, entry4));
        new BibDatabaseMerger().merge(database, other);

        assertEquals(3, database.getEntries().size());
        assertEquals(List.of(entry1, entry3, entry4), database.getEntries());
    }

    @Test
    void mergeBibTexStringsWithSameNameAreImportedWithModifiedName() {
        BibtexString targetString = new BibtexString("name", "content1");

        // BibTeXStrings that are imported from two sources (same name different content)
        BibtexString sourceString1 = new BibtexString("name", "content2");
        BibtexString sourceString2 = new BibtexString("name", "content3");

        // The expected source BibTeXStrings after import (different name, different content)
        BibtexString importedBibTeXString1 = new BibtexString("name_1", "content2");
        BibtexString importedBibTeXString2 = new BibtexString("name_2", "content3");

        BibDatabase target = new BibDatabase();
        BibDatabase source1 = new BibDatabase();
        BibDatabase source2 = new BibDatabase();
        target.addString(targetString);
        source1.addString(sourceString1);
        source2.addString(sourceString2);

        new BibDatabaseMerger().mergeStrings(target, source1);
        new BibDatabaseMerger().mergeStrings(target, source2);
        // Use string representation to compare since the id will not match
        List<String> resultStringsSorted = target.getStringValues()
                                                 .stream()
                                                 .map(BibtexString::toString)
                                                 .sorted()
                                                 .collect(Collectors.toList());

        assertEquals(List.of(targetString.toString(), importedBibTeXString1.toString(),
                importedBibTeXString2.toString()), resultStringsSorted);
    }

    @Test
    void mergeBibTexStringsWithSameNameAndContentAreIgnored() {
        BibtexString targetString1 = new BibtexString("name1", "content1");
        BibtexString targetString2 = new BibtexString("name2", "content2");

        // BibTeXStrings that are imported (equivalent to target strings)
        BibtexString sourceString1 = new BibtexString("name1", "content1");
        BibtexString sourceString2 = new BibtexString("name2", "content2");

        BibDatabase target = new BibDatabase();
        BibDatabase source = new BibDatabase();
        target.addString(targetString1);
        target.addString(targetString2);
        source.addString(sourceString1);
        source.addString(sourceString2);

        new BibDatabaseMerger().mergeStrings(target, source);
        // Use string representation to compare since the id will not match
        List<String> resultStringsSorted = target.getStringValues()
                                                 .stream()
                                                 .map(BibtexString::toString)
                                                 .sorted()
                                                 .collect(Collectors.toList());

        assertEquals(List.of(targetString1.toString(), targetString2.toString()), resultStringsSorted);
    }
}

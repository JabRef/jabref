package org.jabref.logic.openoffice.bst;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

/// Standalone manual smoke-test for the BST → pandoc → OOText pipeline.
/// Run the `main` method to verify all four stages work BEFORE any dispatch/UI changes.
/// This is a throwaway test — not a JUnit test class.
public class BstPipelineManualTest {

    public static void main(String[] args) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Ding2006")
                .withField(StandardField.AUTHOR, "Ding, Eric L and Hutfless, Susan M and Ding, Xin and Girotra, Saket")
                .withField(StandardField.TITLE, "Chocolate and Prevention of Cardiovascular Disease: A Systematic Review")
                .withField(StandardField.JOURNAL, "Nutrition & Metabolism")
                .withField(StandardField.VOLUME, "3")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.YEAR, "2006");
        new ConvertToBibtexCleanup().cleanup(entry);

        // Point this at your local IEEEtran.bst (or use the test resource below)
        Path bstFile = Path.of("jablib/src/test/resources/org/jabref/logic/bst/IEEEtran.bst");
        BstVM vm = new BstVM(bstFile);

        BibDatabase database = new BibDatabase(List.of(entry));

        BstEntryRenderer renderer = new BstEntryRenderer(vm);
        String latex = renderer.renderEntryToLatex(entry, database);
        System.out.println("=== STRIPPED LaTeX ===\n" + latex);

        PandocLatexConverter pandoc = new PandocLatexConverter();
        System.out.println("\npandoc available: " + pandoc.isAvailable());
        String html = pandoc.latexToHtml(latex);
        System.out.println("\n=== pandoc HTML ===\n" + html);

        String ooText = BstHtmlToOOText.convert(html);
        System.out.println("\n=== OOText (ready for OOTextIntoOO.write) ===\n" + ooText);
    }
}

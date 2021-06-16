package org.jabref.logic.bst;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.RemoveTilde;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BstPreviewLayout implements PreviewLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstPreviewLayout.class);

    private final String name;

    private VM vm;
    private String error;

    public BstPreviewLayout(Path path) {
        name = path.getFileName().toString();
        if (!Files.exists(path)) {
            LOGGER.error("File {} not found", path.toAbsolutePath());
            error = Localization.lang("Error opening file '%0'.", path.toString());
            return;
        }
        try {
            vm = new VM(path.toFile());
        } catch (Exception e) {
            LOGGER.error("Could not read {}.", path.toAbsolutePath(), e);
            error = Localization.lang("Error opening file '%0'.", path.toString());
        }
    }

    @Override
    public String generatePreview(BibEntry originalEntry, BibDatabase database) {
        if (error != null) {
            return error;
        }
        // ensure that the entry is of BibTeX format (and do not modify the original entry)
        BibEntry entry = (BibEntry) originalEntry.clone();
        new ConvertToBibtexCleanup().cleanup(entry);
        String result = vm.run(List.of(entry));
        // Remove all comments
        result = result.replaceAll("%.*", "");
        // Remove all LaTeX comments
        // The RemoveLatexCommandsFormatter keeps the words inside latex environments. Therefore, we remove them manually
        result = result.replace("\\begin{thebibliography}{1}", "");
        result = result.replace("\\end{thebibliography}", "");
        // The RemoveLatexCommandsFormatter keeps the word inside the latex command, but we want to remove that completely
        result = result.replaceAll("\\\\bibitem[{].*[}]", "");
        // We want to replace \newblock by a space instead of completely removing it
        result = result.replace("\\newblock", " ");
        // remove all latex commands statements - assumption: command in a separate line
        result = result.replaceAll("(?m)^\\\\.*$", "");
        // remove some IEEEtran.bst output (resulting from a multiline \providecommand)
        result = result.replace("#2}}", "");
        // Have quotes right - and more
        result = new LatexToUnicodeFormatter().format(result);
        result = result.replace("``", "\"");
        result = result.replace("''", "\"");
        // Final cleanup
        result = new RemoveNewlinesFormatter().format(result);
        result = new RemoveLatexCommandsFormatter().format(result);
        result = new RemoveTilde().format(result);
        result = result.trim().replaceAll("  +", " ");
        return result;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }
}

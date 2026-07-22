package org.jabref.logic.preview;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.RemoveTilde;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A [PreviewLayout] for [BstStyle] entries in the style-select dialog.
/// Works for both internal (classpath) and external (filesystem) BST styles by delegating
/// VM creation to [BstStyle.createBstVM], mirroring the cleanup logic of [BstPreviewLayout].
@NullMarked
public final class BstStylePreviewLayout implements PreviewLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstStylePreviewLayout.class);

    private static final Pattern COMMENT_PATTERN = Pattern.compile("%.*");
    private static final Pattern BIBITEM_PATTERN = Pattern.compile("\\\\bibitem[{].*[}]");
    private static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile("(?m)^\\\\.*$");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("  +");

    private final BstStyle style;
    @Nullable private final BstVM bstVM;
    @Nullable private final String error;

    public BstStylePreviewLayout(BstStyle style) {
        this.style = style;
        BstVM vm = null;
        String err = null;
        try {
            vm = style.createBstVM();
        } catch (IOException e) {
            LOGGER.error("Could not load BST style for preview: {}", style.getPath(), e);
            err = Localization.lang("Error opening file '%0'", style.getName());
        }
        this.bstVM = vm;
        this.error = err;
    }

    @Override
    public String generatePreview(BibEntry originalEntry, BibDatabaseContext databaseContext) {
        if (error != null) {
            return error;
        }
        if (bstVM == null) {
            return "";
        }

        BibEntry entry = new BibEntry(originalEntry);
        new ConvertToBibtexCleanup().cleanup(entry);

        String result = bstVM.render(List.of(entry));
        result = COMMENT_PATTERN.matcher(result).replaceAll("");
        result = result.replace("\\begin{thebibliography}{1}", "");
        result = result.replace("\\end{thebibliography}", "");
        result = BIBITEM_PATTERN.matcher(result).replaceAll("");
        result = result.replace("\\newblock", " ");
        result = LATEX_COMMAND_PATTERN.matcher(result).replaceAll("");
        result = result.replace("#2}}", "");
        result = new LatexToUnicodeFormatter().format(result);
        result = result.replace("``", "\"");
        result = result.replace("''", "\"");
        result = new RemoveNewlinesFormatter().format(result);
        result = new RemoveLatexCommandsFormatter().format(result);
        result = new RemoveTilde().format(result);
        result = MULTIPLE_SPACES_PATTERN.matcher(result.trim()).replaceAll(" ");
        return result;
    }

    @Override
    public String getDisplayName() {
        return style.getName();
    }

    @Override
    public String getName() {
        return style.getName();
    }

    @Override
    public String getShortTitle() {
        return style.getName();
    }

    @Override
    public String getText() {
        return "";
    }
}

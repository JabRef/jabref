package org.jabref.logic.preview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.RemoveTilde;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class BstPreviewLayout implements PreviewLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstPreviewLayout.class);

    private static final Pattern COMMENT_PATTERN = Pattern.compile("%.*");
    private static final Pattern BIBITEM_PATTERN = Pattern.compile("\\\\bibitem[{].*[}]");
    private static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile("(?m)^\\\\.*$");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("  +");

    // LaTeX inline formatting → HTML, resolved before RemoveLatexCommandsFormatter strips them.
    // Both the \cmd{text} form (used by IEEEtran: \emph{Journal}) and the {\cmd text} group
    // form (used by abbrv.bst: {\em Booktitle}) are handled.
    private static final Pattern EMPH_PATTERN = Pattern.compile("\\\\emph\\{([^}]*?)}");
    private static final Pattern TEXTIT_PATTERN = Pattern.compile("\\\\textit\\{([^}]*?)}");
    private static final Pattern TEXTBF_PATTERN = Pattern.compile("\\\\textbf\\{([^}]*?)}");
    private static final Pattern TEXTSC_PATTERN = Pattern.compile("\\\\textsc\\{([^}]*?)}");
    private static final Pattern GROUP_EM_PATTERN = Pattern.compile("\\{\\\\em\\s+([^}]*?)}");
    private static final Pattern GROUP_IT_PATTERN = Pattern.compile("\\{\\\\it\\s+([^}]*?)}");
    private static final Pattern GROUP_BF_PATTERN = Pattern.compile("\\{\\\\bf\\s+([^}]*?)}");
    private static final Pattern GROUP_SC_PATTERN = Pattern.compile("\\{\\\\sc\\s+([^}]*?)}");

    /// Matches a single LaTeX math command wrapped in the `{{$...$}}` form written by some
    /// exporters, e.g. `{{$\Sigma$}}`. Group 1 is the command without its leading backslash
    /// (`Sigma`). Reads as: `{{` `$` `\` (command) `$` `}}`.
    private static final Pattern BRACED_MATH_COMMAND_PATTERN = Pattern.compile("\\{\\{\\$\\\\([^$]+)\\$\\}\\}");

    private final Path path;
    private String source;
    private final String name;
    @Nullable private BstVM bstVM;
    @Nullable private String error;

    /// Private constructor used by [of(BstStyle)] for pre-built internal styles.
    private BstPreviewLayout(Path path, String source, @Nullable BstVM bstVM, @Nullable String error) {
        this.path = path;
        this.source = source;
        this.name = path.getFileName().toString();
        this.bstVM = bstVM;
        this.error = error;
    }

    public BstPreviewLayout(Path path) {
        this.path = path;
        try {
            this.source = String.join("\n", Files.readAllLines(path));
        } catch (IOException e) {
            LOGGER.error("Error reading file", e);
            this.source = "";
        }

        name = path.getFileName().toString();
        if (!Files.exists(path)) {
            LOGGER.error("File {} not found", path.toAbsolutePath());
            error = Localization.lang("Error opening file '%0'", path.toString());
            return;
        }
        try {
            bstVM = new BstVM(path);
        } catch (IOException e) {
            LOGGER.error("Could not read {}.", path.toAbsolutePath(), e);
            error = Localization.lang("Error opening file '%0'", path.toString());
        }
    }

    /// Creates a [BstPreviewLayout] for the given [BstStyle], supporting both internal
    /// (classpath) and external (filesystem) styles.
    public static BstPreviewLayout of(BstStyle style) {
        if (style.getFilePath() != null) {
            return new BstPreviewLayout(style.getFilePath());
        }
        // Internal style: read content from the classpath resource
        String resourcePath = style.getPath();
        String styleName = style.getName();
        try (InputStream is = BstPreviewLayout.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                String err = Localization.lang("Error opening file '%0'", styleName);
                return new BstPreviewLayout(Path.of(styleName), "", null, err);
            }
            String source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BstVM vm = new BstVM(source);
            return new BstPreviewLayout(Path.of(styleName), source, vm, null);
        } catch (IOException e) {
            LOGGER.error("Could not load internal BST style for preview: {}", resourcePath, e);
            return new BstPreviewLayout(Path.of(styleName), "", null,
                    Localization.lang("Error opening file '%0'", styleName));
        }
    }

    @Override
    public String generatePreview(BibEntry originalEntry, BibDatabaseContext databaseContext) {
        if (error != null) {
            return error;
        }

        if (bstVM == null) {
            return "";
        }

        // Ensure that the entry is of BibTeX format (and do not modify the original entry)
        BibEntry entry = new BibEntry(originalEntry);
        new ConvertToBibtexCleanup().cleanup(entry);
        convertBracedMathToUnicode(entry);
        String result = bstVM.render(List.of(entry));
        // Remove all comments
        result = COMMENT_PATTERN.matcher(result).replaceAll("");
        // Remove all LaTeX comments
        // The RemoveLatexCommandsFormatter keeps the words inside latex environments. Therefore, we remove them manually
        result = result.replace("\\begin{thebibliography}{1}", "");
        result = result.replace("\\end{thebibliography}", "");
        // The RemoveLatexCommandsFormatter keeps the word inside the latex command, but we want to remove that completely
        result = BIBITEM_PATTERN.matcher(result).replaceAll("");
        // We want to replace \\newblock by a space instead of completely removing it
        result = result.replace("\\newblock", " ");
        // Remove all latex commands statements - assumption: command in a separate line
        result = LATEX_COMMAND_PATTERN.matcher(result).replaceAll("");
        // Remove some IEEEtran.bst output (resulting from a multiline \\providecommand)
        result = result.replace("#2}}", "");
        // Have quotes right - and more
        result = new LatexToUnicodeFormatter().format(result);
        result = result.replace("``", "\"");
        result = result.replace("''", "\"");
        // Convert LaTeX inline formatting to HTML before RemoveLatexCommandsFormatter strips them.
        result = org.jabref.logic.openoffice.bst.BSTFormatUtils.mapInlineLatexToHtml(result);
        // Final cleanup
        result = new RemoveNewlinesFormatter().format(result);
        result = new RemoveLatexCommandsFormatter().format(result);
        result = new RemoveTilde().format(result);
        result = MULTIPLE_SPACES_PATTERN.matcher(result.trim()).replaceAll(" ");
        return result;
    }

    /// Replaces `{{$\Cmd$}}` math expressions in all fields of the given entry by the corresponding
    /// Unicode character, wrapped in braces. The entry is modified in place, so it has to be a copy.
    ///
    /// The BST engine passes `$...$` through unchanged, so without this step the raw math reaches the
    /// preview and is then stripped by [RemoveLatexCommandsFormatter].
    ///
    /// The braces around the result are required: they keep `BstCaseChanger` from lowercasing the
    /// symbol (Σ would become σ). This is also the reason [BibEntry#getFieldLatexFree] cannot be used
    /// here -- it resolves the LaTeX, but does not add the protecting braces.
    ///
    /// Commands that cannot be resolved are kept as they are: [LatexToUnicodeAdapter#format] returns
    /// its (NFC-normalized) input if parsing fails, so replacing unconditionally would corrupt them.
    private static void convertBracedMathToUnicode(BibEntry entry) {
        for (Map.Entry<Field, String> field : Map.copyOf(entry.getFieldMap()).entrySet()) {
            Matcher matcher = BRACED_MATH_COMMAND_PATTERN.matcher(field.getValue());
            StringBuilder converted = new StringBuilder();
            boolean anyConversion = false;
            while (matcher.find()) {
                String unicode = LatexToUnicodeAdapter.format("\\" + matcher.group(1));
                if (unicode.contains("\\")) {
                    matcher.appendReplacement(converted, Matcher.quoteReplacement(matcher.group()));
                    continue;
                }
                matcher.appendReplacement(converted, Matcher.quoteReplacement("{" + unicode + "}"));
                anyConversion = true;
            }
            if (anyConversion) {
                matcher.appendTail(converted);
                entry.setField(field.getKey(), converted.toString());
            }
        }
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortTitle() {
        return name;
    }

    @Override
    public String getText() {
        return source;
    }

    /// Checks if the given style file is a BST file by checking the extension
    public static boolean isBstStyleFile(String styleFile) {
        return StandardFileType.BST.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    public Path getFilePath() {
        return path;
    }
}

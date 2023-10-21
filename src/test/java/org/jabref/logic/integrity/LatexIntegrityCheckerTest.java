package org.jabref.logic.integrity;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.ed.ph.snuggletex.definitions.CoreErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LatexIntegrityCheckerTest {

    private final LatexIntegrityChecker checker = new LatexIntegrityChecker();
    private final BibEntry entry = new BibEntry();

    @ParameterizedTest
    @MethodSource("provideAcceptedInputs")
    void acceptsAllowedInputs(Field field, String value) {
        entry.setField(field, value);
        assertEquals(List.of(), checker.check(entry));
    }

    /**
     * This method provides inputs containing valid LaTeX Syntax which a proper parser
     * should be able to parse without errors.
     */
    private static Stream<Arguments> provideAcceptedInputs() {
        return Stream.of(
                // Basic text inputs
                Arguments.of(StandardField.TITLE, "Simple Text"),

                // Simple commands
                Arguments.of(StandardField.TITLE, "\\section{X}"),
                Arguments.of(StandardField.TITLE, "\\newline"),
                Arguments.of(StandardField.TITLE, "\\par"),

                // Text decorations
                Arguments.of(StandardField.TITLE, "\\underline{Underlined}"),
                Arguments.of(StandardField.TITLE, "\\texttt{Monospace}"),
                Arguments.of(StandardField.TITLE, "\\textit{Italic}"),

                // Special characters and symbols
                Arguments.of(StandardField.TITLE, "Café"),
                Arguments.of(StandardField.TITLE, "αβγδε"),
                Arguments.of(StandardField.TITLE, "\\# \\$ \\% \\& \\{ \\} \\_ \\^ \\\\"),

                // Fonts and sizes
                Arguments.of(StandardField.TITLE, "\\tiny Tiny Text"),
                Arguments.of(StandardField.TITLE, "\\small Small Text"),
                Arguments.of(StandardField.TITLE, "\\large Large Text"),

                // Verbatim and special characters
                Arguments.of(StandardField.TITLE, "\\verb|Verbatim|"),
                Arguments.of(StandardField.TITLE, "$\\widetilde{i}$"),
                Arguments.of(StandardField.TITLE, "\\ldots"),

                // Simple environments
                Arguments.of(StandardField.TITLE, "\\begin{quote}Quoted Text\\end{quote}\n"),
                Arguments.of(StandardField.TITLE, "\\begin{center}Centered Text\\end{center}\n"),

                // Math environment inputs
                Arguments.of(StandardField.TITLE, "$x + y = z$"),
                Arguments.of(StandardField.TITLE, "\\(a^2 + b^2 = c^2\\)"),
                Arguments.of(StandardField.TITLE, "\\[E = mc^2\\]"),
                Arguments.of(StandardField.TITLE, "\\begin{math} V = I \\cdot R \\end{math}"),

                // Equations and alignment
                // Currently Unsupported
                // Arguments.of(StandardField.TITLE, "\\begin{align} x + y &= z \\\\ a &= b + c \\end{align}"),
                // Arguments.of(StandardField.TITLE, "\\begin{align*} A &:= B \\\\ C &\\rightarrow D \\end{align*}"),
                // Arguments.of(StandardField.TITLE, "\\begin{equation} E = mc^2 \\end{equation}"),
                // Arguments.of(StandardField.TITLE, "\\begin{align*} x + y &= z \\\\ a &= b + c \\\\ p &= \\frac{q}{r} \\end{align*}"),
                Arguments.of(StandardField.TITLE, "\\begin{eqnarray} x + y &= z \\\\ a &= b + c \\\\ p &= \\frac{q}{r} \\end{eqnarray}"),

                // Equations and matrices
                Arguments.of(StandardField.TITLE, "\\[ \\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix} \\]"),
                Arguments.of(StandardField.TITLE, "\\[ \\begin{bmatrix} x & y & z \\\\ u & v & w \\end{bmatrix} \\]"),

                // Tables
                Arguments.of(StandardField.TITLE, "\\begin{tabular}{|c|c|} \\hline 1 & 2 \\\\ 3 & 4 \\\\ \\hline \\end{tabular}"),
                Arguments.of(StandardField.TITLE, "\\begin{tabular}{cc} A & B \\\\ C & D \\end{tabular}"),
                Arguments.of(StandardField.TITLE, "\\begin{tabular}{|l|r|} \\hline Item & Quantity \\\\ \\hline Apple & 3 \\\\ Banana & 5 \\\\ \\hline \\end{tabular}"),

                // Lists and enumerations
                Arguments.of(StandardField.TITLE, "\\begin{itemize} \\item Item 1 \\item Item 2 \\item Item 3 \\end{itemize}"),
                Arguments.of(StandardField.TITLE, "\\begin{enumerate} \\item First \\item Second \\item Third \\end{enumerate}"),

                // Line breaks and spacing
                Arguments.of(StandardField.TITLE, "First Line \\\\ Second Line"),
                Arguments.of(StandardField.TITLE, "Some \\hspace{2cm} Space"),
                // Currently Unsupported
                // Arguments.of(StandardField.TITLE, "Some \\vspace{1cm} Space"),

                // Multiple commands and environments
                Arguments.of(StandardField.TITLE, "\\textbf{\\emph{Bold and Emphasized Text}} $5-3_k$"),
                Arguments.of(StandardField.TITLE, "\\begin{itemize} \\item\\begin{quote} \\textbf{Quoted} \\emph{Text} \\end{quote} \\end{itemize}"),

                // More currently unsupported operations

                // Figures and Graphics
                // Arguments.of(StandardField.TITLE, } "\\includegraphics[width=0.5\\textwidth]{image.jpg}"),
                // Arguments.of(StandardField.TITLE, "\\begin{figure} \\centering \\includegraphics[width=0.8\\textwidth]{plot.png} \\caption{Plot Caption} \\label{fig:plot} \\end{figure}"),

                // Citations and references
                // Arguments.of(StandardField.TITLE, "\\cite{key}"),
                // Arguments.of(StandardField.TITLE, "\\label{sec:intro}"),
                // Arguments.of(StandardField.TITLE, "As shown in \\ref{fig:plot}"),

                // Arguments.of(StandardField.TITLE, "\\input{chapter1.tex}"),
                // Arguments.of(StandardField.TITLE, "Footnote\\footnote{This is a footnote}"),
                // Arguments.of(StandardField.TITLE, "$\text{in math}$"),

                // Comments should not raise any error, because they are not typeset using LaTeX
                Arguments.of(StandardField.COMMENT, "\\undefinedCommand"),

                // Some commands not supported by SnuggleTeX as default
                // Source: https://github.com/JabRef/jabref/issues/8712#issuecomment-1730441206
                Arguments.of(StandardField.ABSTRACT, "\\textless{}xml\\textgreater{} \\textbar something \textbackslash"),

                // Mirrored from org.jabref.logic.integrity.AmpersandCheckerTest.provideAcceptedInputs
                Arguments.of(StandardField.TITLE, "No ampersand at all"),
                Arguments.of(StandardField.FOREWORD, "Properly escaped \\&"),
                Arguments.of(StandardField.AUTHOR, "\\& Multiple properly escaped \\&"),
                Arguments.of(StandardField.BOOKTITLE, "\\\\\\& With multiple backslashes"),
                Arguments.of(StandardField.COMMENT, "\\\\\\& With multiple backslashes multiple times \\\\\\\\\\&"),
                Arguments.of(StandardField.NOTE, "In the \\& middle of \\\\\\& something"),
                Arguments.of(StandardField.DOI, "10.1007/0-387-22874-8_7"),
                Arguments.of(new UserSpecificCommentField("test"), "_something_ which is triggers the integrity check ^^^ $")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnacceptedInputs")
    void rejectsDisallowedInputs(String expectedMessage, Field field, String value) {
        entry.setField(field, value);
        assertEquals(List.of(new IntegrityMessage(expectedMessage, entry, field)), checker.check(entry));
    }

    /**
     * This method provides inputs containing invalid LaTeX syntax which no LaTeX parser should be able to parse
     * without errors. The inputs are bundled with the {@link uk.ac.ed.ph.snuggletex.ErrorCode} output by the internal
     * LaTeX parsers {@link uk.ac.ed.ph.snuggletex.SnuggleSession}.
     */
    private static Stream<Arguments> provideUnacceptedInputs() {
        return Stream.of(

                // ------------------------------------ LATEX PARSING/TOKENISATION ERRORS ------------------------------------
                // TTEG00: Finished reading document before finding required terminator "{0}"
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEG00, "}"), StandardField.ABSTRACT, "Unbalanced braces {"),

                // TTEG01: Nothing following \
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEG01), StandardField.ABSTRACT, "\\"),

                // TTEG02: Non-ASCII character {0} (Unicode U+{1}) at offset {2} in input document - replaced with ’x’
                // todo find out how to trigger / activate - is this even implemented in snuggletex?
                // Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEG02, "€", "20AC", "3"), StandardField.TITLE, "A title with € symbol"),

                // TTEG03: Delimiter {0} closing Math mode followed no matching opener
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEG03, "\\]"), StandardField.TITLE, "1+1=2\\]"),

                // TTEG04: Argument placeholder tokens (e.g. #1) may only appear in command and environment definitions
                // Excluded
                // Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEG04), StandardField.FOREWORD, "Text with #1 placeholder"),

                // TTEM00: Already in math mode - cannot use \( or \[
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEM00), StandardField.ABSTRACT, "Braces inside: $\\(1+1\\)=2$"),

                // TTEM01: $ was ended by $$
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEM01), StandardField.TITLE, "$1+1=2$$"),

                // TTEM02: Math mode opened by {0} but matching {1} was never found
                // SKipped

                // TTEM03: Math superscript (^) and subscript (_) characters are not allowed in text mode
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEM03), StandardField.SUBTITLE, "_ or ^ Outside Math-Environment"),

                // TTEM04: $ characters cannot be used inside math mode
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEM04), StandardField.TITLE, "\\$ inside math mode \\($1+1=2$\\)"),

                // TTEV00: \verb or \verb* must be followed by a non-whitespace delimiter character
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEV00), StandardField.KEYWORDS, "\\verb "),

                // TTEV01: Line ended before the end delimiter of \verb or \verb* was found
                // Skipped

                // TTEC00: Undefined command \{0}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEC00, "undefinedCommand"), StandardField.ABSTRACT, "\\undefinedCommand"),

                // TTEC01: Command \{0} cannot be used in {1} mode
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEC01, "par", "MATH"), StandardField.TITLE, "$\\par$"),

                // TTEC02: Command \{0} is missing required argument #{1}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEC02, "textbf", "1"), StandardField.KEYWORDS, "\\textbf"),

                // TTEC03: Could not find target for combining command \{0}
                // Skipped

                // TTEC04: Inappropriate target for combining command \{0}
                // Skipped

                // TTEE00: Found \end of environment {0} instead of {1}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE00, "align", "itemize"), StandardField.ABSTRACT, "\\begin{itemize} \\end{align}"),

                // TTEE01: Expected to read valid environment name enclosed in braces without whitespace
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE01), StandardField.TITLE, "\\begin{ align }"),

                // TTEE02: Undefined environment {0}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE02, "undefinedEnv"), StandardField.ABSTRACT, "\\begin{undefinedEnv}"),

                // TTEE03: Environment {0} cannot be used in {1} mode
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE03, "itemize", "MATH"), StandardField.TITLE, "$\\begin{itemize}$"),

                // TTEE04: Environment {0} was still open at end of input document
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE04, "itemize"), StandardField.ABSTRACT, "\\begin{itemize}"),

                // TTEE05: Unexpected \end - no environment is currently open
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE05), StandardField.KEYWORDS, "\\end{itemize}"),

                // TTEE06: Environment {0} is missing required argument #{1}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEE06, "tabular", "1"), StandardField.TITLE, "\\begin{tabular}\\end{tabular}"),

                // TTEU00: Expansion limit ({0}) for user-defined commands and environments has been exceeded. Possible recursion?
                // Skipped

                // TTEUC0: Input ended before name of new command was found
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC0), StandardField.TITLE, "\\newcommand{\\"),

                // TTEUC1: Name of new command must be preceded by \
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC1), StandardField.TITLE, "\\newcommand{MyCommand}"),

                // TTEUC2: Input ended before end of new command definition
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC2), StandardField.TITLE, "\\newcommand{\\MyCommand}{"),

                // TTEUC3: No definition provided for new command {0}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC3, "MyCommand"), StandardField.TITLE, "\\newcommand{\\MyCommand}"),

                // TTEUC4: Command \{0} has not already been defined so cannot be renewed
                // Skipped

                // TTEUC5: Command \{0} already exists - use \renewcommand to redefine it
                // Skipped

                // TTEUC6: No ’}’ found after new command name
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC6), StandardField.TITLE, "\\newcommand{\\MyCommand"),

                // TTEUC7: Number of arguments specified in command or environment definition {0} must be an integer between 1 and 9 - not {1}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC7, "MyCommand", "0"), StandardField.TITLE, "\\newcommand{\\MyCommand}[0]{Text}"),

                // TTEUC8: Reserved command {0} cannot be redefined
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC8, "begin"), StandardField.TITLE, "\\renewcommand{\\begin}{}"),

                // TTEUC9: Input ended before end of argument count specification
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUC9), StandardField.TITLE, "\\newcommand{\\MyCommand}["),

                // TTEUCA: Definition of command {0} refers to argument #{1} but only {2} have been declared
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUCA, "MyCommand", "2", "1"), StandardField.TITLE, "\\newcommand{\\MyCommand}[1]{#2}"),

                // TTEUE0: Expected to read name of new environment enclosed in braces
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUE0), StandardField.TITLE, "\\newenvironment{}"),

                // TTEUE1: No {0} definition provided for new environment {1}
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUE1, "begin", "MyEnvironment"), StandardField.TITLE, "\\newenvironment{MyEnvironment}"),

                // TTEUE2: Environment {0} has not already been defined so cannot be renewed
                // Skipped

                // TTEUE3: Environment {0} already exists - use \renewenvironment to redefine it
                // Skipped

                // TTEUE5: Definition of begin of environment {0} refers to argument #{1} but only {2} have been declared
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUE5, "MyEnvironment", "1", "0"), StandardField.TITLE, "\\newenvironment{MyEnvironment}{#1}{}"),

                // TTEUE6: Definition of end of environment {0} refers to argument #{1} but arguments may not be used here
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TTEUE6, "MyEnvironment", "2"), StandardField.TITLE, "\\newenvironment{MyEnvironment}[1]{#1}{#2}"),

                // ------------------------------------ TOKEN FIX-UP ERRORS ------------------------------------
                // TFEG00: Block token {0} cannot be used in LR mode
                // Skipped

                // TFEL00: Found content before first \item
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEL00), StandardField.TITLE, "\\begin{itemize}content before first \\item{}\\end{itemize}"),

                // TFEM00: Ambiguous multiple use of \over at current level
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEM00), StandardField.TITLE, "$1 \\over \\over 3$"),

                // TFEM01: Trailing subscript/superscript token
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEM01), StandardField.TITLE, "$x_$"),

                // TFEM02: Double subscript/superscript token is ambiguous - use curly brackets
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEM02), StandardField.TITLE, "$x_i_j$"),

                // TFEM03: \right had no preceding \left
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEM03), StandardField.TITLE, "$\\right)$"),

                // TFEM04: \left had no following \right
                Arguments.of(LatexIntegrityChecker.errorMessageFormatHelper(CoreErrorCode.TFEM04), StandardField.TITLE, "$\\left($")

                // TFETB0: \hline must be the only token in table row
                // Skipped
        );
    }
}

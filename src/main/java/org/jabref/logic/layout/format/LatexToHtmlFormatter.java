package org.jabref.logic.layout.format;

import java.io.IOException;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ed.ph.snuggletex.InputError;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnugglePackage;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.WebPageOutputOptions;

import static uk.ac.ed.ph.snuggletex.definitions.Globals.TEXT_MODE_ONLY;

/**
 * This formatter converts LaTeX commands to HTML
 */
public class LatexToHtmlFormatter extends Formatter implements LayoutFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexToHtmlFormatter.class);

    private static final SnuggleEngine ENGINE = new SnuggleEngine();
    private static final SnuggleSession SESSION;

    // Code adapted from org.jabref.logic.integrity.LatexIntegrityChecker
    static {
        SnugglePackage snugglePackage = ENGINE.getPackages().get(0);
        snugglePackage.addComplexCommand("textgreater", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textless", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textbackslash", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textbar", false, 0, TEXT_MODE_ONLY, null, null, null);
        // ENGINE.getPackages().get(0).addComplexCommandOneArg()
        // engine.getPackages().get(0).addComplexCommandOneArg("text", false, ALL_MODES,LR, StyleDeclarationInterpretation.NORMALSIZE, null, TextFlowContext.ALLOW_INLINE);

        SESSION = ENGINE.createSession();
        SESSION.getConfiguration().setFailingFast(true);
    }

    @Override
    public String getName() {
        return Localization.lang("LaTeX to HTML");
    }

    @Override
    public String getKey() {
        return "latex_to_html";
    }

    @Override
    public String format(String latexInput) {
        SESSION.reset();
        latexInput = latexInput.replace("\\providecommand", "\\newcommand");
        LOGGER.trace("Parsing {}", latexInput);
        SnuggleInput input = new SnuggleInput(latexInput);
        try {
            SESSION.parseInput(input);
        } catch (IOException e) {
            LOGGER.error("Error at parsing", e);
            return latexInput;
        }

        WebPageOutputOptions webPageOutputOptions = new WebPageOutputOptions();
        webPageOutputOptions.setHtml5(true);
        String result = SESSION.buildWebPageString(webPageOutputOptions);

        if (!SESSION.getErrors().isEmpty()) {
            InputError error = SESSION.getErrors().getFirst();
            LOGGER.error("Error at parsing", error.toString());
            return "Error: " + error;
        }

        return result;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts LaTeX encoding to HTML.");
    }

    @Override
    public String getExampleInput() {
        return "M{\\\"{o}}nch";
    }
}

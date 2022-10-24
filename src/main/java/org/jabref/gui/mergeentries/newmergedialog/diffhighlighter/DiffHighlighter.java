package org.jabref.gui.mergeentries.newmergedialog.diffhighlighter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.fxmisc.richtext.StyleClassedTextArea;

public abstract sealed class DiffHighlighter permits SplitDiffHighlighter, UnifiedDiffHighlighter {
    protected final StyleClassedTextArea sourceTextview;
    protected final StyleClassedTextArea targetTextview;

    protected DiffMethod diffMethod;

    public DiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview, DiffMethod diffMethod) {
        Objects.requireNonNull(sourceTextview, "source text view MUST NOT be null.");
        Objects.requireNonNull(targetTextview, "target text view MUST NOT be null.");

        this.sourceTextview = sourceTextview;
        this.targetTextview = targetTextview;
        this.diffMethod = diffMethod;
    }

    abstract void highlight();

    protected List<String> splitString(String str) {
        return Arrays.asList(str.split(diffMethod.separator()));
    }

    private void setDiffMethod(DiffMethod diffMethod) {
        this.diffMethod = diffMethod;
    }

    public DiffMethod getDiffMethod() {
        return diffMethod;
    }

    public String getSeparator() {
        return diffMethod.separator();
    }

    public enum DiffMethod {
        WORDS(" "), CHARS(""), COMMA(",");

        private final String separator;

        DiffMethod(String separator) {
            this.separator = separator;
        }

        public String separator() {
            return separator;
        }
    }

    protected String join(List<String> stringList) {
        return String.join(getSeparator(), stringList);
    }

    enum ChangeType {
        ADDITION, DELETION, CHANGE_DELETION
    }

    record Change(
            int position,
            int spanSize,
            ChangeType type) {
    }
}

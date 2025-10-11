package org.jabref.gui.mergeentries.threewaymerge.diffhighlighter;

import java.util.Arrays;
import java.util.List;

import org.jabref.gui.mergeentries.threewaymerge.DiffMethod;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.jspecify.annotations.NonNull;

public abstract sealed class DiffHighlighter permits SplitDiffHighlighter, UnifiedDiffHighlighter {
    protected final StyleClassedTextArea sourceTextview;
    protected final StyleClassedTextArea targetTextview;

    protected DiffMethod diffMethod;

    public DiffHighlighter(@NonNull StyleClassedTextArea sourceTextview,
                           @NonNull StyleClassedTextArea targetTextview,
                           DiffMethod diffMethod) {
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

    public enum BasicDiffMethod implements DiffMethod {
        WORDS(" "), CHARS(""), COMMA(",");

        private final String separator;

        BasicDiffMethod(String separator) {
            this.separator = separator;
        }

        @Override
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

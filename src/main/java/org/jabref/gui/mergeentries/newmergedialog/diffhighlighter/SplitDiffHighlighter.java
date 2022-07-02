package org.jabref.gui.mergeentries.newmergedialog.diffhighlighter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * A diff highlighter in which changes are split between source and target text view.
 * They are represented by an addition in the target text view and deletion in the source text view.
 */
public final class SplitDiffHighlighter extends DiffHighlighter {

    public SplitDiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview, DiffMethod diffMethod) {
        super(sourceTextview, targetTextview, diffMethod);
    }

    @Override
    public void highlight() {
        String sourceContent = sourceTextview.getText();
        String targetContent = targetTextview.getText();
        if (sourceContent.equals(targetContent)) {
            return;
        }

        List<String> sourceTokens = splitString(sourceContent);
        List<String> targetTokens = splitString(targetContent);

        List<AbstractDelta<String>> deltaList = DiffUtils.diff(sourceTokens, targetTokens).getDeltas();

        for (AbstractDelta<String> delta : deltaList) {
            int affectedSourceTokensPosition = delta.getSource().getPosition();
            int affectedTargetTokensPosition = delta.getTarget().getPosition();

            List<String> affectedTokensInSource = delta.getSource().getLines();
            List<String> affectedTokensInTarget = delta.getTarget().getLines();
            int joinedSourceTokensLength = affectedTokensInSource.stream()
                    .map(String::length)
                    .reduce(Integer::sum)
                    .map(value -> value + (getSeparator().length() * (affectedTokensInSource.size() - 1)))
                    .orElse(0);

            int joinedTargetTokensLength = affectedTokensInTarget.stream()
                    .map(String::length)
                    .reduce(Integer::sum)
                    .map(value -> value + (getSeparator().length() * (affectedTokensInTarget.size() - 1)))
                    .orElse(0);
            int affectedSourceTokensPositionInText = getPositionInText(affectedSourceTokensPosition, sourceTokens);
            int affectedTargetTokensPositionInText = getPositionInText(affectedTargetTokensPosition, targetTokens);
            switch (delta.getType()) {
                case CHANGE -> {
                    sourceTextview.setStyleClass(affectedSourceTokensPositionInText, affectedSourceTokensPositionInText + joinedSourceTokensLength, "deletion");
                    targetTextview.setStyleClass(affectedTargetTokensPositionInText, affectedTargetTokensPositionInText + joinedTargetTokensLength, "updated");
                }
                case DELETE ->
                        sourceTextview.setStyleClass(affectedSourceTokensPositionInText, affectedSourceTokensPositionInText + joinedSourceTokensLength, "deletion");
                case INSERT ->
                        targetTextview.setStyleClass(affectedTargetTokensPositionInText, affectedTargetTokensPositionInText + joinedTargetTokensLength, "addition");
            }
        }
    }

    public int getPositionInText(int positionInTokenList, List<String> tokenList) {
        if (positionInTokenList == 0) {
            return 0;
        } else {
            return tokenList.stream().limit(positionInTokenList).map(String::length)
                    .reduce(Integer::sum)
                    .map(value -> value + (getSeparator().length() * positionInTokenList))
                    .orElse(0);
        }
    }
}

package org.jabref.gui.mergeentries.newmergedialog.diffhighlighter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import org.fxmisc.richtext.StyleClassedTextArea;

/**
 * A diff highlighter in which differences of type {@link DeltaType#CHANGE} are unified and represented by an insertion
 * and deletion in the target text view. Normal addition and deletion are kept as they are.
 */
public final class UnifiedDiffHighlighter extends DiffHighlighter {

    public UnifiedDiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview, DiffMethod diffMethod) {
        super(sourceTextview, targetTextview, diffMethod);
    }

    public UnifiedDiffHighlighter(StyleClassedTextArea sourceTextview, StyleClassedTextArea targetTextview) {
        this(sourceTextview, targetTextview, DiffMethod.CHARS);
    }

    @Override
    public void highlight() {
        String sourceContent = sourceTextview.getText();
        String targetContent = targetTextview.getText();
        if (sourceContent.equals(targetContent)) {
            return;
        }

        List<String> sourceWords = splitString(sourceContent);
        List<String> targetWords = splitString(targetContent);
        List<String> unifiedWords = new ArrayList<>(targetWords);

        List<AbstractDelta<String>> deltaList = DiffUtils.diff(sourceWords, targetWords).getDeltas();

        List<Change> changeList = new ArrayList<>();

        int deletionCount = 0;
        for (AbstractDelta<String> delta : deltaList) {
            switch (delta.getType()) {
                case CHANGE -> {
                    int changePosition = delta.getTarget().getPosition();
                    int deletionPoint = changePosition + deletionCount;
                    int insertionPoint = deletionPoint + 1;
                    List<String> deltaSourceWords = delta.getSource().getLines();
                    List<String> deltaTargetWords = delta.getTarget().getLines();

                    unifiedWords.add(deletionPoint, join(deltaSourceWords));

                    changeList.add(new Change(deletionPoint, 1, ChangeType.CHANGE_DELETION));
                    changeList.add(new Change(insertionPoint, deltaTargetWords.size(), ChangeType.ADDITION));
                    deletionCount++;
                }
                case DELETE -> {
                    int deletionPoint = delta.getTarget().getPosition() + deletionCount;
                    unifiedWords.add(deletionPoint, join(delta.getSource().getLines()));

                    changeList.add(new Change(deletionPoint, 1, ChangeType.DELETION));
                    deletionCount++;
                }
                case INSERT -> {
                    int insertionPoint = delta.getTarget().getPosition() + deletionCount;
                    changeList.add(new Change(insertionPoint, delta.getTarget().getLines().size(), ChangeType.ADDITION));
                }
            }
        }
        targetTextview.clear();

        boolean changeInProgress = false;
        for (int position = 0; position < unifiedWords.size(); position++) {
            String word = unifiedWords.get(position);
            Optional<Change> changeAtPosition = findChange(position, changeList);
            if (changeAtPosition.isEmpty()) {
                appendToTextArea(targetTextview, getSeparator() + word, "unchanged");
            } else {
                Change change = changeAtPosition.get();
                List<String> changeWords = unifiedWords.subList(change.position(), change.position() + change.spanSize());

                if (change.type() == ChangeType.DELETION) {
                    appendToTextArea(targetTextview, getSeparator() + join(changeWords), "deletion");
                } else if (change.type() == ChangeType.ADDITION) {
                    if (changeInProgress) {
                        appendToTextArea(targetTextview, join(changeWords), "addition");
                        changeInProgress = false;
                    } else {
                        appendToTextArea(targetTextview, getSeparator() + join(changeWords), "addition");
                    }
                } else if (change.type() == ChangeType.CHANGE_DELETION) {
                    appendToTextArea(targetTextview, getSeparator() + join(changeWords), "deletion");
                    changeInProgress = true;
                }
                position = position + changeWords.size() - 1;
            }
        }
        if (targetTextview.getLength() >= getSeparator().length()) {
            // There always going to be an extra separator at the start
            targetTextview.deleteText(0, getSeparator().length());
        }
    }

    private void appendToTextArea(StyleClassedTextArea textArea, String text, String styleClass) {
        if (text.isEmpty()) {
            return;
        }
        // Append separator without styling it
        if (text.startsWith(getSeparator())) {
            textArea.append(getSeparator(), "unchanged");
            textArea.append(text.substring(getSeparator().length()), styleClass);
        } else {
            textArea.append(text, styleClass);
        }
    }

    private Optional<Change> findChange(int position, List<Change> changeList) {
        return changeList.stream().filter(change -> change.position() == position).findAny();
    }
}

package org.jabref.gui.mergeentries;

import java.util.List;

import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import com.tobiasdiez.easybind.EasyObservableValue;

@DefaultProperty("children")
public class DiffHighlightingEllipsingTextFlow extends TextFlow {

    private final static String DEFAULT_ELLIPSIS_STRING = "...";
    private StringProperty ellipsisString;

    private ObservableList<Node> allChildren = FXCollections.observableArrayList();
    private ChangeListener sizeChangeListener = (observableValue, number, t1) -> adjustText();
    private ListChangeListener<Node> listChangeListener = this::adjustChildren;

    private final String fullText;
    private final EasyObservableValue<String> comparisonString;
    private final ObjectProperty<MergeEntries.DiffMode> diffMode;

    public DiffHighlightingEllipsingTextFlow(String fullText, EasyObservableValue<String> comparisonString, ObjectProperty<MergeEntries.DiffMode> diffMode) {
        this.fullText = fullText;
        allChildren.addListener(listChangeListener);
        widthProperty().addListener(sizeChangeListener);
        heightProperty().addListener(sizeChangeListener);

        this.comparisonString = comparisonString;
        this.diffMode = diffMode;
        comparisonString.addListener((obs, oldValue, newValue) -> highlightDiff());
        diffMode.addListener((obs, oldValue, newValue) -> highlightDiff());
        highlightDiff();
    }

    @Override
    public ObservableList<Node> getChildren() {
        return allChildren;
    }

    private void adjustChildren(ListChangeListener.Change<? extends Node> change) {
        super.getChildren().clear();
        super.getChildren().addAll(allChildren);
        super.autosize();
        adjustText();
    }

    private void adjustText() {
        if (allChildren.size() == 0) {
            return;
        }
        // remove listeners
        widthProperty().removeListener(sizeChangeListener);
        heightProperty().removeListener(sizeChangeListener);

        if (removeUntilTextFits() && fillUntilOverflowing()) {
            ellipseUntilTextFits();
        }

        widthProperty().addListener(sizeChangeListener);
        heightProperty().addListener(sizeChangeListener);
    }

    private boolean removeUntilTextFits() {
        while (getHeight() > getMaxHeight() || getWidth() > getMaxWidth()) {
            if (super.getChildren().isEmpty()) {
                // nothing fits
                return false;
            }
            super.getChildren().remove(super.getChildren().size() - 1);
            super.autosize();
        }
        return true;
    }

    private boolean fillUntilOverflowing() {
        while (getHeight() <= getMaxHeight() && getWidth() <= getMaxWidth()) {
            if (super.getChildren().size() == allChildren.size()) {
                if (allChildren.size() > 0) {
                    // all Texts are displayed, let's make sure all chars are as well
                    Node lastChildAsShown = super.getChildren().get(super.getChildren().size() - 1);
                    Node lastChild = allChildren.get(allChildren.size() - 1);
                    if (lastChildAsShown instanceof Text && ((Text) lastChildAsShown).getText().length() < ((Text) lastChild).getText().length()) {
                        ((Text) lastChildAsShown).setText(((Text) lastChild).getText());
                    } else {
                        // nothing to fill the space with
                        return false;
                    }
                }
            } else {
                super.getChildren().add(allChildren.get(super.getChildren().size()));
            }
            super.autosize();
        }
        return true;
    }

    private boolean ellipseUntilTextFits() {
        while (getHeight() > getMaxHeight() || getWidth() > getMaxWidth()) {
            Text lastChildAsShown = (Text) super.getChildren().remove(super.getChildren().size() - 1);
            while (getEllipsisString().equals(lastChildAsShown.getText()) || "".equals(lastChildAsShown.getText())) {
                if (super.getChildren().isEmpty()) {
                    return false;
                }
                lastChildAsShown = (Text) super.getChildren().remove(super.getChildren().size() - 1);
            }
            Text shortenedChild = new Text(ellipseString(lastChildAsShown.getText()));
            shortenedChild.getStyleClass().addAll(lastChildAsShown.getStyleClass());
            super.getChildren().add(shortenedChild);
            super.autosize();
        }
        return true;
    }

    public void highlightDiff() {
        allChildren.clear();
        if (comparisonString.get() != null && !comparisonString.get().equals(fullText)) {
            final List<Text> highlightedText = switch (diffMode.getValue()) {
                case PLAIN -> {
                    Text text = new Text(fullText);
                    text.getStyleClass().add("text-unchanged");
                    yield List.of(text);
                }
                case WORD -> DiffHighlighting.generateDiffHighlighting(comparisonString.get(), fullText, " ");
                case CHARACTER -> DiffHighlighting.generateDiffHighlighting(comparisonString.get(), fullText, "");
                default -> throw new UnsupportedOperationException("Not implemented " + diffMode.getValue());
            };
            allChildren.addAll(highlightedText);
        } else {
            Text text = new Text(fullText);
            text.getStyleClass().add("text-unchanged");
            allChildren.add(text);
        }
        super.autosize();
        adjustText();
    }

    private String ellipseString(String s) {
        int spacePos = s.lastIndexOf(' ');
        if (spacePos <= 0) {
            return "";
        }
        return s.substring(0, spacePos) + getEllipsisString();
    }

    public final void setEllipsisString(String value) {
        ellipsisString.set((value == null) ? "" : value);
    }

    public String getEllipsisString() {
        return ellipsisString == null ? DEFAULT_ELLIPSIS_STRING : ellipsisString.get();
    }

    public final StringProperty ellipsisStringProperty() {
        return ellipsisString;
    }

    public String getFullText() {
        return fullText;
    }
}

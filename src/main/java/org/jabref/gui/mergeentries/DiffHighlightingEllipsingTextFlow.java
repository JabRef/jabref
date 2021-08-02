package org.jabref.gui.mergeentries;

import javafx.beans.DefaultProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

@DefaultProperty("children")
public class DiffHighlightingEllipsingTextFlow extends TextFlow {

    private final static String DEFAULT_ELLIPSIS_STRING = "...";
    private StringProperty ellipsisString;

    private ObservableList<Node> allChildren = FXCollections.observableArrayList();
    private ChangeListener sizeChangeListener = (observableValue, number, t1) -> adjustText();
    private ListChangeListener<Node> listChangeListener = this::adjustChildren;

    private final String fullText;

    public DiffHighlightingEllipsingTextFlow(String fullText) {
        this.fullText = fullText;
        allChildren.addListener(listChangeListener);
        widthProperty().addListener(sizeChangeListener);
        heightProperty().addListener(sizeChangeListener);
        adjustText();
    }

    @Override
    public ObservableList<Node> getChildren() {
        return allChildren;
    }

    private void adjustChildren(ListChangeListener.Change<? extends Node> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                super.getChildren().remove(change.getFrom(), change.getTo());
            } else if (change.wasAdded()) {
                super.getChildren().addAll(change.getFrom(), change.getAddedSubList());
            }
        }
        adjustText();
    }

    private void adjustText() {
        if (allChildren.size() == 0) {
            return;
        }
        // remove listeners
        widthProperty().removeListener(sizeChangeListener);
        heightProperty().removeListener(sizeChangeListener);
        while (getHeight() > getMaxHeight() || getWidth() > getMaxWidth()) {
            if (super.getChildren().isEmpty()) {
                // nothing fits
                widthProperty().addListener(sizeChangeListener);
                heightProperty().addListener(sizeChangeListener);
                return;
            }
            super.getChildren().remove(super.getChildren().size() - 1);
            super.autosize();
        }
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
                        widthProperty().addListener(sizeChangeListener);
                        heightProperty().addListener(sizeChangeListener);
                        return;
                    }
                }
            } else {
                super.getChildren().add(allChildren.get(super.getChildren().size()));
            }
            super.autosize();
        }
        // ellipse the last text as much as necessary
        while (getHeight() > getMaxHeight() || getWidth() > getMaxWidth()) {
            Node lastChildAsShown = super.getChildren().remove(super.getChildren().size() - 1);
            while (getEllipsisString().equals(((Text) lastChildAsShown).getText())) {
                if (super.getChildren().size() == 0) {
                    widthProperty().addListener(sizeChangeListener);
                    heightProperty().addListener(sizeChangeListener);
                    return;
                }
                lastChildAsShown = super.getChildren().remove(super.getChildren().size() - 1);
            }
            if (lastChildAsShown instanceof Text && ((Text) lastChildAsShown).getText().length() > 0) {
                Text shortenedChild = new Text(ellipseString(((Text) lastChildAsShown).getText()));
                super.getChildren().add(shortenedChild);
            } else {
                // don't know what to do with anything else. Leave without adding listeners
                return;
            }
            super.autosize();
        }
        widthProperty().addListener(sizeChangeListener);
        heightProperty().addListener(sizeChangeListener);
    }

    public void highlightDiffTo(String s) {
        allChildren.removeListener(listChangeListener);
        allChildren.clear();
        if (s != null && !s.equals(fullText)) {
            allChildren.addAll(DiffHighlighting.generateDiffHighlighting(fullText, s, " "));
        } else {
            allChildren.addAll(new Text(fullText));
        }
        adjustText();
        allChildren.addListener(listChangeListener);
    }

    private String ellipseString(String s) {
        int spacePos = s.lastIndexOf(' ');
        if (spacePos < 0) {
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

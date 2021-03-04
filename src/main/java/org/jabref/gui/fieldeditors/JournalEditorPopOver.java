package org.jabref.gui.fieldeditors;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.controlsfx.control.PopOver;

public class JournalEditorPopOver {

    public void openPopOver(Button journalStatisticsButton) {

        Label text1 = new Label("This is a text field");
        Label text2 = new Label("This is another textfield");
        Label text3 = new Label("Lorem ipsum dolor sit amet");
        VBox vBox = new VBox(text1, text2, text3);
        PopOver popOver = new PopOver(vBox);

        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);

        popOver.show(journalStatisticsButton, 0);

    }
}

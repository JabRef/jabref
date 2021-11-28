package org.jabref.gui.search;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
@Tag("dropdown")
class DropDownTest {

    private PopOver searchbarDropDown;
    private Button button;
    private Button popOverNotShowing;
    private Button buttonText;
    private CustomTextField searchField;


    @Start
    public void start(Stage stage) {
        this.searchbarDropDown = new PopOver();
        this.button = new Button("button");
        this.popOverNotShowing = new Button("popOverNotShowing");
        this.buttonText = new Button("WriteSomeText");
        this.searchField = SearchTextField.create();
        button.setOnAction(actionEvent -> {
                searchbarDropDown.show(button);
        });
        popOverNotShowing.setOnAction(actionEvent -> {
                popOverNotShowing.setText("popOverGone");
                searchbarDropDown.hide();
        });
        buttonText.setOnAction(actionEvent -> {
            searchField.setText("hello");
        });
        stage.setScene(new Scene(new StackPane(new HBox(button, popOverNotShowing, buttonText)), 500, 500));
        stage.show();
    }

    @Test
    void testButtonWorking(FxRobot robot) {
        robot.clickOn(buttonText); //enter Text into searchField
        assertEquals("hello", searchField.getText());
    }

    @Test
    void testDropDownShowing(FxRobot robot) {
        robot.clickOn(button); //popOver is showing
        assertTrue(searchbarDropDown.isShowing());
    }

    @Test
    void dropDownNotShowing(FxRobot robot) {
        robot.clickOn(button); //popOver is showing
        robot.clickOn(popOverNotShowing); //popover hides
        assertFalse(searchbarDropDown.isShowing());
    }
}

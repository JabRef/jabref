package org.jabref.styletester;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;

import com.airhacks.afterburner.views.ViewLoader;

public class StyleTesterView {
    @FXML private Button normalButtonHover;
    @FXML private Button normalButtonPressed;
    @FXML private Button normalButtonFocused;
    @FXML private Button textButtonHover;
    @FXML private Button textButtonPressed;
    @FXML private Button textButtonFocused;
    @FXML private Button containedButtonHover;
    @FXML private Button containedButtonPressed;
    @FXML private Button containedButtonFocused;
    private Parent content;

    StyleTesterView() {
        content = ViewLoader.view(this)
                            .load()
                            .getView();

        setStates();
    }

    private void setStates() {
        PseudoClass hover = PseudoClass.getPseudoClass("hover");
        normalButtonHover.pseudoClassStateChanged(hover, true);
        textButtonHover.pseudoClassStateChanged(hover, true);
        containedButtonHover.pseudoClassStateChanged(hover, true);

        PseudoClass pressed = PseudoClass.getPseudoClass("pressed");
        normalButtonPressed.pseudoClassStateChanged(pressed, true);
        textButtonPressed.pseudoClassStateChanged(pressed, true);
        containedButtonPressed.pseudoClassStateChanged(pressed, true);

        PseudoClass focused = PseudoClass.getPseudoClass("focused");
        normalButtonFocused.pseudoClassStateChanged(focused, true);
        textButtonFocused.pseudoClassStateChanged(focused, true);
        containedButtonFocused.pseudoClassStateChanged(focused, true);
    }

    public Parent getContent() {
        return content;
    }
}

package org.jabref.gui.ai.statuspane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;

public class UniversalStatusPaneViewModel extends AbstractViewModel {
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    private final BooleanProperty showTextArea = new SimpleBooleanProperty(false);
    private final StringProperty textAreaContent = new SimpleStringProperty("");

    private final BooleanProperty showSpinner = new SimpleBooleanProperty(false);

    private final BooleanProperty showButton1 = new SimpleBooleanProperty(false);
    private final StringProperty button1Text = new SimpleStringProperty("");
    private final ObjectProperty<EventHandler<ActionEvent>> button1Action = new SimpleObjectProperty<>();

    private final BooleanProperty showButton2 = new SimpleBooleanProperty(false);
    private final StringProperty button2Text = new SimpleStringProperty("");
    private final ObjectProperty<EventHandler<ActionEvent>> button2Action = new SimpleObjectProperty<>();

    public void executeButton1Action() {
        BindingsHelper.handle(button1Action);
    }

    public void executeButton2Action() {
        BindingsHelper.handle(button2Action);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public BooleanProperty showTextAreaProperty() {
        return showTextArea;
    }

    public StringProperty textAreaContentProperty() {
        return textAreaContent;
    }

    public BooleanProperty showSpinnerProperty() {
        return showSpinner;
    }

    public BooleanProperty showButton1Property() {
        return showButton1;
    }

    public StringProperty button1TextProperty() {
        return button1Text;
    }

    public ObjectProperty<EventHandler<ActionEvent>> button1ActionProperty() {
        return button1Action;
    }

    public BooleanProperty showButton2Property() {
        return showButton2;
    }

    public StringProperty button2TextProperty() {
        return button2Text;
    }

    public ObjectProperty<EventHandler<ActionEvent>> button2ActionProperty() {
        return button2Action;
    }
}

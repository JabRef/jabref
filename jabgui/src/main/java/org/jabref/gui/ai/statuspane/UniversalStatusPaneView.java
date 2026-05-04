package org.jabref.gui.ai.statuspane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import com.airhacks.afterburner.views.ViewLoader;

/// A handy component to display a state of a system: whether there is some important information, error, or action to take.
///
/// Look at the [org.jabref.gui.ai.summary.AiSummaryView] for an example of usage.
public class UniversalStatusPaneView extends BorderPane {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private TextArea textArea;
    @FXML private ProgressIndicator spinner;
    @FXML private HBox buttonsBox;
    @FXML private Button button1;
    @FXML private Button button2;

    // NOTE: Needed to construct the view model in a field in order for localization tests to work.
    private final UniversalStatusPaneViewModel viewModel = new UniversalStatusPaneViewModel();

    public UniversalStatusPaneView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        setupBindings();
    }

    private void setupBindings() {
        titleLabel.managedProperty().bind(titleLabel.visibleProperty());
        descriptionLabel.managedProperty().bind(descriptionLabel.visibleProperty());
        textArea.managedProperty().bind(textArea.visibleProperty());
        spinner.managedProperty().bind(spinner.visibleProperty());
        buttonsBox.managedProperty().bind(buttonsBox.visibleProperty());
        button1.managedProperty().bind(button1.visibleProperty());
        button2.managedProperty().bind(button2.visibleProperty());

        titleLabel.visibleProperty().bind(viewModel.titleProperty().isNotEmpty());
        descriptionLabel.visibleProperty().bind(viewModel.descriptionProperty().isNotEmpty());

        textArea.visibleProperty().bind(viewModel.showTextAreaProperty());
        spinner.visibleProperty().bind(viewModel.showSpinnerProperty());
        button1.visibleProperty().bind(viewModel.showButton1Property());
        button2.visibleProperty().bind(viewModel.showButton2Property());

        buttonsBox.visibleProperty().bind(viewModel.showButton1Property().or(viewModel.showButton2Property()));

        titleLabel.textProperty().bind(viewModel.titleProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
        textArea.textProperty().bind(viewModel.textAreaContentProperty());
        button1.textProperty().bind(viewModel.button1TextProperty());
        button2.textProperty().bind(viewModel.button2TextProperty());
    }

    @FXML
    private void onButton1Click() {
        viewModel.executeButton1Action();
    }

    @FXML
    private void onButton2Click() {
        viewModel.executeButton2Action();
    }

    public StringProperty titleProperty() {
        return viewModel.titleProperty();
    }

    public String getTitle() {
        return viewModel.titleProperty().get();
    }

    public void setTitle(String title) {
        viewModel.titleProperty().set(title);
    }

    public StringProperty descriptionProperty() {
        return viewModel.descriptionProperty();
    }

    public String getDescription() {
        return viewModel.descriptionProperty().get();
    }

    public void setDescription(String description) {
        viewModel.descriptionProperty().set(description);
    }

    public BooleanProperty showTextAreaProperty() {
        return viewModel.showTextAreaProperty();
    }

    public boolean isShowTextArea() {
        return viewModel.showTextAreaProperty().get();
    }

    public void setShowTextArea(boolean showTextArea) {
        viewModel.showTextAreaProperty().set(showTextArea);
    }

    public StringProperty textAreaContentProperty() {
        return viewModel.textAreaContentProperty();
    }

    public String getTextAreaContent() {
        return viewModel.textAreaContentProperty().get();
    }

    public void setTextAreaContent(String textAreaContent) {
        viewModel.textAreaContentProperty().set(textAreaContent);
    }

    public BooleanProperty showSpinnerProperty() {
        return viewModel.showSpinnerProperty();
    }

    public boolean isShowSpinner() {
        return viewModel.showSpinnerProperty().get();
    }

    public void setShowSpinner(boolean showSpinner) {
        viewModel.showSpinnerProperty().set(showSpinner);
    }

    public BooleanProperty showButton1Property() {
        return viewModel.showButton1Property();
    }

    public boolean isShowButton1() {
        return viewModel.showButton1Property().get();
    }

    public void setShowButton1(boolean showButton1) {
        viewModel.showButton1Property().set(showButton1);
    }

    public StringProperty button1TextProperty() {
        return viewModel.button1TextProperty();
    }

    public String getButton1Text() {
        return viewModel.button1TextProperty().get();
    }

    public void setButton1Text(String button1Text) {
        viewModel.button1TextProperty().set(button1Text);
    }

    public ObjectProperty<EventHandler<ActionEvent>> button1ActionProperty() {
        return viewModel.button1ActionProperty();
    }

    public EventHandler<ActionEvent> getButton1Action() {
        return viewModel.button1ActionProperty().get();
    }

    public void setButton1Action(EventHandler<ActionEvent> button1Action) {
        viewModel.button1ActionProperty().set(button1Action);
    }

    public BooleanProperty showButton2Property() {
        return viewModel.showButton2Property();
    }

    public boolean isShowButton2() {
        return viewModel.showButton2Property().get();
    }

    public void setShowButton2(boolean showButton2) {
        viewModel.showButton2Property().set(showButton2);
    }

    public StringProperty button2TextProperty() {
        return viewModel.button2TextProperty();
    }

    public String getButton2Text() {
        return viewModel.button2TextProperty().get();
    }

    public void setButton2Text(String button2Text) {
        viewModel.button2TextProperty().set(button2Text);
    }

    public ObjectProperty<EventHandler<ActionEvent>> button2ActionProperty() {
        return viewModel.button2ActionProperty();
    }

    public EventHandler<ActionEvent> getButton2Action() {
        return viewModel.button2ActionProperty().get();
    }

    public void setButton2Action(EventHandler<ActionEvent> button2Action) {
        viewModel.button2ActionProperty().set(button2Action);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onButton1ClickProperty() {
        return viewModel.button1ActionProperty();
    }

    public EventHandler<ActionEvent> getOnButton1Click() {
        return viewModel.button1ActionProperty().get();
    }

    public void setOnButton1Click(EventHandler<ActionEvent> onButton1Click) {
        viewModel.button1ActionProperty().set(onButton1Click);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onButton2ClickProperty() {
        return viewModel.button2ActionProperty();
    }

    public EventHandler<ActionEvent> getOnButton2Click() {
        return viewModel.button2ActionProperty().get();
    }

    public void setOnButton2Click(EventHandler<ActionEvent> onButton2Click) {
        viewModel.button2ActionProperty().set(onButton2Click);
    }
}

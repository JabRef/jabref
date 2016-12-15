## Architecture: Model - View - (Controller) - ViewModel  (MV(C)VM)
The goal of the MVVM architecture is to separate the state/behavior from the appearance of the ui. 
This is archived by dividing JabRef into different layers, each having a clear responsibility.
- The _Model_ contains the business logic and data structures. These aspects are again encapsulated in the _logic_ and _model_ package, respectively.
- The _View_ controls the appearance and structure of the UI. It is usually defined in a _FXML_ file. 
- _View model_ converts the data from logic and model in a form that is easily usable in the gui. Thus it controls the state of the View. Moreover, the ViewModel contains all the logic needed to change the current state of the UI or perform an action. These actions are usually passed down to the _logic_ package, after some data validation. The important aspect is that the ViewModel contains all the ui-related logic but does *not* have direct access to the controls defined in the View. Hence, the ViewModel can easily be tested by unit tests.
- The _Controller_ initializes the view model and binds it to the view. In an ideal world all the binding would already be done directly in the FXML. But JavaFX's binding expressions are not yet powerful enough to accomplish this. It is important to keep in mind that the Controller should be as minimalistic as possible. Especially one should resist the temptation to validate inputs in the controller. The ViewModel should handle data validation!

The only class which access model and logic classes is the ViewModel. Controller and View have only access the ViewModel and never the backend. The ViewModel does not know the Controller or View.

More details about the MVVM pattern can be found in [an article by Microsoft](https://msdn.microsoft.com/en-us/magazine/dd419663.aspx).

## An example
### ViewModel:
- The ViewModel should derive from `AbstractViewModel`.
````java
public class MyDialogViewModel extends AbstractViewModel {
}
````
- Add a (readonly) property as a private field and generate the getters according to the [JavaFX bean conventions](https://docs.oracle.com/javafx/2/binding/jfxpub-binding.htm):
````java
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();

    public ReadOnlyStringProperty headingProperty() {
        return heading.getReadOnlyProperty();
    }

    public String getHeading() {
        return heading.get();
    }
````
- Create constructor which initializes the fields to their default values. Write tests to ensure that everything works as expected!
````java
public MyDialogViewModel(Dependency dependency) {
    this.dependency = Objects.requireNonNull(dependency);
    heading.set("Hello " + dependency.getUserName());
}
````
- Add methods which allow interaction. Again, don't forget to write tests!
````java
public void shutdown() {
    heading.set("Goodbye!");
}
````
### Controller:
- The "code-behind" part of the view, which binds the `View` to the `ViewModel`.

- 
````java
public class AboutDialogController extends AbstractController<AboutDialogViewModel>
````
- 
````java
@FXML protected Button helloButton;
@FXML protected ImageView iconImage;
````
- Dependencies can easily be injected into the controller using the `@Inject` annotation.
````java
@Inject private DialogService dialogService;
````
- 
````java
@FXML
private void initialize() {
    viewModel = new AboutDialogViewModel(dialogService, clipBoardManager, buildInfo);
}
````
- The initialize method may use data-binding to connect the ui-controls and the `ViewModel`. However, it is recommended to do as much binding as possible directly in the FXML-file.
````java
    helloLabel.textProperty().bind(viewModel.helloMessageProperty());
````

- 
````java
@FXML
private void openJabrefWebsite() {
    viewModel.openJabrefWebsite();
}
````

- The current stage can be received using `getStage()`, which is helpful to close the current dialog `getStage().close()`.

### View:
The view consists of two parts:
- a FXML file "MyDialog.fxml" which defines the structure and the layout of the UI. It is recommended to use a graphical design tools like [SceneBuilder](http://gluonhq.com/labs/scene-builder/) to edit the FXML file. The tool [Scenic View](http://fxexperience.com/scenic-view/) is very helpful in debugging styling issues.
- a `View` class that loads the FXML file. The fxml file is loaded automatically using the same name as the class. To make this convention-over-configuration approach work, both the FXML file and the View class should have the same name and should be located in the same package.

- For dialogs, we simply derive from `AbstractDialogView` and implement the `show` method.
````java
public class MyDialogView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog myDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("My first dialog"));
        myDialog.setDialogPane((DialogPane) this.getView());
        myDialog.show();
    }
}
````

# Resources:
- [curated list of awesome JavaFX frameworks, libraries, books and etc...](https://github.com/mhrimaz/AwesomeJavaFX)
- [ControlsFX](http://fxexperience.com/controlsfx/features/) amazing collection of controls
- [usage of icon fonts with JavaFX](http://aalmiray.github.io/ikonli/#_javafx) 
- [Undo manager](https://github.com/TomasMikula/UndoFX)
- [Docking manager](https://github.com/alexbodogit/AnchorFX) [or](https://github.com/RobertBColton/DockFX)
- [additional bindings](https://github.com/lestard/advanced-bindings) or [EasyBind](https://github.com/TomasMikula/EasyBind)
- [Validation framework](https://github.com/sialcasa/mvvmFX/wiki/Validation)
- [mvvm framework](https://github.com/sialcasa/mvvmFX/wiki)
- [CSS Reference](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html)
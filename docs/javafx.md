## Architecture: Model - View - (Controller) - View Model  (MV(C)VM)
- _View model_ converts the data from `logic` and `model` in a form that is easily usable in the gui.
- The only purpose of the _View_ is to load and display the _fxml_ file.
- The _Controller_ initializes the view model and binds it to the view. In an ideal world all the binding would already be done directly in the _fxml_. But JavaFX's binding expressions are not yet powerful enough to accomplish this.

The only class which uses `model` and `logic` classes is the `ViewModel`. `Controller` and `View` only access the `ViewModel` and never the backend.  

## An example
### View model:
- The view model should derive from `AbstractViewModel`.
````java
public class MyDialogViewModel extends AbstractViewModel {
}
````
- Add a (readonly) property as a private field and generate the getters:
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
- 
````java
public class AboutDialogController extends AbstractController<AboutDialogViewModel>
````
- 
````java
@FXML protected Button closeButton;
@FXML protected ImageView iconImage;
@Inject private DialogService dialogService;
````
- 
````java
@FXML
private void initialize() {
    viewModel = new AboutDialogViewModel(dialogService, clipBoardManager, buildInfo);
}
````
- 
````java
@FXML
private void openJabrefWebsite() {
    viewModel.openJabrefWebsite();
}
````

### View:

# Resources:
- [curated list of awesome JavaFX frameworks, libraries, books and etc...](https://github.com/mhrimaz/AwesomeJavaFX)
- [ControlsFX](http://fxexperience.com/controlsfx/features/) amazing collection of controls
- [usage of icon fonts with JavaFX](http://aalmiray.github.io/ikonli/#_javafx) 
- [Undo manager](https://github.com/TomasMikula/UndoFX)
- [Docking manager](https://github.com/alexbodogit/AnchorFX) [or](https://github.com/RobertBColton/DockFX)
- [additional bindings](https://github.com/lestard/advanced-bindings)
- [Validation framework](https://github.com/sialcasa/mvvmFX/wiki/Validation)
- [mvvm framework](https://github.com/sialcasa/mvvmFX/wiki)
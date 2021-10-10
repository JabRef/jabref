# Readings on JavaFX

JabRef's recommendations about JavaFX

## Architecture: Model - View - \(Controller\) - ViewModel  \(MV\(C\)VM\)

The goal of the MVVM architecture is to separate the state/behavior from the appearance of the ui. This is archived by dividing JabRef into different layers, each having a clear responsibility.

* The _Model_ contains the business logic and data structures. These aspects are again encapsulated in the _logic_ and _model_ package, respectively.
* The _View_ controls the appearance and structure of the UI. It is usually defined in a _FXML_ file.
* _View model_ converts the data from logic and model in a form that is easily usable in the gui. Thus it controls the state of the View. Moreover, the ViewModel contains all the logic needed to change the current state of the UI or perform an action. These actions are usually passed down to the _logic_ package, after some data validation. The important aspect is that the ViewModel contains all the ui-related logic but does _not_ have direct access to the controls defined in the View. Hence, the ViewModel can easily be tested by unit tests.
* The _Controller_ initializes the view model and binds it to the view. In an ideal world all the binding would already be done directly in the FXML. But JavaFX's binding expressions are not yet powerful enough to accomplish this. It is important to keep in mind that the Controller should be as minimalistic as possible. Especially one should resist the temptation to validate inputs in the controller. The ViewModel should handle data validation! It is often convenient to load the FXML file directly from the controller.

The only class which access model and logic classes is the ViewModel. Controller and View have only access the ViewModel and never the backend. The ViewModel does not know the Controller or View.

More details about the MVVM pattern can be found in [an article by Microsoft](https://msdn.microsoft.com/en-us/magazine/dd419663.aspx) and in [an article focusing on the implementation with JavaFX](http://blog.buildpath.de/javafx-decouple-the-view-and-its-behavior-to-create-a-testable-ui/).

## An example

### ViewModel

* The ViewModel should derive from `AbstractViewModel`

```java
public class MyDialogViewModel extends AbstractViewModel {
}
```

* Add a \(readonly\) property as a private field and generate the getters according to the [JavaFX bean conventions](https://docs.oracle.com/javafx/2/binding/jfxpub-binding.htm):

```java
private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();

public ReadOnlyStringProperty headingProperty() {
    return heading.getReadOnlyProperty();
}

public String getHeading() {
    return heading.get();
}
```

* Create constructor which initializes the fields to their default values. Write tests to ensure that everything works as expected!

```java
public MyDialogViewModel(Dependency dependency) {
    this.dependency = Objects.requireNonNull(dependency);
    heading.set("Hello " + dependency.getUserName());
}
```

* Add methods which allow interaction. Again, don't forget to write tests!

```java
public void shutdown() {
    heading.set("Goodbye!");
}
```

### View - Controller

* The "code-behind" part of the view, which binds the `View` to the `ViewModel`.
* The usual convention is that the controller ends on the suffix `*View`. Dialogs should derive from `BaseDialog`.

```java
public class AboutDialogView extends BaseDialog<Void>
```

* You get access to nodes in the FXML file by declaring them with the `@FXML` annotation.

```java
@FXML protected Button helloButton;
@FXML protected ImageView iconImage;
```

* Dependencies can easily be injected into the controller using the `@Inject` annotation.

```java
@Inject private DialogService dialogService;
```

* It is convenient to load the FXML-view directly from the controller class.

  The FXML file is loaded using `ViewLoader` based on the name of the class passed to `view`. To make this convention-over-configuration approach work, both the FXML file and the View class should have the same name and should be located in the same package.

  Note that fields annotated with `@FXML` or `@Inject` only become accessible after `ViewLoader.load()` is called.

  a `View` class that loads the FXML file.

```java
private Dependency dependency;

public AboutDialogView(Dependency dependency) {
        this.dependency = dependency;

        this.setTitle(Localization.lang("About JabRef"));

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);
}
```

* Dialogs should use [setResultConverter](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/Dialog.html#setResultConverter-javafx.util.Callback-) to convert the data entered in the dialog to the desired result. This conversion should be done by the view model and not the controller.

```java
setResultConverter(button -> {
    if (button == ButtonType.OK) {
        return viewModel.getData();
    }
    return null;
});
```

* The initialize method may use data-binding to connect the ui-controls and the `ViewModel`. However, it is recommended to do as much binding as possible directly in the FXML-file.

```java
@FXML
private void initialize() {
    viewModel = new AboutDialogViewModel(dialogService, dependency, ...);

    helloLabel.textProperty().bind(viewModel.helloMessageProperty());
}
```

* calling the view model:

```java
@FXML
private void openJabrefWebsite() {
    viewModel.openJabrefWebsite();
}
```

### View - FXML

The view consists a FXML file `MyDialog.fxml` which defines the structure and the layout of the UI. Moreover, the FXML file may be accompanied by a style file that should have the same name as the FXML file but with a `css` ending, e.g., `MyDialog.css`. It is recommended to use a graphical design tools like [SceneBuilder](http://gluonhq.com/labs/scene-builder/) to edit the FXML file. The tool [Scenic View](https://github.com/JonathanGiles/scenic-view) is very helpful in debugging styling issues.

## Resources

* [curated list of awesome JavaFX frameworks, libraries, books and etc...](https://github.com/mhrimaz/AwesomeJavaFX)
* [ControlsFX](http://fxexperience.com/controlsfx/features/) amazing collection of controls
* [Undo manager](https://github.com/FXMisc/UndoFX)
* [Docking manager](https://github.com/alexbodogit/AnchorFX) [or](https://github.com/RobertBColton/DockFX)
* [additional bindings](https://github.com/lestard/advanced-bindings) or [EasyBind](https://github.com/TomasMikula/EasyBind)
* [Kubed](https://github.com/hudsonb/kubed): data visualization \(inspired by d3\)
* [Validation framework](https://github.com/sialcasa/mvvmFX/wiki/Validation)
* [mvvm framework](https://github.com/sialcasa/mvvmFX/wiki)
* [CSS Reference](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html)
* [JFoenix](https://github.com/jfoenixadmin/JFoenix) Material Designs look & feel
* [JavaFX Documentation project](https://fxdocs.github.io/docs/html5/index.html): Collected information on JavaFX in a central place
* [FXExperience](http://fxexperience.com/) JavaFX Links of the week
* [Foojay](https://foojay.io/) Java and JavaFX tutorials
* [FXTutorials](https://github.com/AlmasB/FXTutorials) A wide range of practical tutorials focusing on Java, JavaFX and FXGL

## Features missing in JavaFX

* bidirectional binding in FXML, see [official feature request](https://bugs.openjdk.java.net/browse/JDK-8090665)

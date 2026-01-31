
<b>Pattern 1: Use underscore (_) instead of named parameters when lambda parameters are unused. Replace unused lambda parameters like `obs`, `oldValue`, `observable` with `_` to indicate they are intentionally ignored.
</b>

Example code before:
```
idLookupGuess.selectedProperty().addListener((observable, oldValue, newValue) -> {
    preferences.setIdLookupGuessing(newValue);
});
```

Example code after:
```
idLookupGuess.selectedProperty().addListener((_, _, newValue) -> {
    preferences.setIdLookupGuessing(newValue);
});
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14662#discussion_r2644296016
- https://github.com/JabRef/jabref/pull/14616#discussion_r2617245721
</details>


___

<b>Pattern 2: Avoid code duplication by extracting repeated logic into separate helper methods. When similar code blocks appear multiple times (e.g., validation logic, identifier detection), create a dedicated method to encapsulate the shared functionality.
</b>

Example code before:
```
// In listener 1
Optional<Identifier> identifier = Identifier.from(text);
if (identifier.isPresent()) {
    boolean isValid = switch (identifier.get()) {
        case DOI doi -> DOI.isValid(doi.asString());
        case ISBN isbn -> isbn.isValid();
        default -> true;
    };
}
// In listener 2 - same code repeated
Optional<Identifier> identifier = Identifier.from(text);
if (identifier.isPresent()) {
    boolean isValid = switch (identifier.get()) {
        case DOI doi -> DOI.isValid(doi.asString());
        case ISBN isbn -> isbn.isValid();
        default -> true;
    };
}
```

Example code after:
```
private boolean isValidIdentifier(Identifier id) {
    return switch (id) {
        case DOI doi -> DOI.isValid(doi.asString());
        case ISBN isbn -> isbn.isValid();
        default -> true;
    };
}

// In both listeners
Optional<Identifier> identifier = Identifier.from(text);
if (identifier.isPresent() && isValidIdentifier(identifier.get())) {
    // process
}
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14662#discussion_r2634063426
- https://github.com/JabRef/jabref/pull/14662#discussion_r2650002511
</details>


___

<b>Pattern 3: Use Markdown-style JavaDoc comments (///) for multi-line documentation instead of traditional block comments (/** */). This applies to class-level and method-level documentation.
</b>

Example code before:
```
/**
 * Method to style refresh buttons
 */
private void styleButton() {
    // implementation
}
```

Example code after:
```
/// Method to style refresh buttons
private void styleButton() {
    // implementation
}
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14878#discussion_r2715445319
- https://github.com/JabRef/jabref/pull/14652#discussion_r2637124799
</details>


___

<b>Pattern 4: Avoid unnecessary null checks when variables are guaranteed to be non-null by design or framework constraints. Remove defensive null checks unless there's a documented reason why null is possible.
</b>

Example code before:
```
idLookupGuess.selectedProperty().addListener((_, _, newValue) -> {
    if (newValue && idText.getText() != null && !idText.getText().trim().isEmpty()) {
        // process
    }
});
```

Example code after:
```
idLookupGuess.selectedProperty().addListener((_, _, newValue) -> {
    if (newValue && !idText.getText().trim().isEmpty()) {
        // process
    }
});
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14662#discussion_r2644295663
- https://github.com/JabRef/jabref/pull/14652#discussion_r2637124572
</details>


___

<b>Pattern 5: Extract preferences retrieval logic into separate "fromBackingStore" methods to enable proper reset and import functionality. Create a method that reads from backing store with defaults, then use it in both initialization and reset/import operations.
</b>

Example code before:
```
public MergeDialogPreferences getMergeDialogPreferences() {
    mergeDialogPreferences = new MergeDialogPreferences(
        DiffMode.parse(get(MERGE_ENTRIES_DIFF_MODE)),
        getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF)
    );
    return mergeDialogPreferences;
}
```

Example code after:
```
public MergeDialogPreferences getMergeDialogPreferences() {
    mergeDialogPreferences = getMergeDialogPreferencesFromBackingStore(
        MergeDialogPreferences.getDefault()
    );
    return mergeDialogPreferences;
}

private MergeDialogPreferences getMergeDialogPreferencesFromBackingStore(MergeDialogPreferences defaults) {
    return new MergeDialogPreferences(
        DiffMode.valueOf(get(MERGE_ENTRIES_DIFF_MODE, defaults.getMergeDiffMode().name())),
        getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, defaults.getMergeShouldShowDiff())
    );
}
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14616#discussion_r2617245721
- https://github.com/JabRef/jabref/pull/14620#discussion_r2618174028
</details>


___

<b>Pattern 6: Use Optional.stream() instead of filter(Optional::isPresent).map(Optional::get) when working with streams of Optional values. This provides cleaner and more idiomatic code for flattening optionals in streams.
</b>

Example code before:
```
List<Path> linkedFiles = entry.getFiles().stream()
    .map(file -> file.findIn(directories))
    .filter(Optional::isPresent)
    .map(Optional::get)
    .toList();
```

Example code after:
```
List<Path> linkedFiles = entry.getFiles().stream()
    .map(file -> file.findIn(directories))
    .flatMap(Optional::stream)
    .toList();
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14710#discussion_r2649300999
</details>


___

<b>Pattern 7: Write CHANGELOG entries from an end-user perspective, avoiding technical implementation details. Focus on user-visible behavior changes rather than internal code structure or class names.
</b>

Example code before:
```
- We updated KeywordList parsing to support escaping of delimiter characters. [#12810]
```

Example code after:
```
- We fixed an issue where keywords containing delimiter characters could not be properly saved. [#12810]
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14637#discussion_r2628455729
- https://github.com/JabRef/jabref/pull/14829#discussion_r2677324060
</details>


___

<b>Pattern 8: Avoid reformatting entire files when making focused changes. Only format the specific code sections you modified, not unrelated parts of the file. Configure IDE to prevent automatic whole-file reformatting.
</b>

Example code before:
```
// Entire file reformatted with new spacing/indentation
@FXML
private ButtonType generateButtonType;
@FXML
private TabPane tabs;
@FXML
private Tab tabAddEntry;
```

Example code after:
```
// Only modified sections formatted
@FXML private ButtonType generateButtonType;
@FXML private TabPane tabs;
@FXML private Tab tabAddEntry;
```

<details><summary>Examples for relevant past discussions:</summary>

- https://github.com/JabRef/jabref/pull/14662#discussion_r2636219224
- https://github.com/JabRef/jabref/pull/14616#discussion_r2617243432
</details>


___

package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.util.Optional;
import java.util.function.Supplier;

import javafx.collections.ObservableList;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.gui.walkthrough.declarative.WalkthroughNodeIds;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/// Makes the groups pane and all entries available while the groups walkthrough is active.
@NullMarked
// [impl->req~ux.walkthrough.groups-preparation~1]
public class PrepareGroupsWalkthroughSideEffect implements WalkthroughSideEffect {
    private final StateManager stateManager;
    private final Supplier<Optional<TextInputControl>> searchFieldSupplier;

    private boolean groupsPaneWasVisible;
    private String previousSearchQuery = "";
    private Optional<TextInputControl> searchField = Optional.empty();

    public PrepareGroupsWalkthroughSideEffect(StateManager stateManager, Stage stage) {
        this(stateManager, () -> findSearchField(stage));
    }

    PrepareGroupsWalkthroughSideEffect(StateManager stateManager, Supplier<Optional<TextInputControl>> searchFieldSupplier) {
        this.stateManager = stateManager;
        this.searchFieldSupplier = searchFieldSupplier;
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        return searchFieldSupplier.get().map(resolvedSearchField -> {
            ObservableList<SidePaneType> visibleSidePanes = stateManager.getVisibleSidePaneComponents();
            groupsPaneWasVisible = visibleSidePanes.contains(SidePaneType.GROUPS);
            if (!groupsPaneWasVisible) {
                visibleSidePanes.add(SidePaneType.GROUPS);
            }

            searchField = Optional.of(resolvedSearchField);
            previousSearchQuery = resolvedSearchField.getText();
            resolvedSearchField.clear();
            return true;
        }).orElse(false);
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        if (!groupsPaneWasVisible) {
            stateManager.getVisibleSidePaneComponents().remove(SidePaneType.GROUPS);
        }
        searchField.ifPresent(field -> field.setText(previousSearchQuery));
        return true;
    }

    @Override
    public @NonNull String description() {
        return "Prepare groups walkthrough.";
    }

    private static Optional<TextInputControl> findSearchField(Stage stage) {
        return Optional.ofNullable(stage.getScene())
                       .map(scene -> scene.lookup("#" + WalkthroughNodeIds.GLOBAL_SEARCH_FIELD))
                       .filter(TextInputControl.class::isInstance)
                       .map(TextInputControl.class::cast);
    }
}

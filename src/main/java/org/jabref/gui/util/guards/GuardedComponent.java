package org.jabref.gui.util.guards;

import java.util.List;
import java.util.Optional;

import javafx.scene.Node;

import org.jabref.gui.util.DynamicallyChangeableNode;

/**
 * Component that will not be accessed if some conditions are unsatisfied.
 */
public abstract class GuardedComponent extends DynamicallyChangeableNode {
    private final List<ComponentGuard> guards;

    public GuardedComponent(List<ComponentGuard> guards) {
        this.guards = guards;

        guards.forEach(guard -> guard.addListener(_ -> checkGuards()));

        // Note: don't forget to call check guards at the end of your class constructor.
    }

    protected abstract Node showGuardedComponent();

    public void checkGuards() {
        for (ComponentGuard guard : guards) {
            if (!guard.get()) {
                setContent(guard.getExplanation());
                return;
            }
        }

        setContent(showGuardedComponent());
    }
}

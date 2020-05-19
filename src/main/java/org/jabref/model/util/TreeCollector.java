package org.jabref.model.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.TreeNode;

/**
 * Merges a list of nodes into a tree.
 * Nodes with a common parent are added as direct children.
 * For example, the list { A > A1, A > A2, B } is transformed into the forest { A > A1, A2, B}.
 */
public class TreeCollector<T> implements Collector<T, ObservableList<T>, ObservableList<T>> {

    private Function<T, List<T>> getChildren;
    private BiConsumer<T, T> addChild;
    private BiPredicate<T, T> equivalence;

    /**
     * @param getChildren a function that returns a list of children of the specified node
     * @param addChild    a function that adds the second argument as a child to the first-specified node
     * @param equivalence a function that tells us whether two nodes are equivalent
     */
    private TreeCollector(Function<T, List<T>> getChildren, BiConsumer<T, T> addChild, BiPredicate<T, T> equivalence) {
        this.getChildren = getChildren;
        this.addChild = addChild;
        this.equivalence = equivalence;
    }

    public static <T extends TreeNode<T>> TreeCollector<T> mergeIntoTree(BiPredicate<T, T> equivalence) {
        return new TreeCollector<>(
                TreeNode::getChildren,
                (parent, child) -> child.moveTo(parent),
                equivalence);
    }

    @Override
    public Supplier<ObservableList<T>> supplier() {
        return FXCollections::observableArrayList;
    }

    @Override
    public BiConsumer<ObservableList<T>, T> accumulator() {
        return (alreadyProcessed, newItem) -> {
            // Check if the node is already in the tree
            Optional<T> sameItemInTree = alreadyProcessed
                    .stream()
                    .filter(item -> equivalence.test(item, newItem))
                    .findFirst();
            if (sameItemInTree.isPresent()) {
                for (T child : new ArrayList<>(getChildren.apply(newItem))) {
                    merge(sameItemInTree.get(), child);
                }
            } else {
                alreadyProcessed.add(newItem);
            }
        };
    }

    private void merge(T target, T node) {
        Optional<T> sameItemInTree = getChildren
                .apply(target).stream()
                .filter(item -> equivalence.test(item, node))
                .findFirst();
        if (sameItemInTree.isPresent()) {
            // We need to copy the list because the #addChild method might remove the child from its own parent
            for (T child : new ArrayList<>(getChildren.apply(node))) {
                merge(sameItemInTree.get(), child);
            }
        } else {
            addChild.accept(target, node);
        }
    }

    @Override
    public BinaryOperator<ObservableList<T>> combiner() {
        return (list1, list2) -> {
            for (T item : list2) {
                accumulator().accept(list1, item);
            }
            return list1;
        };
    }

    @Override
    public Function<ObservableList<T>, ObservableList<T>> finisher() {
        return i -> i;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
    }
}

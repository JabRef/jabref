package org.jabref.toolkit.util;

import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine;

/// A custom IFactory implementation that enables unit tests to inject modified Commands instances.
/// This is useful to override init methods of the system under test or construct the sut with mocked dependencies.
/// Usually IFactory implementations act as a provider for classes that are already instantiated by a DI-framework.
public class CommandFactory implements CommandLine.IFactory {

    private final List<Object> objects;

    public CommandFactory(Object... preparedCommands) {
        this.objects = new ArrayList<>(List.of(preparedCommands));
    }

    @Override
    public <K> K create(Class<K> clazz) throws Exception {
        try {
            return doCreate(clazz); // custom factory lookup or instantiation
        } catch (Exception _) {
            return CommandLine.defaultFactory().create(clazz); // fallback if missing
        }
    }

    private <K> K doCreate(Class<K> clazz) {
        for (Object o : objects) {
            if (clazz.isInstance(o)) {
                return clazz.cast(o);
            }
        }
        throw new IllegalArgumentException("No object found for class: " + clazz.getName());
    }
}

package org.jabref.toolkit.util;

import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine;

public class CommandFactory implements CommandLine.IFactory {

    private final List<Object> objects;

    public CommandFactory(Object... preparedObjects) {
        List<Object> objects = new ArrayList<>(List.of(preparedObjects));
        this.objects = objects;
    }

    @Override
    public <K> K create(Class<K> clazz) throws Exception {
        try {
            return doCreate(clazz); // custom factory lookup or instantiation
        } catch (Exception ex) {
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

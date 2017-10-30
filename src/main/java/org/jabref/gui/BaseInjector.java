package org.jabref.gui;

/*
 * #%L
 * afterburner.fx
 * %%
 * Copyright (C) 2013 Adam Bien
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.airhacks.afterburner.configuration.Configurator;

/**
 * Based on {@link com.airhacks.afterburner.injection.Injector}, but contains a few fixes
 */
public class BaseInjector {

    private static Map<Class<?>, Object> modelsAndServices = new WeakHashMap<>();
    private static Set<Object> presenters = Collections.newSetFromMap(new WeakHashMap<>());
    private static Configurator configurator = new Configurator();
    private static Function<Class<?>, Object> instanceSupplier = getDefaultInstanceSupplier();
    private static Consumer<String> LOG = getDefaultLogger();

    public static <T> T instantiatePresenter(Class<T> clazz, Function<String, Object> injectionContext) {
        @SuppressWarnings("unchecked")
        T presenter = registerExistingAndInject((T) instanceSupplier.apply(clazz), injectionContext);
        return presenter;
    }

    public static <T> T instantiatePresenter(Class<T> clazz) {
        return instantiatePresenter(clazz, f -> null);
    }

    public static void setInstanceSupplier(Function<Class<?>, Object> instanceSupplier) {
        BaseInjector.instanceSupplier = instanceSupplier;
    }

    public static void setLogger(Consumer<String> logger) {
        LOG = logger;
    }

    public static void setConfigurationSource(Function<Object, Object> configurationSupplier) {
        configurator.set(configurationSupplier);
    }

    public static void resetInstanceSupplier() {
        instanceSupplier = getDefaultInstanceSupplier();
    }

    public static void resetConfigurationSource() {
        configurator.forgetAll();
    }

    /**
     * Caches the passed presenter internally and injects all fields
     *
     * @param <T>      the class to initialize
     * @param instance An already existing (legacy) presenter interesting in injection
     * @return presenter with injected fields
     */
    public static <T> T registerExistingAndInject(T instance, Function<String, Object> injectionContext) {
        T product = injectAndInitialize(instance, injectionContext);
        presenters.add(product);
        return product;
    }

    @SuppressWarnings("unchecked")
    public static <T> T instantiateModelOrService(Class<T> clazz, Function<String, Object> injectionContext) {
        T product = (T) modelsAndServices.get(clazz);
        if (product == null) {
            product = injectAndInitialize((T) instanceSupplier.apply(clazz), injectionContext);
            modelsAndServices.putIfAbsent(clazz, product);
        }
        return clazz.cast(product);
    }

    public static <T> void setModelOrService(Class<T> clazz, T instance) {
        modelsAndServices.put(clazz, instance);
    }

    static <T> T injectAndInitialize(T product, Function<String, Object> injectionContext) {
        injectMembers(product, injectionContext);
        initialize(product);
        return product;
    }

    static void injectMembers(final Object instance, Function<String, Object> injectionContext) {
        Class<? extends Object> clazz = instance.getClass();
        injectMembers(clazz, instance, injectionContext);
    }

    public static void injectMembers(Class<? extends Object> clazz, final Object instance, Function<String, Object> injectionContext) throws SecurityException {
        LOG.accept("Injecting members for class " + clazz + " and instance " + instance);
        Field[] fields = clazz.getDeclaredFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                LOG.accept("Field annotated with @Inject found: " + field);
                Class<?> type = field.getType();
                String fieldName = field.getName();

                // First try the configurator
                Object value = configurator.getProperty(clazz, fieldName);
                LOG.accept("Value returned by configurator is: " + value);

                // Next try injection context
                if (value == null) {
                    value = injectionContext.apply(fieldName);
                }

                if (value == null && isNotPrimitiveOrString(type)) {
                    LOG.accept("Field is not a JDK class");
                    value = instantiateModelOrService(type, injectionContext);
                }

                if (value != null) {
                    LOG.accept("Value is a primitive, injecting...");
                    injectIntoField(field, instance, value);
                }
            }
        }
        Class<? extends Object> superclass = clazz.getSuperclass();
        if (superclass != null) {
            LOG.accept("Injecting members of: " + superclass);
            injectMembers(superclass, instance, injectionContext);
        }
    }

    static void injectIntoField(final Field field, final Object instance, final Object target) {
        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            boolean wasAccessible = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(instance, target);
                return null; // return nothing...
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new IllegalStateException("Cannot set field: " + field + " with value " + target, ex);
            } finally {
                field.setAccessible(wasAccessible);
            }
        });
    }

    static void initialize(Object instance) {
        Class<? extends Object> clazz = instance.getClass();
        invokeMethodWithAnnotation(clazz, instance, PostConstruct.class
        );
    }

    static void destroy(Object instance) {
        Class<? extends Object> clazz = instance.getClass();
        invokeMethodWithAnnotation(clazz, instance, PreDestroy.class
        );
    }

    static void invokeMethodWithAnnotation(Class<?> clazz, final Object instance, final Class<? extends Annotation> annotationClass) throws IllegalStateException, SecurityException {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            if (method.isAnnotationPresent(annotationClass)) {
                AccessController.doPrivileged((PrivilegedAction<?>) () -> {
                    boolean wasAccessible = method.isAccessible();
                    try {
                        method.setAccessible(true);
                        return method.invoke(instance);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        throw new IllegalStateException("Problem invoking " + annotationClass + " : " + method, ex);
                    } finally {
                        method.setAccessible(wasAccessible);
                    }
                });
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            invokeMethodWithAnnotation(superclass, instance, annotationClass);
        }
    }

    public static void forgetAll() {
        Collection<Object> values = modelsAndServices.values();
        values.forEach(BaseInjector::destroy);
        presenters.forEach(BaseInjector::destroy);
        presenters.clear();
        modelsAndServices.clear();
        resetInstanceSupplier();
        resetConfigurationSource();
    }

    static Function<Class<?>, Object> getDefaultInstanceSupplier() {
        return (c) -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new IllegalStateException("Cannot instantiate view: " + c, ex);
            }
        };
    }

    public static Consumer<String> getDefaultLogger() {
        return l -> {
        };
    }

    private static boolean isNotPrimitiveOrString(Class<?> type) {
        return !type.isPrimitive() && !type.isAssignableFrom(String.class);
    }
}

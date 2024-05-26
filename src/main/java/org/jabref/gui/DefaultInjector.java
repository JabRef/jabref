package org.jabref.gui;

import java.util.function.Function;

import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.injection.PresenterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultInjector implements PresenterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInjector.class);

    /**
     * This method takes care of creating dependencies.
     * By default, it just creates a new instance of the class.
     * Dependencies without default constructor are constructed by hand.
     */
    private static Object createDependency(Class<?> clazz) {
        if (clazz == FileUpdateMonitor.class) {
            return Globals.getFileUpdateMonitor();
        } else {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                LOGGER.error("Cannot instantiate dependency: {}", clazz, ex);
                return null;
            }
        }
    }

    @Override
    public <T> T instantiatePresenter(Class<T> clazz, Function<String, Object> injectionContext) {
        LOGGER.debug("Instantiate {}", clazz.getName());

        // Use our own method to construct dependencies
        Injector.setInstanceSupplier(DefaultInjector::createDependency);

        return Injector.instantiatePresenter(clazz, injectionContext);
    }

    @Override
    public void injectMembers(Object instance, Function<String, Object> injectionContext) {
        LOGGER.debug("Inject into {}", instance.getClass().getName());

        // Use our own method to construct dependencies
        Injector.setInstanceSupplier(DefaultInjector::createDependency);

        Injector.injectMembers(instance, injectionContext);
    }
}

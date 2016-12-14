package net.sf.jabref.gui;

import java.util.function.Function;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.keyboard.KeyBindingPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.injection.PresenterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultInjector implements PresenterFactory {

    private static final Log LOGGER = LogFactory.getLog(DefaultInjector.class);

    @Override
    public <T> T instantiatePresenter(Class<T> clazz, Function<String, Object> injectionContext) {
        LOGGER.debug("Instantiate " + clazz.getName());

        // Use our own method to construct dependencies
        Injector.setInstanceSupplier(DefaultInjector::createDependency);

        return Injector.instantiatePresenter(clazz, injectionContext);
    }

    /**
     * This method takes care of creating dependencies.
     * By default, it just creates a new instance of the class.
     * Dependencies without default constructor are constructed by hand.
     */
    private static Object createDependency(Class<?> clazz) {
        if (clazz == DialogService.class) {
            return new FXDialogService();
        } else if (clazz == JabRefPreferences.class) {
            return Globals.prefs;
        } else if (clazz == KeyBindingPreferences.class) {
            return Globals.getKeyPrefs();
        } else {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                LOGGER.error("Cannot instantiate dependency: " + clazz, ex);
                return null;
            }
        }
    }
}

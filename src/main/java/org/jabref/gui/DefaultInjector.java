package org.jabref.gui;

import java.util.function.Function;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

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
        if (clazz == DialogService.class) {
            return new FXDialogService();
        } else if (clazz == TaskExecutor.class) {
            return Globals.TASK_EXECUTOR;
        } else if (clazz == PreferencesService.class) {
            return Globals.prefs;
        } else if (clazz == KeyBindingRepository.class) {
            return Globals.getKeyPrefs();
        } else if (clazz == JournalAbbreviationLoader.class) {
            return Globals.journalAbbreviationLoader;
        } else if (clazz == StateManager.class) {
            return Globals.stateManager;
        } else if (clazz == FileUpdateMonitor.class) {
            return Globals.getFileUpdateMonitor();
        } else {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                LOGGER.error("Cannot instantiate dependency: " + clazz, ex);
                return null;
            }
        }
    }

    @Override
    public <T> T instantiatePresenter(Class<T> clazz, Function<String, Object> injectionContext) {
        LOGGER.debug("Instantiate " + clazz.getName());

        // Use our own method to construct dependencies
        Injector.setInstanceSupplier(DefaultInjector::createDependency);

        return Injector.instantiatePresenter(clazz, injectionContext);
    }
}

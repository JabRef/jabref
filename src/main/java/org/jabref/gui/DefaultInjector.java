package org.jabref.gui;

import java.util.function.Function;

import javax.swing.undo.UndoManager;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.model.entry.BibEntryTypesManager;
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
            return JabRefGUI.getMainFrame().getDialogService();
        } else if (clazz == TaskExecutor.class) {
            return Globals.TASK_EXECUTOR;
        } else if (clazz == PreferencesService.class) {
            return Globals.prefs;
        } else if (clazz == KeyBindingRepository.class) {
            return Globals.getKeyPrefs();
        } else if (clazz == JournalAbbreviationRepository.class) {
            return Globals.journalAbbreviationRepository;
        } else if (clazz == StateManager.class) {
            return Globals.stateManager;
        } else if (clazz == FileUpdateMonitor.class) {
            return Globals.getFileUpdateMonitor();
        } else if (clazz == ProtectedTermsLoader.class) {
            return Globals.protectedTermsLoader;
        } else if (clazz == ClipBoardManager.class) {
            return Globals.getClipboardManager();
        } else if (clazz == UndoManager.class) {
            return Globals.undoManager;
        } else if (clazz == BibEntryTypesManager.class) {
            return Globals.entryTypesManager;
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

    @Override
    public void injectMembers(Object instance, Function<String, Object> injectionContext) {
        LOGGER.debug("Inject into " + instance.getClass().getName());

        // Use our own method to construct dependencies
        Injector.setInstanceSupplier(DefaultInjector::createDependency);

        Injector.injectMembers(instance, injectionContext);
    }
}

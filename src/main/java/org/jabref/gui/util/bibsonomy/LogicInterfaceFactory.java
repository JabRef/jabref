package org.jabref.gui.util.bibsonomy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.model.database.BibDatabaseContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.logic.LogicInterface;
import org.bibsonomy.rest.client.RestLogicFactory;

public class LogicInterfaceFactory {
    private static final Log LOGGER = LogFactory.getLog(LogicInterfaceFactory.class);

    public static LogicInterface getLogicWithoutFileSupport() {
        return new RestLogicFactory(
                BibSonomyProperties.getApiUrl(),
                RestLogicFactory.DEFAULT_RENDERING_FORMAT,
                RestLogicFactory.DEFAULT_CALLBACK_FACTORY,
                (username, hash, filename) -> {
                    try {
                        return Files.createTempDirectory("bibsonomy").toFile();
                    } catch (IOException e) {
                        LOGGER.error("Could not create temp dir for bibsonomy");
                        return new File("");
                    }
                })
                .getLogicAccess(
                        BibSonomyProperties.getUsername(),
                        BibSonomyProperties.getApiKey());
    }

    public static LogicInterface getLogic(BibDatabaseContext context) {
        return new RestLogicFactory(
                BibSonomyProperties.getApiUrl(),
                RestLogicFactory.DEFAULT_RENDERING_FORMAT,
                RestLogicFactory.DEFAULT_CALLBACK_FACTORY,
                new JabRefFileFactory(context))
                .getLogicAccess(
                        BibSonomyProperties.getUsername(),
                        BibSonomyProperties.getApiKey());
    }

}

package org.jabref.logic.git.util;

import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.SystemReader;

/**
 * Noop implementation of {@link SystemReader}
 * to prevent leaking of system and user git config into tests.
 */
public class NoopGitSystemReader extends SystemReader.Delegate {
    private static final StoredConfig NOP = new StoredConfig() {
        @Override
        public void load() {
        }

        @Override
        public void save() {
        }
    };

    public NoopGitSystemReader() {
        super(SystemReader.getInstance());
    }

    @Override
    public StoredConfig getUserConfig() {
        return NOP;
    }

    @Override
    public StoredConfig getSystemConfig() {
        return NOP;
    }
}

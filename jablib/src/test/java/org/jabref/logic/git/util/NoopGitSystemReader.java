package org.jabref.logic.git.util;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

/**
 * Nooop implementation of {@link SystemReader}
 * to prevent leaking of system and user git config into tests.
 */
public class NoopGitSystemReader extends SystemReader {

    private static final SystemReader PROXY = SystemReader.getInstance();

    @Override
    public String getHostname() {
        return PROXY.getHostname();
    }

    @Override
    public String getenv(String variable) {
        return PROXY.getenv(variable);
    }

    @Override
    public String getProperty(String key) {
        return PROXY.getProperty(key);
    }

    @Override
    public FileBasedConfig openUserConfig(Config parent, FS fs) {
        return new FileBasedConfig(parent, null, fs) {
            @Override
            public void load() {
            }

            @Override
            public boolean isOutdated() {
                return false;
            }
        };
    }

    @Override
    public FileBasedConfig openSystemConfig(Config parent, FS fs) {
        return new FileBasedConfig(parent, null, fs) {
            @Override
            public void load() {
            }

            @Override
            public boolean isOutdated() {
                return false;
            }
        };
    }

    @Override
    public FileBasedConfig openJGitConfig(Config parent, FS fs) {
        return new FileBasedConfig(parent, null, fs) {
            @Override
            public void load() {
            }

            @Override
            public boolean isOutdated() {
                return false;
            }
        };
    }

    @Override
    public long getCurrentTime() {
        return PROXY.getCurrentTime();
    }

    @Override
    public int getTimezone(long when) {
        return PROXY.getTimezone(when);
    }
}
